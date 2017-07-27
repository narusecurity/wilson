package com.me2ds.wilson;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Scheduler;
import com.google.gson.Gson;
import com.me2ds.wilson.pattern.InvalidPatternException;
import com.me2ds.wilson.pattern.Pattern;
import com.me2ds.wilson.template.InvalidTemplateException;
import com.me2ds.wilson.template.TemplateNotFoundException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.stringtemplate.v4.ST;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.me2ds.wilson.Constants.*;
import static akka.pattern.Patterns.gracefulStop;
import static com.me2ds.wilson.spring.SpringExtension.SpringExtProvider;

/**
 * Created by w3kim on 15. 6. 26..
 */
public class Wilson {
    private static final Logger logger = LoggerFactory.getLogger(Wilson.class.getSimpleName());

    private static Config wilsonConfig;
    private static List<String> templates;
    private static List<Pattern> patterns;
    private static List<String> src = new ArrayList<>();
    private static Map<String, List<Integer>> dst = new HashMap<>();

    /**
     *
     */
    private ActorRef manager;

    public static Config getConfig() {
        return wilsonConfig;
    }

    public static Config getRabbitConfig() { return wilsonConfig.getConfig("wilson.rabbitmq"); }

    public static List<String> getTemplates() {
        return templates;
    }

    public static List<Pattern> getPatterns() {
        return patterns;
    }

    public static List<String> getHosts() {
        return src;
    }

    public static Map<String, List<Integer>> getDestinations() {
        return dst;
    }

