package io.github.nhtuan10.modular.api.module;

public interface ModuleContext {
    void notifyModuleReady();

    void notifyModuleReady(String moduleName);

    String getCurrentModuleName();
}
