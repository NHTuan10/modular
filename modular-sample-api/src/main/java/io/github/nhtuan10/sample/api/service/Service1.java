package io.github.nhtuan10.sample.api.service;

import io.github.nhtuan10.modular.api.annotation.ModularMethod;
import io.github.nhtuan10.modular.api.annotation.ModularService;

@ModularService
public interface Service1 {
     @ModularMethod
     String message(SomeData someData);
}