    /**
     *
     */
    private void run() {
        wilsonConfig = loadWilsonConfig();

        try {
            templates = loadTemplates();
        } catch (TemplateNotFoundException | InvalidTemplateException e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
        try {
            patterns = loadPatterns();
        } catch (InvalidPatternException e ){
            logger.error(e.getMessage());
            System.exit(1);
        }

        createPatternActors();
        createNoiseActors();

        // create a spring context and scan the classes
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext();

        ctx.scan("com.me2ds.wilson");
        ctx.refresh();

        final ActorSystem system = ctx.getBean(ActorSystem.class, wilsonConfig);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdown(30);
            }
        }));

        Props managerProps = SpringExtProvider.get(system).props("Manager");
        manager = system.actorOf(managerProps, Manager.NAME);

        tickCounter = new AtomicLong(System.currentTimeMillis());
        schedule(system);


    }

    private static AtomicLong tickCounter;

    /**
     *
     * @param system
     */
    private void schedule(ActorSystem system) {
        Scheduler scheduler = system.scheduler();
        ExecutionContextExecutor dispatcher = system.dispatcher();

        /**
         *
         */
        scheduler.schedule(DURATION_ZERO, DURATION_TICK, new Runnable() {
            @Override
            public void run() {
                Tick tick = Tick.tick(System.currentTimeMillis());
                manager.tell(tick, ActorRef.noSender());
            }
        }, dispatcher);

        /**
         *
         */
        scheduler.schedule(DURATION_ZERO, DURATION_HOUR, new Runnable() {
            @Override
            public void run() {
                Tick hourly = Tick.tick(System.currentTimeMillis());
                manager.tell(hourly, ActorRef.noSender());
            }
        }, dispatcher);
    }

    /**
     *
     * @param timeout
     */
    private void shutdown(int timeout) {
        // clean up code
        FiniteDuration duration = Duration.create(timeout, TimeUnit.SECONDS);
        gracefulStop(manager, duration, Shutdown.create(duration));
    }

    /**
     *
     * @return
     */
    private List<Pattern> loadPatterns() throws InvalidPatternException {
        List<Pattern> patterns = new ArrayList<>();
        Gson gson = new Gson();
        List<? extends Config> patternsList = wilsonConfig.getConfigList(WILSON_PATTERNS_LIST);
        for (Config pattern : patternsList) {
            try {
                patterns.add(gson.fromJson(
                        pattern.resolve().getValue(WILSON_PATTERN).render(),
                        Pattern.class
                ));
            } catch (Exception e) {
                throw new InvalidPatternException(pattern.toString());
            } // try
        } // for
        return patterns;
    }

    /**
     * @return
     */
    private List<String> loadTemplates() throws TemplateNotFoundException, InvalidTemplateException {
        List<String> templates = new ArrayList<>();

        List<String> templatesList = wilsonConfig.getStringList("wilson.templates"); // TODO factor out constant
        for (String templateName : templatesList) {
            final String templatePath = templateName + ".tpl"; // TODO factor out completing template path
            try {
                // read
                byte[] encoded = Files.readAllBytes(Paths.get(templatePath));
                String template = new String(encoded, StandardCharsets.UTF_8);

                // validate
                validateTemplate(template);

                // add
                templates.add(template);
            } catch (JSONException e) {
                throw new InvalidTemplateException(templateName);
            } catch (IOException e) {
                throw new TemplateNotFoundException(templatePath);
            }
        }
        return templates;
    }

    /**
     *
     * @param template
     * @throws JSONException
     */
    private void validateTemplate(String template) throws JSONException {
        ST test = new ST(template);
        test.add("timestamp_double", System.currentTimeMillis() / 1000.0);
        test.add("sip", "192.168.0.100");
        test.add("dip", "8.8.8.8");
        test.add("dport", 25);
        test.add("sbyte", 100);
        test.add("dbyte", 100);
        test.add("duration", 10);
        test.add("dst_asname", "fake");
        new JSONObject(test.render());
    }

    /**
     *
     */
    private void createNoiseActors() {
        Config srcConfig = wilsonConfig.getConfig("wilson.src");
        int srcSize = srcConfig.getInt("size");

        List<String> ipPrefix = srcConfig.getStringList("ip_prefix");
        if (ipPrefix.isEmpty()) {
            logger.error("At least one IP prefix must be provided to create SRC_IP's");
            System.exit(1);
        } // if

        long numberOfPossiblePermutation = 0;
        for (String prefix : ipPrefix) {
            long tmp = 1;
            for (int i = (4 - prefix.split("\\.").length); i > 0; i--) {
                if (i > 1) {
                    tmp *= 255;
                } else {
                    tmp *= 254;
                }
            }
            numberOfPossiblePermutation += tmp;
        }
        logger.info("{} usable host IP addresses can be generated", numberOfPossiblePermutation);

        if (numberOfPossiblePermutation < srcSize) {
            logger.error("Requested host size cannot be met with the given IP prefixes");
            System.exit(1);
        }

        // TODO calculate the max possible number of addresses and warn user if srcSize is greater
        while (src.size() < srcSize) {
            String srcIp = ipPrefix.get(PRNG.getInt(ipPrefix.size()));
            for (int j = 4 - srcIp.split("\\.").length; j > 0; j--) {
                int chosen = (j > 1) ? PRNG.getInt(255) : PRNG.getInt(1, 255);
                srcIp += "." + chosen;
            }
            if (src.contains(srcIp)) continue;
            src.add(srcIp);
        }

        Config dstConfig = wilsonConfig.getConfig("wilson.dst");
        int dstSize = dstConfig.getInt("size");
        int portVariation = dstConfig.getInt("port_variation");
        while (dst.size() < dstSize) {
            String dstIp = "" + PRNG.getInt(1, 254);
            for (int j = 0; j < 3; j++) {
                dstIp += ".";
                dstIp += (j > 1) ? PRNG.getInt(255) : PRNG.getInt(1, 255);
            }
            if (dst.containsKey(dstIp)) continue;
            int numPorts = PRNG.getInt(1, portVariation);
            List<Integer> ports = new ArrayList<>();
            for (int k = 0; k < numPorts; k++) {
                ports.add(PRNG.getInt(1, 65000));
            }
            dst.put(dstIp, ports);
        }
    }

    /**
     *
     */
    private void createPatternActors() {
        Gson gson = new Gson();
        for (Pattern pattern : patterns) {
            // TODO display pattern info
            src.add(pattern.getSrc_ip());
            // TODO add randomly generated ports
            dst.put(pattern.getDst_ip(), Collections.singletonList(pattern.getDst_port()));
        } // for
    }

    /**
     *
     */
    private Config loadWilsonConfig() {
        String confPath = System.getProperty(WILSON_CONF_ENV_KEY);
        final File userConfigFile = (confPath != null) ? new File(confPath) : new File(WILSON_CONF);
        if (!userConfigFile.exists()) {
            logger.error("Configuration file not found");
            System.exit(1);
        } // if
        return ConfigFactory.parseFile(userConfigFile);
    }

    /**
     * Main
     * @param args
     */
    public static void main(String[] args) {
        new Wilson().run();
    }
}
