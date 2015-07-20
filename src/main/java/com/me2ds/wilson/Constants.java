package com.me2ds.wilson;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Created by w3kim on 15. 6. 26..
 */
public class Constants {

    public static final String WILSON_CONF = "wilson.conf";
    public static final String WILSON_CONF_ENV_KEY = "wilson.conf";
    public static final String WILSON_PATTERNS_LIST = "wilson.patterns";
    public static final String WILSON_PATTERN = "pattern";
    public static final String APP_NAME = "wilson";

    public static final FiniteDuration DURATION_TICK = Duration.create(1, TimeUnit.MILLISECONDS);
    public static final FiniteDuration DURATION_ZERO = Duration.create(0, TimeUnit.SECONDS);
}
