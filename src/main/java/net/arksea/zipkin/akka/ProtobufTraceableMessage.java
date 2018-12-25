package net.arksea.zipkin.akka;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;
import zipkin2.codec.SpanBytesEncoder;

/**
 *
 * Created by xiaohaixing on 2018/12/21.
 */
public class ProtobufTraceableMessage implements  ITraceableMessage {
    private final Descriptors.FieldDescriptor field;
    private final Message message;
    private SpanBytesEncoder encoder = SpanBytesEncoder.PROTO3;
    private SpanBytesDecoder decoder = SpanBytesDecoder.PROTO3;

    public ProtobufTraceableMessage(Descriptors.FieldDescriptor field, Message message) {
        this.field = field;
        this.message = message;
    }

    @Override
    public Span getTracingSpan() {
        try {
            ByteString bs = (ByteString) message.getField(this.field);
            if (bs == null) {
                return null;
            }
            byte[] bytes = bs.toByteArray();
            if (bytes == null) {
                return null;
            }
            return decoder.decodeOne(bytes);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public ByteString getRawTracingSpan() {
        try {
            return (ByteString) message.getField(this.field);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public void setTracingSpan(Span tracingSpan) {
//        try {
//            ByteString bs = ByteString.copyFrom(encoder.encode(tracingSpan));
//            message.toBuilder().setField(field, bs);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }

//    public void setRawTracingSpan(ByteString tracingSpan) {
//        try {
//            message.toBuilder().setField(field, tracingSpan);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
}
