package io.github.nhtuan10.modular.api.exception;

public class ServiceInvocationRuntimeException extends RuntimeException {
    public ServiceInvocationRuntimeException(Exception e) {
        super(e);
    }

    public ServiceInvocationRuntimeException(String message, Exception e) {
        super(message, e);
    }
}
