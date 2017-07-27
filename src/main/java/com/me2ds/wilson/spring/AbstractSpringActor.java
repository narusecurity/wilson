package com.me2ds.wilson.spring;

import akka.actor.AbstractActor;
import akka.actor.Props;

import javax.inject.Named;


/**
 * Created by w3kim on 2017-05-21.
 */
@Named("AbstractSpringActor")
public abstract class AbstractSpringActor extends AbstractActor {
    protected Props props(String name) {
        return SpringExtension.SpringExtProvider.get(getContext().system()).props(name);
    }
}