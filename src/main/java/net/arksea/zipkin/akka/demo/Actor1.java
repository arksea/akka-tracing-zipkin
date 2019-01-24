package net.arksea.zipkin.akka.demo;

import akka.actor.*;
import akka.japi.Creator;
import net.arksea.zipkin.akka.ActorTracingFactory;
import net.arksea.zipkin.akka.IActorTracing;
import net.arksea.zipkin.akka.TracingConfigImpl;

/**
 *
 * Created by xiaohaixing on 2018/12/19.
 */
public class Actor1 extends AbstractActor {
    private final ActorRef actor2;
    private IActorTracing tracing = ActorTracingFactory.create(new TracingConfigImpl(), "Actor1", "localhost", 0);

    public Actor1(ActorRef actor2) {
        this.actor2 = actor2;
    }

    public static Props props(ActorRef actor2) {
        return Props.create(new Creator<Actor1>() {
            @Override
            public Actor1 create() throws Exception {
                return new Actor1(actor2);
            }
        });
    }

    @Override
    public Receive createReceive() {
        return tracing.receiveBuilder()
            .match(Message1.class, this::handleMessage1)
            .match(Message3.class, this::handleMessage3)
            .build();
    }

    private void handleMessage1(Message1 msg) throws InterruptedException {
        tracing.tell(actor2, new Message2("hello, I'm Actor1"), self());
        tracing.tell(actor2, new Message2("hello, I'm Actor1"), self());
    }

    private void handleMessage3(Message3 msg) throws InterruptedException {
    }
}
