package net.arksea.zipkin.akka;

import zipkin2.Span;
import zipkin2.reporter.Reporter;

/**
 *
 * Created by xiaohaixing on 2019/1/3.
 */
public class SamplingReporter implements Reporter<Span> {
    private final long mod;
    private Reporter<Span> reporter;
    public SamplingReporter(long mod, Reporter<Span> reporter) {
        this.mod = mod;
        this.reporter = reporter;
    }

    @Override
    public void report(Span span) {
        String traceId = span.traceId();
        if (mod <= 0) {
        } else if (mod == 1) {
            reporter.report(span);
        } else if (traceId.hashCode() % mod == 0) {
            reporter.report(span);
        }
    }
}
