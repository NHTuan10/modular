package io.github.nhtuan10.modular.api.exception;

public class ModuleLoadRuntimeException extends RuntimeException {
    public ModuleLoadRuntimeException(String message) {
        super(message);
    }

    public ModuleLoadRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
