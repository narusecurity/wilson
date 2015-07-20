package com.me2ds.wilson;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Created by w3kim on 15. 7. 20..
 */
public class Tick implements Serializable {

    public static final int TYPE_TICK = 0;
    public static final int TYPE_HOURLY = 1;

    private int type;
    private FiniteDuration interval;

    private Tick(int interval, int type) {
        this(interval, type, TimeUnit.MILLISECONDS);
    }

    private Tick(int interval, int type, TimeUnit timeUnit) {
        this.type = type;
        this.interval = Duration.create(interval, timeUnit);
    }

    /**
     *
     * @return
     */
    public static Tick hourly() {
        return new Tick(3600, TYPE_HOURLY);
    }

    /**
     *
     * @return
     */
    public static Tick tick() {
        return new Tick(1, TYPE_TICK);
    }

    /**
     *
     * @return
     */
    public FiniteDuration getInterval() {
        return interval;
    }

    /**
     *
     * @return
     */
    public int getType() {
        return type;
    }
}
