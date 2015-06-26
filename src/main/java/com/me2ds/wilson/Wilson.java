package com.me2ds.wilson;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Created by w3kim on 15. 6. 26..
 */
public class Wilson {
    private static final Logger logger = LoggerFactory.getLogger(Wilson.class.getSimpleName());

    public static void main(String[] args) {
        Config applicationConfig = ConfigFactory.load();
        Config userConfig;

        String confPath = System.getProperty(Constants.WILSON_CONF_ENV_KEY);
        final File userConfigFile = (confPath != null) ? new File(confPath) : new File(Constants.WILSON_CONF);
        if (!userConfigFile.exists()) {
            logger.error("Configuration file not found");
            System.exit(1);
        } // if
        userConfig = ConfigFactory.parseFile(userConfigFile);

        Gson gson = new Gson();
        List<? extends Config> scenarios = userConfig.getConfigList("wilson.patterns");
        for (Config scenario : scenarios) {
            String json = scenario.resolve().getValue("pattern").render();
            Pattern s = gson.fromJson(json, Pattern.class);
            logger.info(s.toString());
        } // for

    }
}
