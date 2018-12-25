package net.arksea.zipkin.akka;

import zipkin2.Span;

/**
 *
 * Created by xiaohaixing on 2018/12/20.
 */
public class AbstractTraceableMessage implements ITraceableMessage {
    private Span tracingSpan;

    @Override
    public Span getTracingSpan() {
        return tracingSpan;
    }

    @Override
    public void setTracingSpan(Span tracingSpan) {
        this.tracingSpan = tracingSpan;
    }
}
