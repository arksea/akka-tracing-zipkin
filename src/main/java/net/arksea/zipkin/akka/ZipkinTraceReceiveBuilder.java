package net.arksea.zipkin.akka;

import akka.japi.pf.FI;
import akka.japi.pf.ReceiveBuilder;

/**
 *
 * Created by xiaohaixing on 2018/12/19.
 */
public class ZipkinTraceReceiveBuilder extends ReceiveBuilder {
    private final ZipkinTracing tracing;
    ZipkinTraceReceiveBuilder(ZipkinTracing tracing) {
        this.tracing = tracing;
    }

    public <P> ZipkinTraceReceiveBuilder match(final Class<P> type, final FI.UnitApply<P> apply) {
        ZipkinUnitApply<P> traceUnitApply = new ZipkinUnitApply<>(apply, tracing);
        return (ZipkinTraceReceiveBuilder) matchUnchecked(type, traceUnitApply);
    }
}
