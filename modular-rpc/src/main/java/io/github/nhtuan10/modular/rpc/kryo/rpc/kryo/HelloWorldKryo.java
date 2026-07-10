package io.github.nhtuan10.modular.rpc.kryo.rpc.kryo;

import io.github.nhtuan10.modular.impl.serdeserializer.KryoSerDeserializer;
import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static io.grpc.examples.helloworld.GreeterGrpc.SERVICE_NAME;

public class HelloWorldKryo {
    private static final KryoSerDeserializer serializer = new KryoSerDeserializer();

    static <T> MethodDescriptor.Marshaller<T> marshallerFor(Class<T> clz) {
        return new MethodDescriptor.Marshaller<T>() {
            @Override
            public InputStream stream(T value) {
                return new ByteArrayInputStream(serializer.serialization(value));
            }

            @Override
            public T parse(InputStream stream) {
                try {
                    return serializer.deserialization(stream.readAllBytes(), clz);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }


    public static record HelloRequest2(String name) {
    }

    public static record HelloReply2(String message) {
    }

    static final MethodDescriptor<HelloRequest2, HelloReply2> SAY_HELLO_METHOD_DESCRIPTOR =
            MethodDescriptor.newBuilder(
                            marshallerFor(HelloRequest2.class),
                            marshallerFor(HelloReply2.class))
                    .setFullMethodName(
                            MethodDescriptor.generateFullMethodName(SERVICE_NAME, "sayHello"))
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setSampledToLocalTracing(true)
                    .build();


    static abstract class HelloWorldImplBase implements BindableService {
        public abstract void sayHello(
                HelloRequest2 req, StreamObserver<HelloReply2> responseObserver);

        @Override
        public final ServerServiceDefinition bindService() {
            ServerServiceDefinition.Builder ssd = ServerServiceDefinition.builder(SERVICE_NAME);
            ssd.addMethod(SAY_HELLO_METHOD_DESCRIPTOR, ServerCalls.asyncUnaryCall(
                    this::sayHello));
            return ssd.build();
        }
    }
}
