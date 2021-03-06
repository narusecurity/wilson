package com.me2ds.wilson;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
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
    private ActorRef hostManager;

    public HostActor(ActorRef hostManager) {
        this.hostManager = hostManager;
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
                test.add("duration", 30);
                test.add("dst_asname", "fake_as");

                hostManager.tell(new JSONObject(test.render()), getSelf());
            }
        } else {
            unhandled(message);
        }
    }
}
