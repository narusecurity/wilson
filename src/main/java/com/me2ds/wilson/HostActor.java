package com.me2ds.wilson;

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

    @Override
    public void onReceive(Object message) throws Exception {
        if (message.equals("tick")) {
            if (PRNG.TF()) {
                String template = templates.get(PRNG.getInt(templates.size()));
                String dip = new ArrayList<>(destinations.keySet()).get(PRNG.getInt(destinations.size()));
                List<Integer> ports = destinations.get(dip);
                int port = ports.get(PRNG.getInt(ports.size()));

                ST test = new ST(template);
                test.add("sip", getSelf().path().name());
                test.add("dip", dip);
                test.add("dport", port);
                test.add("sbyte", 100);
                test.add("dbyte", 100);
                System.out.println(new JSONObject(test.render()).toString(3));
            }
        } else {
            unhandled(message);
        }
    }
}
