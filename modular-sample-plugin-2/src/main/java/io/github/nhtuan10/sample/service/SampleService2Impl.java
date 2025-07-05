package io.github.nhtuan10.sample.service;

import io.github.nhtuan10.modular.api.annotation.ModularConfiguration;
import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.sample.api.service.SampleService;
import io.github.nhtuan10.sample.api.service.SampleService2;
import io.github.nhtuan10.sample.api.service.SomeData;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@EqualsAndHashCode
@ModularService
public class SampleService2Impl implements SampleService2, SampleService {
    @Override
    public void test() {
        log.info("Service 2 Impl: Invoke test");
    }

    @Override
    public SomeData testReturn(SomeData in) {
        return new SomeData(in.getName() + " from SampleService2Impl");
    }

    @Override
    public String testStringParam(String in) {
        return "SampleService2Impl ->" + in;
    }
}

@ModularConfiguration
class Config {
    @ModularService
    SampleService2Impl sampleService2FromMethod() {
        return new SampleService2Impl();
    }
}