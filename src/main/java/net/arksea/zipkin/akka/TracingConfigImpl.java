package net.arksea.zipkin.akka;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * Created by xiaohaixing on 2019/1/24.
 */
public class TracingConfigImpl implements ITracingConfig {
    private boolean enabled;
    private String tracingServerHost;
    private int tracingServerPort;
    private int samplingMod;

    public TracingConfigImpl() {
        try {
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("actor-tracing.properties");
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                this.enabled = Boolean.parseBoolean(props.getProperty("enabledTracing"));
                this.tracingServerHost = props.getProperty("tracingServer.host");
                this.tracingServerPort = Integer.parseInt(props.getProperty("tracingServer.port"));
                this.samplingMod = Integer.parseInt(props.getProperty("samplingMod"));
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        enabled = false;
        samplingMod = 0;
        tracingServerHost = "127.0.0.1";
        tracingServerPort = 9411;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getTracingServerHost() {
        return tracingServerHost;
    }

    @Override
    public int getTracingServerPort() {
        return tracingServerPort;
    }

    @Override
    public int getSamplingMod() {
        return samplingMod;
    }
}
