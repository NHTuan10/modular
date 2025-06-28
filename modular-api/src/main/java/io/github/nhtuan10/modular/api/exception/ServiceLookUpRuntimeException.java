package io.github.nhtuan10.modular.api.exception;

public class ServiceLookUpRuntimeException extends RuntimeException {
    public ServiceLookUpRuntimeException(String message) {
        super(message);
    }

    public ServiceLookUpRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
