package com.me2ds.wilson;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.typesafe.config.Config;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by w3kim on 15-07-20.
 */
public class HostManager extends UntypedActor {

    private static final Logger logger = LoggerFactory.getLogger(HostManager.class.getSimpleName());
    /**
     *
     */
    private static final long INTERVAL = 1000;
    /**
     *
     */
    private List<String> hosts;
    /**
     *
     */
    private Router router;
    /**
     *
     */
    private Connection conn;
    private Channel channel;
    private String exchangeName;
    private String routingKey;
    /**
     *
     */
    private int numMessagesPerSecond;
    /**
     *
     */
    private AtomicInteger sent;
    /**
     *
     */
    private long mark;

    /**
     *
     */ {
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
        this.conn = RabbitConnection.getConnection();
        try {
            this.channel = this.conn.createChannel();
            String queueName = rc.getString("queue");
            this.exchangeName = rc.getString("exchange");
            this.routingKey = rc.getString("routingkey");
            this.channel.queueBind(queueName, exchangeName, routingKey);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (rc.hasPath("num_messages_per_second"))
            this.numMessagesPerSecond = Wilson.getConfig().getInt("wilson.num_messages_per_second");

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

            if (numMessagesPerSecond == 0 ||
                    (sent.get() < numMessagesPerSecond)) {
                // send
                sent.incrementAndGet();
                channel.basicPublish(exchangeName, routingKey, null, message.toString().getBytes());
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
