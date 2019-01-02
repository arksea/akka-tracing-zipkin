package net.arksea.zipkin.akka.demo;

import akka.actor.*;
import akka.japi.Creator;
import net.arksea.zipkin.akka.ActorTracingFactory;
import net.arksea.zipkin.akka.IActorTracing;

/**
 *
 * Created by xiaohaixing on 2018/12/19.
 */
public class Actor1 extends AbstractActor {
    private final ActorRef actor2;
    private IActorTracing tracing = ActorTracingFactory.create(self());

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
        //tracing.putTag("tag1", "aaaaaaaaaaaaa");
        //tracing.putTag("tag2", "bbbbbbbbbbbb");
        //tracing.addAnnotation("111111111");
        //Thread.sleep(1);
        //tracing.addAnnotation("2222222");
        //Thread.sleep(1);
        tracing.tell(actor2, new Message2("hello, I'm Actor1"), self());
        tracing.tell(actor2, new Message2("hello, I'm Actor1"), self());
//        Message2 msgDelay = new Message2("hello, this is delay message");
//        tracing.scheduleOnce(context(), 1, actor2, msgDelay, self());
    }

    private void handleMessage3(Message3 msg) throws InterruptedException {

    }
}
