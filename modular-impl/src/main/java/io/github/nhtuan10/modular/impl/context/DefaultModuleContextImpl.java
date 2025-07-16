package io.github.nhtuan10.modular.impl.context;

import io.github.nhtuan10.modular.api.classloader.ModularClassLoader;
import io.github.nhtuan10.modular.api.exception.ModuleLoadRuntimeException;
import io.github.nhtuan10.modular.api.module.ModuleContext;
import io.github.nhtuan10.modular.api.module.ModuleLoader;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DefaultModuleContextImpl implements ModuleContext {
    private ModuleLoader moduleLoader;

    @Override
    public void notifyModuleReady() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        if (classLoader instanceof ModularClassLoader) {
            String moduleName = getCurrentModuleName();
            moduleLoader.notifyModuleReady(moduleName);
        } else {
            throw new ModuleLoadRuntimeException("Illegal invocation of ModularContext#notifyModuleReady");
        }
    }

    @Override
    public String getCurrentModuleName() {
        return moduleLoader.getCurrentModuleName();
    }
}
