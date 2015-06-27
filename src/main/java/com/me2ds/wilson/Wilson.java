package com.me2ds.wilson;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Created by w3kim on 15. 6. 26..
 */
public class Wilson {
    private static final Logger logger = LoggerFactory.getLogger(Wilson.class.getSimpleName());

    private Config userConfig;
    private List<String> src = new ArrayList<>();
    private Map<String, List<Integer>> dst = new HashMap<>();

    private void run() {
        loadConfig();

        createPatternActors();
        createNoiseActors();

        Collections.sort(src);


        System.out.println("SRC---");
        for (String sip : src) {
            System.out.println("\t" + sip);
        }
        System.out.println("DST---");
        SortedSet<String> dipSet = new TreeSet<>(dst.keySet());

        for (String dip : dipSet) {
            for (Integer port : dst.get(dip)) {
                System.out.println("\t" + dip + ":" + port);
            }
        }
    }

    private void createNoiseActors() {
        Config srcConfig = userConfig.getConfig("wilson.src");
        int srcSize = srcConfig.getInt("size");

        List<String> ipPrefix = srcConfig.getStringList("ip_prefix");
        // TODO calculate the max possible number of addresses and warn user if srcSize is greater
        while (src.size() < srcSize) {
            String srcIp = ipPrefix.get(PRNG.getInt(ipPrefix.size()));
            for (int j = 4 - srcIp.split("\\.").length; j > 0; j--) {
                int chosen = (j > 1) ? PRNG.getInt(255) : PRNG.getInt(1, 254);
                srcIp += "." + chosen;
            }
            if (src.contains(srcIp)) continue;
            src.add(srcIp);
        }

        Config dstConfig = userConfig.getConfig("wilson.dst");
        int dstSize = dstConfig.getInt("size");
        int portVariation = dstConfig.getInt("port_variation");
        while (dst.size() < dstSize) {
            String dstIp = "" + PRNG.getInt(1, 254);
            for (int j = 0; j < 3; j++) {
                dstIp += ".";
                dstIp += (j > 1) ? PRNG.getInt(255) : PRNG.getInt(1, 254);
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

    private void createPatternActors() {
        Gson gson = new Gson();
        List<? extends Config> patternsList = userConfig.getConfigList(Constants.WILSON_PATTERNS_LIST);
        for (Config pattern : patternsList) {
            Pattern pobj = gson.fromJson(
                    pattern.resolve().getValue(Constants.WILSON_PATTERN).render(),
                    Pattern.class
            );
            src.add(pobj.getSrc_ip() + "*");
            dst.put(pobj.getDst_ip() + "*", Collections.singletonList(pobj.getDst_port()));
        } // for
    }

    private void loadConfig() {
        String confPath = System.getProperty(Constants.WILSON_CONF_ENV_KEY);
        final File userConfigFile = (confPath != null) ? new File(confPath) : new File(Constants.WILSON_CONF);
        if (!userConfigFile.exists()) {
            logger.error("Configuration file not found");
            System.exit(1);
        } // if
        userConfig = ConfigFactory.parseFile(userConfigFile);
    }

    public static void main(String[] args) {
        new Wilson().run();
    }
}
