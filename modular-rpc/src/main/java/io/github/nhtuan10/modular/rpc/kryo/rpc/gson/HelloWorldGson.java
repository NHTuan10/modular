package io.github.nhtuan10.modular.rpc.kryo.rpc.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static io.grpc.examples.helloworld.GreeterGrpc.SERVICE_NAME;

public class HelloWorldGson {
    private static final Gson gson =
            new GsonBuilder().registerTypeAdapter(byte[].class, new TypeAdapter<byte[]>() {
                @Override
                public void write(JsonWriter out, byte[] value) throws IOException {
                    out.value(Base64.getEncoder().encodeToString(value));
                }

                @Override
                public byte[] read(JsonReader in) throws IOException {
                    return Base64.getDecoder().decode(in.nextString());
                }
            }).create();

    static <T> MethodDescriptor.Marshaller<T> marshallerFor(Class<T> clz) {
        return new MethodDescriptor.Marshaller<T>() {
            @Override
            public InputStream stream(T value) {
                return new ByteArrayInputStream(gson.toJson(value, clz).getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public T parse(InputStream stream) {
                return gson.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), clz);
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
