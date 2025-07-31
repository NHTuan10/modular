package io.github.nhtuan10.modular.api.exception;

public class DuplicatedModuleLoadRuntimeException extends ModuleLoadRuntimeException {
    public DuplicatedModuleLoadRuntimeException(String message) {
        super(message);
    }

    public DuplicatedModuleLoadRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicatedModuleLoadRuntimeException(String moduleName, String message) {
        super(moduleName, message);
    }

    public DuplicatedModuleLoadRuntimeException(String moduleName, String message, Throwable cause) {
        super(moduleName, message, cause);
    }
}
