package net.arksea.zipkin.akka;

import akka.actor.*;
import akka.japi.pf.FI;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import zipkin2.Endpoint;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.reporter.Reporter;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by xiaohaixing on 2018/12/19.
 */
public class ZipkinTracing implements IActorTracing {
    private final Reporter<Span> reporter;
    private final ActorRef actor;
    private final Timer timer;
    private Span.Builder currentSpanBuilder;
    private SpanBytesEncoder encoder = SpanBytesEncoder.PROTO3;
    private SpanBytesDecoder decoder = SpanBytesDecoder.PROTO3;

    ZipkinTracing(Reporter<Span> reporter, ActorRef actor, Timer timer) {
        this.reporter = reporter;
        this.actor = actor;
        this.timer = timer;
    }

    ZipkinTracing(Reporter<Span> reporter, ActorRef actor) {
        this.reporter = reporter;
        this.actor = actor;
        this.timer = new TimerImpl();
    }

    @Override
    public ReceiveBuilder receiveBuilder() {
        return new ZipkinTraceReceiveBuilder(this);
    }

    protected void setCurrentSpan(Span.Builder sb) {
        this.currentSpanBuilder = sb;
    }

    protected void clearCurrentSpan() {
        this.currentSpanBuilder = null;
    }

    protected Span getCurrentSpan() {
        if (currentSpanBuilder == null) {
            return null;
        } else {
            return Span.newBuilder()
                .merge(currentSpanBuilder.build())
                .clearAnnotations()
                .clearTags()
                .build();
        }
    }

    @Override
    public void putTag(String key, String value) {
        currentSpanBuilder.putTag(key, value);
    }

    @Override
    public void addAnnotation(String value) {
        currentSpanBuilder.addAnnotation(timer.nowMicro(), value);
    }

    @Override
    public Cancellable scheduleOnce(ActorContext context, long delayMilliseconds ,
                                    ActorRef receiver, Object message, ActorRef sender) {
        Span currentSpan = getCurrentSpan();
        return context.system().scheduler().scheduleOnce(
            Duration.create(delayMilliseconds, TimeUnit.MILLISECONDS),
            new Runnable() {
                @Override
                public void run() {
                    ITraceableMessage tm = ITraceableMessage.trans(message);
                    if (tm != null) {
                        Span tellSpan = makeTellSpan("scheduleOnce", message, receiver, sender, currentSpan);
                        tm.setTracingSpan(tellSpan);
                        reporter.report(tellSpan);
                    }
                    receiver.tell(message, sender);
                }
            }, context.dispatcher());
    }

    @Override
    public void tell(ActorRef receiver, Object message, ActorRef sender) {
        ITraceableMessage tm = ITraceableMessage.trans(message);
        if (tm != null) {
            Span tellSpan = makeTellSpan("send", message, receiver, sender, getCurrentSpan());
            tm.setTracingSpan(tellSpan);
            reporter.report(tellSpan);
        }
        receiver.tell(message, sender);
    }

    @Override
    public void tell(ActorSelection receiver, Object message, ActorRef sender)  {
        ITraceableMessage tm = ITraceableMessage.trans(message);
        if (tm != null) {
            Span tellSpan = makeTellSpan("send", message, receiver, sender, getCurrentSpan());
            tm.setTracingSpan(tellSpan);
            reporter.report(tellSpan);
        }
        receiver.tell(message, sender);
    }

    @Override
    public Future ask(ActorRef receiver, Object message, ActorRef sender, long timeout) {
        ITraceableMessage tm = ITraceableMessage.trans(message);
        if (tm != null) {
            Span tellSpan = makeTellSpan("ask", message, receiver, sender, getCurrentSpan());
            tm.setTracingSpan(tellSpan);
            reporter.report(tellSpan);
        }
        return Patterns.ask(receiver, message, timeout);
    }

    @Override
    public Future ask(ActorSelection receiver, Object message, ActorRef sender, long timeout) {
        ITraceableMessage tm = ITraceableMessage.trans(message);
        if (tm != null) {
            Span tellSpan = makeTellSpan("ask", message, receiver, sender, getCurrentSpan());
            tm.setTracingSpan(tellSpan);
            reporter.report(tellSpan);
        }
        return Patterns.ask(receiver, message, timeout);
    }

