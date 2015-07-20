package com.me2ds.wilson;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by w3kim on 15-07-20.
 */
public class HostManager extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(HostManager.class.getSimpleName());

    /**
     *
     */
    private List<String> hosts;

    private Router router;

    @Override
    public void preStart() throws Exception {
        super.preStart();

        this.hosts = Wilson.getHosts();

        logger.info("HostManager is initializing with {} hosts", this.hosts.size());
        List<Routee> routees = new ArrayList<>();
        for (String host : hosts) {
            ActorRef r = getContext().actorOf(Props.create(HostActor.class), host);
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        router = new Router(new BroadcastRoutingLogic(), routees);
        logger.info("HostManager is ready");
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message.equals("tick")) {
            router.route(message, getSender());
        } else if (message instanceof Shutdown) {
            shutdown((Shutdown) message);
        } else {
            unhandled(message);
        }
    }

    private void shutdown(Shutdown shutdown) {
        logger.info("shutting down HostManager");
        getContext().stop(getSelf());
    }
}
