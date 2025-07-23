package io.github.nhtuan10.sample.api.service;

import io.github.nhtuan10.modular.api.annotation.ModularService;

import java.util.List;

@ModularService
public interface SampleService {
    default void testDefault() throws ServiceException {
        System.out.println("SampleService: testDefault method try to call test");
        test();
    }

    void test() throws ServiceException;
    SomeData testReturn(SomeData in);

    String testObjectArray(SomeData[] in);

    List<SomeData> testObjectList(List<SomeData> in);
}
