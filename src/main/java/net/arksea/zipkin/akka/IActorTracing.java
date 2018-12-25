package net.arksea.zipkin.akka;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import zipkin2.Span;

import java.util.concurrent.TimeUnit;

/**
 *
 * Created by xiaohaixing on 2018/12/21.
 */
public interface IActorTracing {
    IActorTracing NOOP = new IActorTracing() {
    };
    default ReceiveBuilder receiveBuilder() {
        return ReceiveBuilder.create();
    }
    default void tell(ActorRef target, Object message, ActorRef sender)  {
        target.tell(message, sender);
    }
    default void tell(ActorSelection target, Object message, ActorRef sender)  {
        target.tell(message, sender);
    }

    default Future ask(ActorRef receiver, Object message, ActorRef sender, long timeout) {
        return Patterns.ask(receiver, message, timeout);
    }

    default Future ask(ActorSelection receiver, Object message, ActorRef sender, long timeout) {
        return Patterns.ask(receiver, message, timeout);
    }

    default Cancellable scheduleOnce(ActorContext context, long delayMilliseconds ,
                             ActorRef receiver, Object message, ActorRef sender) {
        return context.system().scheduler().scheduleOnce(
            Duration.create(delayMilliseconds, TimeUnit.MILLISECONDS),
            receiver,message,context.system().dispatcher(),sender);
    }
    default void putTag(String key, String value) {}
    default void addAnnotation(String value) {}
}
