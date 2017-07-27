package com.me2ds.wilson.spring;

import akka.actor.Props;
import akka.actor.UntypedActor;

import javax.inject.Named;

/**
 * Created by w3kim on 2017-05-21.
 */
@Named("SpringActor")
public abstract class SpringActor extends UntypedActor {
    protected Props props(String name) {
        return SpringExtension.SpringExtProvider.get(getContext().system()).props(name);
    }
}