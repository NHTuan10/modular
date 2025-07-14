package com.example.vtweb.launcher;

import io.github.nhtuan10.modular.api.Modular;
import io.github.nhtuan10.modular.api.module.ModuleLoadConfiguration;
import io.github.nhtuan10.modular.api.module.ModuleLoader;
import io.github.nhtuan10.sample.api.service.SampleService;
import io.github.nhtuan10.sample.api.service.ServiceException;
import io.github.nhtuan10.sample.api.service.SomeData;
import io.github.nhtuan10.sample.api.service.SomeInterface;

import java.util.List;

public class Main {
    public static void main(String[] args) {
//        ModuleLoader.ModuleDetail moduleDetail2 = Modular.startModuleSync("modular-sample-plugin-1", List.of("mvn://io.github.nhtuan10/modular-sample-plugin-1/0.0.1"), List.of("io.github.nhtuan10.sample.service", "io.github.nhtuan10.sample.util"));
        ModuleLoader.ModuleDetail moduleDetail2 = Modular.startModuleSyncWithMainClass("modular-sample-plugin-1", List.of("mvn://io.github.nhtuan10/modular-sample-plugin-1/0.0.1"), "io.github.nhtuan10.sample.service.ServiceImpl", List.of("io.github.nhtuan10.sample.service", "io.github.nhtuan10.sample.util"));
//        ModuleLoader.ModuleDetail moduleDetail3 = Modular.startModuleSync("modular-sample-plugin-2", List.of("mvn://io.github.nhtuan10/modular-sample-plugin-2/0.0.1"), List.of("io.github.nhtuan10.sample.service"));
        var plugin2Config = ModuleLoadConfiguration.builder()
                .locationUris(List.of("mvn://io.github.nhtuan10/modular-sample-plugin-2/0.0.1"))
//                .locationUris(List.of("mvn://io.github.nhtuan10/modular-sample-plugin-1/0.0.1", "mvn://io.github.nhtuan10/modular-sample-plugin-2/0.0.1"))
                .packagesToScan(List.of("io.github.nhtuan10.sample.service"))
                .allowNonAnnotatedServices(true)
                .build();
        ModuleLoader.ModuleDetail moduleDetail3 = Modular.startModuleSync("modular-sample-plugin-2", plugin2Config);
        System.out.println("Finished with modular-sample-plugin");
//        Modular.startModuleSyncWithMainClass("modular-sample-plugin2", List.of("mvn://io.github.nhtuan10/modular-sample-plugin/0.0.1"), "MainClass", List.of("io.github.nhtuan10.sample.service", "io.github.nhtuan10.sample.util"));
//        m.startModuleSyncWithMainClass("my-kafka-tool", List.of(
//                "file:///Users/tuan/Library/CloudStorage/OneDrive-Personal/CS/Java/MyKafkaTool/my-kafka-tool-main/target/my-kafka-tool-main-0.1.1-SNAPSHOT.jar",
//                "file:///Users/tuan/Library/CloudStorage/OneDrive-Personal/CS/Java/MyKafkaTool/my-kafka-tool-main/target/my-kafka-tool-main-0.1.1-SNAPSHOT/my-kafka-tool-main-0.1.1-SNAPSHOT.jar" ), "io.github.nhtuan10.mykafkatool.MyKafkaToolLauncher", "");
        System.out.println("Load from 1 module only ---");
        Modular.getModularServices(SampleService.class, "modular-sample-plugin-2").forEach(sampleService -> {
            try {
                sampleService.test();
            } catch (ServiceException e) {
                throw new RuntimeException(e);
            }
        });


        Modular.getModularServices(SomeInterface.class).forEach(SomeInterface::someInterfaceMethod);

//        BlockingQueue<SomeData> q = Modular.getBlockingQueue("testBlockingQueue", SomeData.class);
//        for (int i = 0; i < 2; i++) {
////            SomeData a = q.poll();
//            SomeData a = q.take();
//            System.out.println("Main-class: Polling from testQueue: " + a);
////            Thread.sleep(500);
//        }


//        Modular.getModularServices(SampleService.class, false).parallelStream().forEach(s -> {
        Modular.getModularServices(SampleService.class).parallelStream().forEach(s -> {
            try {
                System.out.println("Equals: " + s.equals(s));
                System.out.println("Equals: " + s.equals(new SampleService() {
                    @Override
                    public void test() {

                    }

                    @Override
                    public String testObjectArray(SomeData[] in) {
                        return "";
                    }

                    @Override
                    public List<SomeData> testObjectList(List<SomeData> in) {
                        return List.of();
                    }

                    @Override
                    public SomeData testReturn(SomeData in) {
                        return null;
                    }
                }));
                System.out.println("Hash code: " + s.hashCode());
                s.test();
                SomeData d = new SomeData("input testReturn");
                Object result = s.testReturn(d);
                System.out.println("testReturn: " + result);
                System.out.println("d.getName(): " + d.getName());
                var inArr = new SomeData[]{new SomeData("input testObjectArray")};
                System.out.println("Return from testObjectArray: " + s.testObjectArray(inArr));
                System.out.println("In array: " + inArr[0]);
//            var list = new ArrayList<SomeData>();
//            list.add(new SomeData("input testObjectList-1"));
//            list.add(new SomeData("input testObjectList-2"));
                var list = List.of(new SomeData("input testObjectList-1"), new SomeData("input testObjectList-2"));
                System.out.println("Return from testObjectList: " + s.testObjectList(list));
                System.out.println("In list: " + list);
            } catch (ServiceException e) {
                e.printStackTrace();
            }
        });

    }
}
