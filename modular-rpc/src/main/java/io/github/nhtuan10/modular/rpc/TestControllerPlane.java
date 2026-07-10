package io.github.nhtuan10.modular.rpc;

//import io.grpc.xds.shaded.io.envoyproxy.controlplane.cache.v3.SimpleCache;
//import io.envoyproxy.controlplane.server.V3DiscoveryServer;

public class TestControllerPlane {
    public static void main(String[] args) {


// 1. Initialize a snapshot cache for cluster resources
//        SimpleCache<String> cache = new SimpleCache<>(node -> "global-group");
//
//// 2. Pass the cache into the Envoy server framework
//        V3DiscoveryServer v3DiscoveryServer = new V3DiscoveryServer(cache);
//
//// 3. Register the server to standard gRPC listeners
//        Server server = ServerBuilder.forPort(9000)
//                .addService(v3DiscoveryServer.getAggregatedDiscoveryServiceImpl()) // ADS
//                .addService(v3DiscoveryServer.getClusterDiscoveryServiceImpl())    // CDS
//                .addService(v3DiscoveryServer.getEndpointDiscoveryServiceImpl())   // EDS
//                .addService(v3DiscoveryServer.getListenerDiscoveryServiceImpl())   // LDS
//                .addService(v3DiscoveryServer.getRouteDiscoveryServiceImpl())      // RDS
//                .build()
//                .start();

    }
}
