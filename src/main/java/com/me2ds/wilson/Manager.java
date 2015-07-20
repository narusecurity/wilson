package com.me2ds.wilson;

import akka.actor.*;
import akka.japi.Procedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static akka.pattern.Patterns.gracefulStop;

/**
 * Created by w3kim on 15-07-19.
 */
public class Manager extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(Manager.class.getSimpleName());

    /**
     *
     */
    private AtomicInteger tickCounter;

    /**
     *
     */
    private ActorRef hosts;

    @Override
    public void preStart() throws Exception {
        super.preStart();

        logger.info("Manager initializing");
        this.tickCounter = new AtomicInteger();
        this.hosts = getContext().actorOf(Props.create(HostManager.class), "hosts");
    }

    @Override
    public void postRestart(Throwable reason) throws Exception {
        super.postRestart(reason);

        logger.info("Manager restarted: [{}] {}", reason.getClass().getSimpleName(), reason.getMessage());
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message.equals("tick")) {
            this.tickCounter.incrementAndGet();
            for (ActorRef child : getContext().getChildren()) {
                child.tell(message, getSelf());
            }
        } else if (message instanceof Shutdown) {
            shutdown((Shutdown) message);
        } else {
            unhandled(message);
        }
    }

    /**
     * @param shutdown
     */
    private void shutdown(Shutdown shutdown) {
        // clean up
        int numChildren = 0;
        for (ActorRef child : getContext().getChildren()) {
            numChildren++;
        }

        // stop this
        if (numChildren == 0) {
            stop();
        } else {
            getContext().become(new ManagerShutdownProcedure(getContext(), numChildren, shutdown));
        }
    }

    /**
     *
     */
    private void stop() {
        getContext().stop(getSelf());
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        logger.info("Stopping Manager now ...");
    }

    /**
     *
     */
    private class ManagerShutdownProcedure implements Procedure<Object> {
        private final int numChildren;
        private final AtomicInteger numTerminated;
        private final UntypedActorContext context;

        public ManagerShutdownProcedure(UntypedActorContext context, int numChildren, Shutdown shutdown) {
            this.numChildren = numChildren;
            this.numTerminated = new AtomicInteger(0);
            this.context = context;

            logger.info("Sending stop messages to {} children(s)", numChildren);

            for (ActorRef child : context.getChildren()) {
                gracefulStop(child, shutdown.getTimeout(), shutdown);
            }
        }

        @Override
        public void apply(Object message) throws Exception {
            if (message instanceof Terminated) {
                logger.info("{} stopped...", ((Terminated) message).getActor().path().name());
                if (numTerminated.incrementAndGet() >= numChildren) {
                    stop();
                }
            }
        }
    }
}
