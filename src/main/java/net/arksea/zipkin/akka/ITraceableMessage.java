package net.arksea.zipkin.akka;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import zipkin2.Span;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 *
 * Created by xiaohaixing on 2018/12/19.
 */
public interface ITraceableMessage {
    Span getTracingSpan();
    void setTracingSpan(Span tracingSpan);

    static ITraceableMessage trans(Object message) {
        if (message instanceof ITraceableMessage) {
            return (ITraceableMessage) message;
        } else if (message instanceof Message){
            Message msg = (Message) message;
            Map<Descriptors.FieldDescriptor, Object> fields = msg.getAllFields();
            Descriptors.FieldDescriptor field = null;
            for (Descriptors.FieldDescriptor d: fields.keySet()) {
                if ("tracingSpan_".equals(d.getName())) {
                    field = d;
                    break;
                }
            }
            if (field != null) {
                return new ProtobufTraceableMessage(field, msg);
            }
            return null;
        } else {
            return null;
        }
    }
}
