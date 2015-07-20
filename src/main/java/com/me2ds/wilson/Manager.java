package com.me2ds.wilson;

import akka.actor.UntypedActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by w3kim on 15-07-19.
 */
public class Manager extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(Manager.class.getSimpleName());

    /**
     *
     */
    private AtomicInteger tickCounter;

    @Override
    public void preStart() throws Exception {
        super.preStart();

        logger.info("Manager initializing");
        this.tickCounter = new AtomicInteger();
    }

    @Override
    public void postRestart(Throwable reason) throws Exception {
        super.postRestart(reason);

        logger.info("Manager restarted: [{}] {}",reason.getClass().getSimpleName(), reason.getMessage());
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message.equals("tick")) {
            tickCounter.incrementAndGet();
        }
        else if (message.equals("shutdown")) {
            shutdown();
        }
        else {
            unhandled(message);
        }
    }

    /**
     *
     */
    private void shutdown() {
        logger.info("Shutting down Manager now ...");
        // clean up
        getContext().stop(getSelf());
    }
}
