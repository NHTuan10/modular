package io.github.nhtuan10.modular.api.exception;

public class ModularServiceInvocationException extends RuntimeException {
    public ModularServiceInvocationException(Exception e) {
        super(e);
    }

    public ModularServiceInvocationException(String message, Exception e) {
        super(message, e);
    }
}
