package net.arksea.zipkin.akka;

import zipkin2.Span;

import java.io.Serializable;

/**
 *
 * Created by xiaohaixing on 2018/12/19.
 */
public interface ITraceableMessage extends Serializable {
    Span getTracingSpan();
    void setTracingSpan(Span tracingSpan);
    default String getTracingName() {
        return this.getClass().getSimpleName();
    }
}
