package io.github.nhtuan10.sample.service;

import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.sample.api.service.SampleService;
import io.github.nhtuan10.sample.api.service.SomeData;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@ModularService
public class ServiceImpl extends BaseService implements SampleService {
    @Override
    public void test() {
        log.info("Service 2 Impl: Invoke test");
        baseMethod();
    }

    @Override
    public SomeData testReturn(SomeData in) {
        return new SomeData(in.name() + " from ServiceImpl");
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
