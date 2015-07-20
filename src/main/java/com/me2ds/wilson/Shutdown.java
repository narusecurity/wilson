package com.me2ds.wilson;

import scala.concurrent.duration.FiniteDuration;

/**
 * Created by w3kim on 15-07-20.
 */
public class Shutdown {

    private FiniteDuration timeout;

    private Shutdown(FiniteDuration timeout) {
        this.timeout = timeout;
    }

    public static Shutdown create(FiniteDuration timeout) {
        return new Shutdown(timeout);
    }

    public FiniteDuration getTimeout() {
        return timeout;
    }
}
