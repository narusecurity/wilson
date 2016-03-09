package com.me2ds.wilson;

import akka.actor.UntypedActor;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.typesafe.config.Config;
import org.json.JSONObject;
import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by w3kim on 15-07-20.
 */
public class HostActor extends UntypedActor {

    private List<String> templates = Wilson.getTemplates();
    private Map<String, List<Integer>> destinations = Wilson.getDestinations();
    private Connection conn;
    private Channel channel;
    private String exchangeName;
    private String routingKey;


    @Override
    public void preStart() throws Exception {
        super.preStart();
        Config rc = Wilson.getRabbitConfig();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(rc.getString("username"));
        factory.setPassword(rc.getString("password"));
        factory.setVirtualHost(rc.getString("virtualhost"));
        factory.setHost(rc.getString("host"));
        factory.setPort(rc.getInt("port"));
        this.conn = factory.newConnection();
        this.channel = this.conn.createChannel();

        String queueName = rc.getString("queue");
        this.exchangeName = rc.getString("exchange");
        this.routingKey = rc.getString("routingkey");
        this.channel.queueBind(queueName, exchangeName, routingKey);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Tick) {
            if (PRNG.TF()) {
                String template = templates.get(PRNG.getInt(templates.size()));
                String dip = new ArrayList<>(destinations.keySet()).get(PRNG.getInt(destinations.size()));
                List<Integer> ports = destinations.get(dip);
                int port = ports.get(PRNG.getInt(ports.size()));
                long timestamp = ((Tick) message).getTimestamp();

                // TODO : Wrap this into a builder
                ST test = new ST(template);
                test.add("timestamp_double", String.format("%f", (double) timestamp / 1000.0));
                test.add("sip", getSelf().path().name());
                test.add("dip", dip);
                test.add("dport", port);
                test.add("sbyte", 100);
                test.add("dbyte", 100);

                channel.basicPublish(exchangeName, routingKey, null, new JSONObject(test.render()).toString().getBytes());
            }
        } else {
            unhandled(message);
        }
    }
}
