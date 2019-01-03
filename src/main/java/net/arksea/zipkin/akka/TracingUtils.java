package net.arksea.zipkin.akka;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;
import zipkin2.codec.SpanBytesEncoder;

import java.util.Optional;

/**
 *
 * Created by xiaohaixing on 2018/12/25.
 */
public class TracingUtils {
    private static SpanBytesEncoder encoder = SpanBytesEncoder.PROTO3;
    private static SpanBytesDecoder decoder = SpanBytesDecoder.PROTO3;

    public static Object fillTracingSpan(Object message, ByteString tracingSpan) {
        if (message instanceof Message) {
            Message msg = (Message) message;
            Descriptors.Descriptor d = msg.getDescriptorForType();
            Descriptors.FieldDescriptor field = d.findFieldByName("tracingSpan");
            if (field != null) {
                return msg.toBuilder().setField(field, tracingSpan).build();
            } else {
                return msg;
            }
        } else if (message instanceof ITraceableMessage) {
            ITraceableMessage tm = (ITraceableMessage) message;
            Span s = decoder.decodeOne(tracingSpan.toByteArray());
            tm.setTracingSpan(s);
            return message;
        } else {
            return message;
        }
    }

    public static <T> T fillTracingSpan(T message, Span tracingSpan) {
        if (message instanceof Message) {
            Message msg = (Message) message;
            Descriptors.Descriptor d = msg.getDescriptorForType();
            Descriptors.FieldDescriptor field = d.findFieldByName("tracingSpan");
            if (field != null) {
                byte[] bytes = encoder.encode(tracingSpan);
                ByteString bs = ByteString.copyFrom(bytes);
                Message newMsg = msg.toBuilder().setField(field, bs).build();
                return (T)newMsg;
            } else {
                return message;
            }
        } else if (message instanceof ITraceableMessage) {
            ITraceableMessage tm = (ITraceableMessage) message;
            tm.setTracingSpan(tracingSpan);
            return message;
        } else {
            return message;
        }
    }

    public static ByteString getProtobufTracingSpan(Message message) {
        Descriptors.Descriptor d = message.getDescriptorForType();
        Descriptors.FieldDescriptor field = d.findFieldByName("tracingSpan");
        if (field == null) {
            return null;
        }
        return (ByteString)message.getField(field);
    }
    /**
     *
     * @param message
     * @return null表示消息不支持tracing，empty表示消息未设置span
     */
    public static Optional<Span> getTracingSpan(Object message) {
        if (message instanceof Message) {
            try {
                Message msg = (Message) message;
                ByteString bs = getProtobufTracingSpan(msg);
                if (bs == null) {
                    return null;
                }
                if (bs.size() == 0) {
                    return Optional.empty();
                }
                byte[] bytes = bs.toByteArray();
                return Optional.of(decoder.decodeOne(bytes));
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        } else if (message instanceof ITraceableMessage) {
            ITraceableMessage tm = (ITraceableMessage) message;
            Span span = tm.getTracingSpan();
            return span == null ? Optional.empty() : Optional.of(span);
        } else {
            return null;
        }
    }
}
