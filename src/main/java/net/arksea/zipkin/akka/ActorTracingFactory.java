package net.arksea.zipkin.akka;

import zipkin2.Span;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

import java.io.IOException;

/**
 *
 * Created by xiaohaixing on 2018/12/21.
 */
public class ActorTracingFactory {
    private static volatile Reporter<Span> reporter;
    private static Timer timer;

    private static Reporter<Span> getReporter(ITracingConfig config) {
        if (reporter == null) {
            synchronized (ActorTracingFactory.class) {
                if (reporter == null) {
                    try {
                        timer = new TimerImplOffset();
                        reporter = createReporter(config);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return Reporter.NOOP;
                    }
                }
            }
        }
        return reporter;
    }

    private static Reporter<Span> createReporter(ITracingConfig config) throws IOException {
        String host = config.getTracingServerHost();
        int port = config.getTracingServerPort();
        OkHttpSender sender = OkHttpSender.newBuilder()
            .endpoint("http://" + host + ":" + port + "/api/v2/spans")
            .encoding(Encoding.PROTO3).build();
        System.out.println("Enabled zipkin tracing: "+host+":"+port);
        return AsyncReporter.create(sender);
    }

    public static IActorTracing create(ITracingConfig config, String serviceName, String host, int port) {
        Reporter<Span> r = getReporter(config);
        Reporter<Span> reproter = new SamplingReporter(config.getSamplingMod(), r);
        return new ZipkinTracing(reproter, serviceName, host, port, config, timer);
    }
}
