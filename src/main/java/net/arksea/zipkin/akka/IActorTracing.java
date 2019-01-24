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
    /**
     * 微秒为单位的时间戳（microseconds）
     * @return
     */
    default long tracingTimestamp() {
        return System.currentTimeMillis() * 1000L;
    }
    ReceiveBuilder receiveBuilder();
    void tell(ActorRef target, Object message, ActorRef sender) ;
    void tell(ActorSelection target, Object message, ActorRef sender);
    Future<Object> ask(ActorRef receiver, Object message, String askerName, long timeout);
    Future<Object> ask(ActorSelection receiver, Object message, String askerName, long timeout);
    Cancellable scheduleOnce(ActorContext context, long delayMilliseconds, ActorRef receiver, Object message, ActorRef sender);
    void putTag(String key, String value);
    void addAnnotation(String value);
    long makeSpanId();
    <T> String makeTracingId(T t);
    Endpoint makeEndpoint(String name);
    <T> void trace(T t, FI.UnitApply<T> unitApply) throws Exception;
}
