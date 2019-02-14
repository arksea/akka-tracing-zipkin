package net.arksea.zipkin.akka;

import zipkin2.Span;
import zipkin2.reporter.Reporter;

import java.util.function.Function;

/**
 *
 * Created by xiaohaixing on 2019/1/3.
 */
public class SamplingReporter implements Reporter<Span> {
    private Reporter<Span> reporter;
    private Function<String,Boolean> sampleFunc;
    public SamplingReporter(Function<String,Boolean> sampleFunc, Reporter<Span> reporter) {
        this.sampleFunc = sampleFunc;
        this.reporter = reporter;
    }

    @Override
    public void report(Span span) {
        String traceId = span.traceId();
        if (sampleFunc.apply(traceId)) {
            reporter.report(span);
        }
    }
}
