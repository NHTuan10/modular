package io.github.nhtuan10.sample.launcher;

import io.github.nhtuan10.modular.api.Modular;
import io.github.nhtuan10.modular.api.module.ModuleLoadConfiguration;
import io.github.nhtuan10.modular.api.module.ModuleLoader;
import io.github.nhtuan10.sample.api.service.ExcludedMe;
import io.github.nhtuan10.sample.api.service.SampleService;
import io.github.nhtuan10.sample.api.service.SomeData;
import io.github.nhtuan10.sample.api.service.SomeInterface;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

@Slf4j
public class Main {
    public static void main(String[] args) {
//        ModuleLoader.ModuleDetail moduleDetail2 = Modular.startModuleSync("modular-sample-plugin-1", List.of("mvn://io.github.nhtuan10/modular-sample-plugin-1/0.0.2"), List.of("io.github.nhtuan10.sample.service", "io.github.nhtuan10.sample.util"));
//        ModuleLoader.ModuleDetail moduleDetail2 = Modular.startModuleSyncWithMainClass("modular-sample-plugin-1",
//                List.of("mvn://io.github.nhtuan10/modular-sample-plugin-1/0.0.2"), "io.github.nhtuan10.sample.service.ServiceImpl", List.of("io.github.nhtuan10.sample.service", "io.github.nhtuan10.sample.util"));
//        ModuleLoader.ModuleDetail moduleDetail3 = Modular.startModuleSync("modular-sample-plugin-2", List.of("mvn://io.github.nhtuan10/modular-sample-plugin-2/0.0.2"), List.of("io.github.nhtuan10.sample.service"));

        ModuleLoadConfiguration plugin1Config = ModuleLoadConfiguration.builder()
                .locationUris(List.of("mvn://io.github.nhtuan10/modular-sample-plugin-1/0.0.2"))
//                .locationUris(List.of("mvn://io.github.nhtuan10/modular-sample-plugin-1/0.0.2", "mvn://io.github.nhtuan10/modular-sample-plugin-2/0.0.2"))
                .packagesToScan(List.of("io.github.nhtuan10.sample.plugin1", "io.github.nhtuan10.sample.util"))
                .mainClass("io.github.nhtuan10.sample.plugin1.ServiceImpl")
                .modularClassLoaderName("commonCL")
                .allowNonAnnotatedServices(true)
                .prefixesLoadedBySystemClassLoader(Set.of(ExcludedMe.class.getName()))
                .jpmsModuleName("io.github.nhtuan10.sample.plugin1")
                .build();

        ModuleLoadConfiguration plugin2Config = ModuleLoadConfiguration.builder()
                .locationUris(List.of("mvn://io.github.nhtuan10/modular-sample-plugin-2/0.0.2"))
//                .locationUris(List.of("mvn://io.github.nhtuan10/modular-sample-plugin-1/0.0.2", "mvn://io.github.nhtuan10/modular-sample-plugin-2/0.0.2"))
                .packagesToScan(List.of("io.github.nhtuan10.sample.plugin2"))
                .modularClassLoaderName("commonCL")
                .allowNonAnnotatedServices(true)
                .jpmsModuleName("io.github.nhtuan10.sample.plugin2")
                .build();


        ModuleLoader.ModuleDetail moduleDetail3 = Modular.startModuleSync("modular-sample-plugin-1", plugin1Config);
        ModuleLoader.ModuleDetail moduleDetail4 = Modular.startModuleSync("modular-sample-plugin-2", plugin2Config);
//        moduleDetail3.join();
        log.info("Finished with modular-sample-plugin");
//        Modular.startModuleSyncWithMainClass("modular-sample-plugin2", List.of("mvn://io.github.nhtuan10/modular-sample-plugin/0.0.2"), "MainClass", List.of("io.github.nhtuan10.sample.service", "io.github.nhtuan10.sample.util"));
//        m.startModuleSyncWithMainClass("my-kafka-tool", List.of(
//                "file:///Users/tuan/Library/CloudStorage/OneDrive-Personal/CS/Java/MyKafkaTool/my-kafka-tool-main/target/my-kafka-tool-main-0.1.1-SNAPSHOT.jar",
//                "file:///Users/tuan/Library/CloudStorage/OneDrive-Personal/CS/Java/MyKafkaTool/my-kafka-tool-main/target/my-kafka-tool-main-0.1.1-SNAPSHOT/my-kafka-tool-main-0.1.1-SNAPSHOT.jar" ), "io.github.nhtuan10.mykafkatool.MyKafkaToolLauncher", "");
        log.info("Load from 1 module only ---");
        Modular.getModularServices(SampleService.class, "modular-sample-plugin-2").forEach(sampleService -> {
            try {
                sampleService.test();
            } catch (Exception e) {
                e.getCause().printStackTrace();
            }
        });


        Modular.getModularServices(SomeInterface.class).forEach(SomeInterface::someInterfaceMethod);

//        BlockingQueue<SomeData> q = Modular.getBlockingQueue("testBlockingQueue", SomeData.class);
//        for (int i = 0; i < 2; i++) {
////            SomeData a = q.poll();
//            SomeData a = q.take();
//            log.info("Main-class: Polling from testQueue: " + a);
////            Thread.sleep(500);
//        }


        Modular.getModularServices(SampleService.class, false).parallelStream()
//                .forEach(s -> {
//        Modular.getModularServices(SampleService.class, false)
//        Modular.getModularServices(SampleService.class)
//                .parallelStream()
                .forEach(s -> {
                    log.info("Equals: " + s.equals(s));
                    log.info("Equals: " + s.equals(new SampleService() {
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
                    log.info("Hash code: " + s.hashCode());

                    try {
                        s.test();
                    } catch (Exception e) {
                        e.getCause().printStackTrace();
                    }
                    SomeData d = new SomeData("input testReturn");
                    Object result = s.testReturn(d);
                    log.info("testReturn: " + result);
                    log.info("d.getName(): " + d.getName());
                    SomeData[] inArr = new SomeData[]{new SomeData("input testObjectArray")};
                    log.info("Return from testObjectArray: " + s.testObjectArray(inArr));
                    log.info("In array: " + inArr[0]);
//            var list = new ArrayList<SomeData>();
//            list.add(new SomeData("input testObjectList-1"));
//            list.add(new SomeData("input testObjectList-2"));
                    List<SomeData> list = List.of(new SomeData("input testObjectList-1"), new SomeData("input testObjectList-2"));
                    log.info("Return from testObjectList: " + s.testObjectList(list));
                    log.info("In list: " + list);

        });

    }
}
