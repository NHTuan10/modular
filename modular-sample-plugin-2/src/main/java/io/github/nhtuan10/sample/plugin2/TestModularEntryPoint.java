package io.github.nhtuan10.sample.plugin2;

import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.modular.entry.ModularEntryPoint;
import io.github.nhtuan10.sample.api.service.SomeData;
import lombok.extern.slf4j.Slf4j;

@ModularService
@Slf4j
public class TestModularEntryPoint implements ModularEntryPoint<SomeData, SomeData> {
    @Override
    public SomeData run(SomeData parameter) {
        log.info("processed " + (parameter != null ? parameter.getName() : "unknown"));
        return new SomeData("processed " + (parameter != null ? parameter.getName() : "unknown"));
    }

}
