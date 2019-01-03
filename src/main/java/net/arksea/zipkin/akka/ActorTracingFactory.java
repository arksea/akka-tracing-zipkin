package net.arksea.zipkin.akka;

import akka.actor.ActorRef;
import zipkin2.Span;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;

/**
 *
 * Created by xiaohaixing on 2018/12/21.
 */
public class ActorTracingFactory {
    private static volatile Reporter<Span> reporter;
    private static Timer timer;

    private static Reporter<Span> getReporter() {
        if (reporter == null) {
            synchronized (ActorTracingFactory.class) {
                if (reporter == null) {
                    try {
                        timer = new TimerImplOffset();
                        reporter = createReporter();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return Reporter.NOOP;
                    }
                }
            }
        }
        return reporter;
    }

    private static Reporter<Span> createReporter() throws IOException {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("actor-tracing.properties");
        if (in == null) {
            return Reporter.NOOP;
        }
        Properties props = new Properties();
        props.load(in);
        String enabled = props.getProperty("enabledTracing");
        if ("true".equals(enabled)) {
            String host = props.getProperty("zipkin.host");
            String port = props.getProperty("zipkin.port");
            OkHttpSender sender = OkHttpSender.newBuilder()
                .endpoint("http://" + host + ":" + port + "/api/v2/spans")
                .encoding(Encoding.PROTO3).build();
            System.out.println("Enabled zipkin tracing: "+host+":"+port);
            return AsyncReporter.create(sender);
        } else {
            return Reporter.NOOP;
        }
    }

    public static IActorTracing create(ActorRef actor) {
        return create(actor.path().name());
    }

    public static IActorTracing create(ActorRef actor, int port) {
        return create(actor.path().name(), port);
    }

    public static IActorTracing create(ActorRef actor, String host, int port) {
        return create(actor.path().name(), host, port);
    }

    public static IActorTracing create(String serviceName) {
        return create(serviceName, 0);
    }

    public static IActorTracing create(String serviceName, int serverBindPort) {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            final String host = addr.getHostAddress();
            return create(serviceName, host, serverBindPort);
        } catch (Exception ex) {
            return create(serviceName, "", serverBindPort);
        }
    }

    public static IActorTracing create(String serviceName, String host, int port) {
        Reporter<Span> r = getReporter();
        if (r == Reporter.NOOP) {
            return IActorTracing.NOOP;
        } else {
            return new ZipkinTracing(getReporter(), serviceName, host, port, timer);
        }
    }
}
