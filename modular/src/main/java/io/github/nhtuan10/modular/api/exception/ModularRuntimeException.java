package io.github.nhtuan10.modular.api.exception;

public class ModularRuntimeException extends RuntimeException {
    public ModularRuntimeException(String message) {
        super(message);
    }

    public ModularRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
