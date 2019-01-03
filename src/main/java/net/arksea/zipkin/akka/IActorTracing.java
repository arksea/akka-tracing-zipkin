package net.arksea.zipkin.akka;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import akka.japi.pf.FI;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import zipkin2.Endpoint;

import java.util.UUID;
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

    default Future<Object> ask(ActorRef receiver, Object message, String askerName, long timeout) {
        return Patterns.ask(receiver, message, timeout);
    }

    default Future<Object> ask(ActorSelection receiver, Object message, String askerName, long timeout) {
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
    default <T> void trace(T t, FI.UnitApply<T> unitApply) throws Exception  {}

    default long makeSpanId() {
        return UUID.randomUUID().getMostSignificantBits();
    }

    default <T> String makeTracingId(T t) {;
        return UUID.randomUUID().toString().replace("-","");
    }

    default Endpoint makeEndpoint(String name) {
        Endpoint.Builder eb = Endpoint.newBuilder();
        if (!"".equals(name)) {
            eb.serviceName(name);
        }
        return eb.build();
    }

    /**
     * 微秒为单位的时间戳（microseconds）
     * @return
     */
    default long tracingTimestamp() {
        return System.currentTimeMillis() * 1000L;
    }
}
