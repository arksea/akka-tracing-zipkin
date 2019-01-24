package net.arksea.zipkin.akka;

/**
 *
 * Created by xiaohaixing on 2019/1/24.
 */
public interface ITracingConfig {
    boolean isEnabled();
    String getTracingServerHost();
    int getTracingServerPort();
    int getSamplingMod();
}
