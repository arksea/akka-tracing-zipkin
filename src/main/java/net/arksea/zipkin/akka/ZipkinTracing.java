package net.arksea.zipkin.akka;

import akka.actor.*;
import akka.japi.pf.FI;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import zipkin2.Endpoint;
import zipkin2.Span;
import zipkin2.reporter.Reporter;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by xiaohaixing on 2018/12/19.
 */
public class ZipkinTracing implements IActorTracing {
    private final Reporter<Span> reporter;
    private final String serviceName;
    private final Timer timer;
    private Span.Builder currentSpanBuilder;
    private final String host;
    private final int port;

    ZipkinTracing(Reporter<Span> reporter, String serviceName, String host, int port, Timer timer) {
        this.reporter = reporter;
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
        this.timer = timer;
    }

    ZipkinTracing(Reporter<Span> reporter, String serviceName, String host, int port) {
        this(reporter, serviceName, host, port, new TimerImplOffset());
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
                    Span span = makeTellSpan(message, sender.path().name(), currentSpan);
                    Object filledMsg = TracingUtils.fillTracingSpan(message, span);
                    receiver.tell(filledMsg, sender);
                }
            }, context.dispatcher());
    }

    @Override
    public void tell(ActorRef receiver, Object message, ActorRef sender) {
        Span span = makeTellSpan(message, sender.path().name(), getCurrentSpan());
        Object filledMsg = TracingUtils.fillTracingSpan(message, span);
        receiver.tell(filledMsg, sender);
    }

    @Override
    public void tell(ActorSelection receiver, Object message, ActorRef sender)  {
        Span span = makeTellSpan(message, sender.path().name(), getCurrentSpan());
        Object filledMsg = TracingUtils.fillTracingSpan(message, span);
        receiver.tell(filledMsg, sender);
    }

    @Override
    public Future ask(ActorRef receiver, Object message, String askerName, long timeout) {
        Span span = makeTellSpan(message, askerName, getCurrentSpan());
        Object filledMsg = TracingUtils.fillTracingSpan(message, span);
        return Patterns.ask(receiver, filledMsg, timeout);
    }

    @Override
    public Future ask(ActorSelection receiver, Object message, String askerName, long timeout) {
        Span span = makeTellSpan(message, askerName, getCurrentSpan());
        Object filledMsg = TracingUtils.fillTracingSpan(message, span);
        return Patterns.ask(receiver, filledMsg, timeout);
    }

    @Override
    public <T> void trace(T t, FI.UnitApply<T> unitApply) throws Exception {
        try {
            long start = timer.nowMicro();
            Optional<Span> op = TracingUtils.getTracingSpan(t);
            if (op == null) {
                if (unitApply != null) {
                    unitApply.apply(t);
                }
            } else {
                Endpoint endpoint = makeEndpoint(this.serviceName);
                Span tracingSpan;
                if (op.isPresent()) {
                    tracingSpan = op.get();
                } else {
                    String name;
                    if (t instanceof ITraceableMessage) {
                        name = ((ITraceableMessage)t).getTracingName();
                    } else {
                        name = t.getClass().getSimpleName();
                    }
                    tracingSpan = Span.newBuilder()
                        .traceId(makeTracingId(t))
                        .id(makeSpanId())
                        .name(name)
                        .kind(Span.Kind.CONSUMER)
                        .remoteEndpoint(endpoint)
                        .build();
                    reporter.report(tracingSpan);
                }
                Span.Builder applySpan = Span.newBuilder()
                    .traceId(tracingSpan.traceId())
                    .id(tracingSpan.id())
                    .name(tracingSpan.name());  //id不变则name不变

                setCurrentSpan(applySpan);
                if (unitApply != null) {
                    unitApply.apply(t);
                }

                long end = timer.nowMicro();
                long duration = end == start ? 1 : end - start;
                Span applyEndSpan = applySpan
                    .kind(Span.Kind.CONSUMER)
                    .duration(duration)
                    .timestamp(start)
                    .localEndpoint(endpoint)
                    .remoteEndpoint(null)
                    .build();
                reporter.report(applyEndSpan);
            }
        } finally {
            clearCurrentSpan();
        }
    }

    protected Span makeTellSpan(Object tm, String sendServiceName, Span currentSpan) {
        Span.Builder sb = Span.newBuilder();
        if (currentSpan == null) {
            sb.traceId(makeTracingId(tm));
        } else {
            sb.traceId(currentSpan.traceId());
            sb.parentId(currentSpan.id());
        }
        long now = timer.nowMicro();
        Endpoint endpoint = makeEndpoint(sendServiceName);
        //tell消息span的名字优先继承已填好的span的名字
        Optional<Span> op = TracingUtils.getTracingSpan(tm);
        String name;
        if (op != null && op.isPresent() && op.get().name() != null) {
            name = op.get().name();
        } else if (tm instanceof ITraceableMessage) {
            name = ((ITraceableMessage)tm).getTracingName();
        } else {
            name = tm.getClass().getSimpleName();
        }

        sb.id(makeSpanId())
            .name(name)
            //.timestamp(now)  //只在span最后一个report设置timestamp和duration,否则ZipkinUI显示不正常
            .addAnnotation(now, "sendMessage")
            .remoteEndpoint(endpoint)
            .localEndpoint(null)
            .kind(Span.Kind.CONSUMER);
        reporter.report(sb.build());
        sb.clearAnnotations();
        return sb.build();
    }

    @Override
    public Endpoint makeEndpoint(String name) {
        Endpoint.Builder eb = Endpoint.newBuilder();
        if (!"".equals(name)) {
            eb.serviceName(name);
        }
        if (!"".equals(this.host)) {
            eb.ip(this.host);
        }
        if (port > 0) {
            eb.port(this.port);
        }
        return eb.build();
    }

    @Override
    public long tracingTimestamp() {
        return timer.nowMicro();
    }

    @Override
    public boolean enabled() {
        return true;
    }

}
