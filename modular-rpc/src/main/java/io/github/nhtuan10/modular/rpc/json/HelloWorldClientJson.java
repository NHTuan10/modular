package io.github.nhtuan10.modular.rpc.json;

import io.github.nhtuan10.modular.rpc.HelloWorldServer;
import io.grpc.*;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.ClientCalls;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * A simple client that requests a greeting from the {@link HelloWorldServer}.
 */
public class HelloWorldClientJson {
    private static final Logger logger = Logger.getLogger(HelloWorldClientJson.class.getName());

    private final GreeterGrpc.GreeterBlockingStub blockingStub;
    private final Channel channel;

    /**
     * Construct client for accessing HelloWorld server using the existing channel.
     */
    public HelloWorldClientJson(Channel channel) {
        // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
        // shut it down.

        // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
        this.channel = channel;
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    /**
     * Say hello to server.
     */
    public void greet(String name) {
        logger.info("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        ClientCall<HelloWorldJson.HelloRequest2, HelloWorldJson.HelloReply2> call =
                channel.newCall(HelloWorldJson.SAY_HELLO_METHOD_DESCRIPTOR, CallOptions.DEFAULT);
        HelloWorldJson.HelloRequest2 req = new HelloWorldJson.HelloRequest2("Dan");
        HelloWorldJson.HelloReply2 res = ClientCalls.blockingUnaryCall(call, req);
        logger.info("Greeting: " + res.message());

//        ListenableFuture<HelloWorldGson.HelloReply2> res = ClientCalls.futureUnaryCall(call, req);
//        Futures.addCallback(res, new FutureCallback<>() {
//            @Override
//            public void onSuccess(HelloWorldGson.HelloReply2 result) {
//                logger.info("Greeting: " + result.message());
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//                Status status = Status.fromThrowable(t);
//                logger.log(Level.WARNING, "RPC failed: {0}", status);
//            }
//        }, Executors.newSingleThreadExecutor());
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting. The second argument is the target server.
     */
    public static void main(String[] args) throws Exception {
        String user = "world";
        // Access a service running on the local machine on port 50051
        String target = "localhost:50051";
        // Allow passing in the user and target strings as command line arguments
        if (args.length > 0) {
            if ("--help".equals(args[0])) {
                System.err.println("Usage: [name [target]]");
                System.err.println("");
                System.err.println("  name    The name you wish to be greeted by. Defaults to " + user);
                System.err.println("  target  The server to connect to. Defaults to " + target);
                System.exit(1);
            }
            user = args[0];
        }
        if (args.length > 1) {
            target = args[1];
        }

        // Create a communication channel to the server, known as a Channel. Channels are thread-safe
        // and reusable. It is common to create channels at the beginning of your application and reuse
        // them until the application shuts down.
        //
        // For the example we use plaintext insecure credentials to avoid needing TLS certificates. To
        // use TLS, use TlsChannelCredentials instead.=
        ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create())
                .build();
        try {
            HelloWorldClientJson client = new HelloWorldClientJson(channel);
            client.greet(user);
        } finally {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
