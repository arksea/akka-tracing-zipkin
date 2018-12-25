package net.arksea.zipkin.akka.demo;

import net.arksea.zipkin.akka.AbstractTraceableMessage;

/**
 *
 * Created by xiaohaixing on 2018/12/20.
 */
public class Message3 extends AbstractTraceableMessage{
    public final String payload;
    public Message3(String payload) {
        this.payload = payload;
    }
}
