package io.github.nhtuan10.modular.impl.context;

import io.github.nhtuan10.modular.api.module.ModuleContext;
import io.github.nhtuan10.modular.api.module.ModuleLoader;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DefaultModuleContextImpl implements ModuleContext {
    private ModuleLoader moduleLoader;

    @Override
    public void notifyModuleReady() {
        // TODO: check if current class loader is modular class loader or not
//        ClassLoader classLoader = this.getClass().getClassLoader();
//        if (classLoader instanceof ModularClassLoader) {
            String moduleName = getCurrentModuleName();
            moduleLoader.notifyModuleReady(moduleName);
//        } else {
//            throw new ModuleLoadRuntimeException("Illegal invocation of ModularContext#notifyModuleReady");
//        }
    }

    @Override
    public void notifyModuleReady(String moduleName) {
        // TODO: check if modular class loader has module with name moduleName or not
//        if (this.getClass().getClassLoader() instanceof ModularClassLoader) {
//            ModularClassLoader modularClassLoader = (ModularClassLoader) this.getClass().getClassLoader();
//            if (modularClassLoader.getModuleNames().contains(moduleName)) {
                moduleLoader.notifyModuleReady(moduleName);
//            }
//        }
    }

    @Override
    public String getCurrentModuleName() {
        return moduleLoader.getCurrentModuleName();
    }
}
