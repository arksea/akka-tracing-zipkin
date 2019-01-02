package net.arksea.zipkin.akka;

import akka.japi.pf.FI;

/**
 *
 * Created by xiaohaixing on 2018/12/19.
 */
public class ZipkinUnitApply<T> implements FI.UnitApply<T> {
    private final FI.UnitApply<T> unitApply;
    private ZipkinTracing tracing;

    public ZipkinUnitApply(FI.UnitApply<T> unitApply, ZipkinTracing tracing) {
        this.unitApply = unitApply;
        this.tracing = tracing;
    }

    @Override
    public void apply(T t) throws Exception {
        tracing.trace(t, unitApply);
    }
}