    public <T> void apply(T t, FI.UnitApply<T> unitApply) throws Exception {
        ITraceableMessage tm = ITraceableMessage.trans(t);
        if (tm != null) {
            try {
                long start = timer.nowMicro();
                long applySpanId = makeSpanId();
                Endpoint endpoint = makeEndpoint(actor);
                Span tracingSpan = tm.getTracingSpan();
                Span msgSpan;
                if (tracingSpan == null) {
                    msgSpan = Span.newBuilder()
                        .traceId(makeTracingId(t))
                        .id(makeSpanId())
                        .name("received("+t.getClass().getSimpleName()+")")
                        .kind(Span.Kind.PRODUCER)
                        .timestamp(start - 100)
                        .localEndpoint(endpoint)
                        .build();
                    tm.setTracingSpan(msgSpan);
                    reporter.report(msgSpan);
                } else {
                    msgSpan = tracingSpan;
                }
                long duration = start - msgSpan.timestamp();
                Span rcvSpan = Span.newBuilder()
                    .merge(msgSpan)
                    .duration(duration)
                    .build();
                reporter.report(rcvSpan);

                Span.Builder applySpan = Span.newBuilder()
                    .merge(msgSpan)
                    .id(applySpanId)
                    .parentId(msgSpan.id())
                    .name("handle("+t.getClass().getSimpleName()+")")
                    .kind(Span.Kind.CONSUMER)
                    .localEndpoint(endpoint)
                    .timestamp(start);
                reporter.report(applySpan.build());

                setCurrentSpan(applySpan);
                unitApply.apply(t);

                long end = timer.nowMicro();
                Span applyEndSpan = currentSpanBuilder
                    .duration(end - start)
                    .build();
                reporter.report(applyEndSpan);
            } finally {
                clearCurrentSpan();
            }
        } else {
            unitApply.apply(t);
        }
    }

    protected Span makeTellSpan(String name, Object tm, ActorRef receiver, ActorRef sender, Span currentSpan) {
        long tellSpanId = makeSpanId();
        Span.Builder sb = Span.newBuilder();
        if (currentSpan != null) {
            sb.merge(currentSpan);
            sb.parentId(currentSpan.id());
        } else {
            sb.traceId(makeTracingId(tm));
            sb.id(makeSpanId());
        }
        Endpoint remoteEp = makeEndpoint(receiver);
        Endpoint localEp = makeEndpoint(sender);
        sb.id(tellSpanId)
            .name(name+"("+tm.getClass().getSimpleName()+")")
            .timestamp(timer.nowMicro())
            .remoteEndpoint(remoteEp)
            .localEndpoint(localEp)
            .kind(Span.Kind.PRODUCER);
        return sb.build();
    }

    protected Span makeTellSpan(String name, Object tm, ActorSelection receiver, ActorRef sender, Span currentSpan) {
        long tellSpanId = makeSpanId();
        Span.Builder sb = Span.newBuilder();
        if (currentSpan != null) {
            sb.merge(currentSpan);
            sb.parentId(currentSpan.id());
        } else {
            sb.traceId(makeTracingId(tm));
            sb.id(makeSpanId());
        }
        Endpoint remoteEp = makeEndpoint(receiver);
        Endpoint localEp = makeEndpoint(sender);
        sb.id(tellSpanId)
            .name(name+"("+tm.getClass().getSimpleName()+")")
            .timestamp(timer.nowMicro())
            .remoteEndpoint(remoteEp)
            .localEndpoint(localEp)
            .kind(Span.Kind.PRODUCER);
        return sb.build();
    }

    public long makeSpanId() {
        return UUID.randomUUID().getMostSignificantBits();
    }

    public <T> String makeTracingId(T t) {;
        return UUID.randomUUID().toString().replace("-","");
    }

    public Endpoint makeEndpoint(ActorRef actor) {
        Endpoint.Builder eb = Endpoint.newBuilder();
        if (actor == null) {
            eb.serviceName("ActorRef.noSender");
        } else {
            ActorPath path = actor.path();
            Address addr = path.address();
            eb.serviceName(path.name());
            if (addr.host().nonEmpty()) {
                String host = addr.host().get();
                int port = Integer.parseInt(addr.port().get().toString());
                eb.ip(host).port(port);
            }
        }
        return eb.build();
    }

    public Endpoint makeEndpoint(ActorSelection actor) {
        Endpoint.Builder eb = Endpoint.newBuilder();
        if (actor == null) {
            eb.serviceName("ActorRef.noSender");
        } else {
            eb.serviceName(actor.pathString());
        }
        return eb.build();
    }
}
