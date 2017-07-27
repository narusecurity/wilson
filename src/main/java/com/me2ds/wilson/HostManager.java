package com.me2ds.wilson;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import com.me2ds.wilson.spring.SpringActor;
import com.typesafe.config.Config;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by w3kim on 15-07-20.
 */
@Named("HostManager")
@Scope("prototype")
public class HostManager extends SpringActor {

    public static final String NAME = "host_manager";
    private static final Logger logger = LoggerFactory.getLogger(HostManager.class.getSimpleName());

    private static final long INTERVAL = 1000;

    private List<String> hosts;

    private Router router;

    @Autowired
    private RabbitTemplate template;

    private int numMessagesPerSecond;
    private String exchangeName;
    private String routingKey;

    private AtomicInteger sent;

    private long mark;

    {
        this.hosts = Wilson.getHosts();

        logger.info("HostManager is initializing with {} hosts", this.hosts.size());
        List<Routee> routees = new ArrayList<>();
        for (String host : hosts) {
            ActorRef r = getContext().actorOf(Props.create(HostActor.class, getSelf()), host);
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        router = new Router(new BroadcastRoutingLogic(), routees);

        Config rc = Wilson.getRabbitConfig();
        this.exchangeName = rc.getString("exchange");
        this.routingKey = rc.getString("routingkey");

        Config wilson = Wilson.getConfig();
        this.numMessagesPerSecond = wilson.getInt("wilson.num_messages_per_second");

        sent = new AtomicInteger(0);

        logger.info("HostManager is ready");
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Tick) {
            router.route(message, getSender());
        } else if (message instanceof JSONObject) {
            long ts = System.currentTimeMillis();
            if (ts - mark > INTERVAL) {
                sent.set(0);
                mark = ts;
            }

            if (numMessagesPerSecond == 0 || (sent.get() < numMessagesPerSecond)) {
                template.convertAndSend(exchangeName, routingKey, message.toString());
                sent.incrementAndGet();
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
        logger.info("shutting down HostManager");
        getContext().stop(getSelf());
    }
}
