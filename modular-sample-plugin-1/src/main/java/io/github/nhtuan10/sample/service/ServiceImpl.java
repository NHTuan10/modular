package io.github.nhtuan10.sample.service;

import io.github.nhtuan10.modular.api.Modular;
import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.sample.api.service.SampleService;
import io.github.nhtuan10.sample.api.service.SampleService2;
import io.github.nhtuan10.sample.api.service.SomeData;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ToString
@ModularService
public class ServiceImpl extends BaseService implements SampleService {
    @Override
    public void test() {
        log.info("Service Impl: Invoke test");
        List<SampleService2> sampleService2List = Modular.getModularServices(SampleService2.class);
        for (SampleService2 sampleService2 : sampleService2List) {
            sampleService2.test();
        }
        baseMethod();
    }

    @Override
    public SomeData testReturn(SomeData in) {
        in.setName("set by sample-plugin-1 ServiceImpl#testReturn");
        return new SomeData(in.getName() + " from ServiceImpl");
    }

    @Override
    public String testStringParam(String in) {
        return "ServiceImpl ->" + in;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
