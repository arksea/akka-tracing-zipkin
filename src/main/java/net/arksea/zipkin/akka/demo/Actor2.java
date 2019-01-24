package net.arksea.zipkin.akka.demo;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.Creator;
import net.arksea.zipkin.akka.ActorTracingFactory;
import net.arksea.zipkin.akka.IActorTracing;
import net.arksea.zipkin.akka.TracingConfigImpl;

/**
 *
 * Created by xiaohaixing on 2018/12/20.
 */
public class Actor2 extends AbstractActor {
    private IActorTracing tracing = ActorTracingFactory.create(new TracingConfigImpl(), "Actor2", "localhost", 0);

    public static Props props() {
        return Props.create(new Creator<Actor2>() {
            @Override
            public Actor2 create() throws Exception {
                return new Actor2();
            }
        });
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return tracing.receiveBuilder()
            .match(Message2.class, this::handleMessage2)
            .build();
    }
    private void handleMessage2(Message2 msg) throws InterruptedException {
        tracing.tell(sender(), new Message3("hello, I'm Actor2"), self());
    }
}
