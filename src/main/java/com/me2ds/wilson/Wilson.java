package com.me2ds.wilson;

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
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by w3kim on 15. 6. 26..
 */
public class Wilson {
    private static final Logger logger = LoggerFactory.getLogger(Wilson.class.getSimpleName());

    private Config wilsonConfig;
    private List<String> templates;
    private List<Pattern> patterns;
    private List<String> src = new ArrayList<>();
    private Map<String, List<Integer>> dst = new HashMap<>();

    /**
     *
     */
    private void run() {
        this.wilsonConfig = loadWilsonConfig();

        try {
            this.templates = loadTemplates();
        } catch (TemplateNotFoundException | InvalidTemplateException e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
        try {
            this.patterns = loadPatterns();
        } catch (InvalidPatternException e ){
            logger.error(e.getMessage());
            System.exit(1);
        }

        createPatternActors();
        createNoiseActors();

        Collections.sort(src);

        System.out.println("SRC---" + src.size());
        for (String sip : src) {
            System.out.println("\t" + sip);
        }
        System.out.println("DST---" + dst.size());
        SortedSet<String> dipSet = new TreeSet<>(dst.keySet());

        for (String dip : dipSet) {
            for (Integer port : dst.get(dip)) {
                System.out.println("\t" + dip + ":" + port);
            }
        }
    }

    /**
     *
     * @return
     */
    private List<Pattern> loadPatterns() throws InvalidPatternException {
        List<Pattern> patterns = new ArrayList<>();
        Gson gson = new Gson();
        List<? extends Config> patternsList = wilsonConfig.getConfigList(Constants.WILSON_PATTERNS_LIST);
        for (Config pattern : patternsList) {
            try {
                patterns.add(gson.fromJson(
                        pattern.resolve().getValue(Constants.WILSON_PATTERN).render(),
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
        test.add("sip", "192.168.0.100");
        test.add("dip", "8.8.8.8");
        test.add("dport", 25);
        test.add("size", 100);
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
        for (Pattern pattern : this.patterns) {
            // TODO display pattern info
            src.add(pattern.getSrc_ip() + "*");
            // TODO add randomly generated ports
            dst.put(pattern.getDst_ip() + "*", Collections.singletonList(pattern.getDst_port()));
        } // for
    }

    /**
     *
     */
    private Config loadWilsonConfig() {
        String confPath = System.getProperty(Constants.WILSON_CONF_ENV_KEY);
        final File userConfigFile = (confPath != null) ? new File(confPath) : new File(Constants.WILSON_CONF);
        if (!userConfigFile.exists()) {
            logger.error("Configuration file not found");
            System.exit(1);
        } // if
        return ConfigFactory.parseFile(userConfigFile);
    }

    public static void main(String[] args) {
        new Wilson().run();
    }
}
