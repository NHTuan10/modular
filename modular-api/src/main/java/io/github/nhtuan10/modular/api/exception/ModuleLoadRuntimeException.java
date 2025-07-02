package io.github.nhtuan10.modular.api.exception;

public class ModuleLoadRuntimeException extends RuntimeException {
    public ModuleLoadRuntimeException(String message) {
        super(message);
    }

    public ModuleLoadRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModuleLoadRuntimeException(String moduleName, String message) {
        super(message);
    }

    public ModuleLoadRuntimeException(String moduleName, String message, Throwable cause) {
        super(message, cause);
    }
}
