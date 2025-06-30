package io.github.nhtuan10.sample.service;

import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.sample.api.service.SampleService2;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@EqualsAndHashCode
@ModularService
public class SampleService2Impl implements SampleService2 {
    @Override
    public void test() {
        log.info("Service 2 Impl: Invoke test");
    }
}
