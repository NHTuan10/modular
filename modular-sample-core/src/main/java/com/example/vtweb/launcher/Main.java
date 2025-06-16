package com.example.vtweb.launcher;

import io.github.nhtuan10.modular.api.module.ModuleLoader;
import io.github.nhtuan10.sample.api.service.SampleService;

import java.lang.reflect.InvocationTargetException;

public class Main {
    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        ModuleLoader m = ModuleLoader.getInstance();
        m.startModuleSync("modular-sample-plugin", "mvn://io.github.nhtuan10/modular-sample-plugin/0.0.1-SNAPSHOT", "io.github.nhtuan10.sample.service");
        SampleService sampleService = ModuleLoader.getContext().<SampleService>getModularServices(SampleService.class).get(0);
        sampleService.test();
    }
}
