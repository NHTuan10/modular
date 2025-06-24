package com.example.vtweb.launcher;

import io.github.nhtuan10.modular.api.module.ModuleLoader;
import io.github.nhtuan10.sample.api.service.SampleService;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        ModuleLoader m = ModuleLoader.getInstance();
        m.startModuleSync("modular-sample-plugin", List.of("mvn://io.github.nhtuan10/modular-sample-plugin/0.0.1"), "io.github.nhtuan10.sample.service");
//        m.startModuleSyncWithMainClass("my-kafka-tool", List.of(
//                "file:///Users/tuan/Library/CloudStorage/OneDrive-Personal/CS/Java/MyKafkaTool/my-kafka-tool-main/target/my-kafka-tool-main-0.1.1-SNAPSHOT.jar",
//                "file:///Users/tuan/Library/CloudStorage/OneDrive-Personal/CS/Java/MyKafkaTool/my-kafka-tool-main/target/my-kafka-tool-main-0.1.1-SNAPSHOT/my-kafka-tool-main-0.1.1-SNAPSHOT.jar" ), "io.github.nhtuan10.mykafkatool.MyKafkaToolLauncher", "");
        SampleService sampleService = ModuleLoader.getContext().<SampleService>getModularServices(SampleService.class).get(0);
        sampleService.test();
    }
}
