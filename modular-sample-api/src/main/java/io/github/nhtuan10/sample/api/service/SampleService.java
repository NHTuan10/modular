package io.github.nhtuan10.sample.api.service;

import io.github.nhtuan10.modular.api.annotation.ModularService;

@ModularService
public interface SampleService {
    default void testDefault() {
        System.out.println("SampleService: testDefault method try to call test");
        test();
    }
    void test();
    SomeData testReturn(SomeData in);

    String testStringParam(String in);
}
