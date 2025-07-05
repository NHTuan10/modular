package com.example.vtweb.launcher;

import io.github.nhtuan10.modular.api.Modular;
import io.github.nhtuan10.modular.api.module.ModuleLoader;
import io.github.nhtuan10.sample.api.service.SampleService;
import io.github.nhtuan10.sample.api.service.SomeData;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        ModuleLoader.ModuleDetail moduleDetail = Modular.startModuleSync("modular-sample-plugin-1", List.of("mvn://io.github.nhtuan10/modular-sample-plugin-1/0.0.1"), List.of("io.github.nhtuan10.sample.service", "io.github.nhtuan10.sample.util"));
//        ModuleLoader.ModuleDetail moduleDetail2 = Modular.startModuleSync("modular-sample-plugin-1", List.of("mvn://io.github.nhtuan10/modular-sample-plugin-1/0.0.1"), List.of("io.github.nhtuan10.sample.service", "io.github.nhtuan10.sample.util"));
        ModuleLoader.ModuleDetail moduleDetail3 = Modular.startModuleSync("modular-sample-plugin-2", List.of("mvn://io.github.nhtuan10/modular-sample-plugin-2/0.0.1"), List.of("io.github.nhtuan10.sample.service"));
        System.out.println("Finished with modular-sample-plugin");
//        Modular.startModuleSyncWithMainClass("modular-sample-plugin2", List.of("mvn://io.github.nhtuan10/modular-sample-plugin/0.0.1"), "MainClass", List.of("io.github.nhtuan10.sample.service", "io.github.nhtuan10.sample.util"));
//        m.startModuleSyncWithMainClass("my-kafka-tool", List.of(
//                "file:///Users/tuan/Library/CloudStorage/OneDrive-Personal/CS/Java/MyKafkaTool/my-kafka-tool-main/target/my-kafka-tool-main-0.1.1-SNAPSHOT.jar",
//                "file:///Users/tuan/Library/CloudStorage/OneDrive-Personal/CS/Java/MyKafkaTool/my-kafka-tool-main/target/my-kafka-tool-main-0.1.1-SNAPSHOT/my-kafka-tool-main-0.1.1-SNAPSHOT.jar" ), "io.github.nhtuan10.mykafkatool.MyKafkaToolLauncher", "");
        Modular.<SampleService>getModularServices(SampleService.class).forEach(s -> {

            System.out.println("Equals: " + s.equals(s));
            System.out.println("Equals: " + s.equals(new SampleService() {
                @Override
                public void test() {

                }

                @Override
                public String testStringParam(String in) {
                    return "";
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
            System.out.println("d.getName()" + d.getName());

            System.out.println("Return from testStringParam: " + s.testStringParam("input testStringParam"));
        });

    }
}
