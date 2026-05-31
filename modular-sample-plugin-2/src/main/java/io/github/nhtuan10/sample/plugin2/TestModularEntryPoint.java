package io.github.nhtuan10.sample.plugin2;

import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.modular.entry.ModularEntryPoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ModularService
@Slf4j
public class TestModularEntryPoint implements ModularEntryPoint<InputData, OutputData> {
    @Override
    public OutputData run(InputData parameter) {
        log.info("processed " + (parameter != null ? parameter.getName() : "unknown"));
        return new OutputData("processed " + (parameter != null ? parameter.getName() : "unknown"));
    }

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class InputData {
    String name;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class OutputData {
    String output;
}