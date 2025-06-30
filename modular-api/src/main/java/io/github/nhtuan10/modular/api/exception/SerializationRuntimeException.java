package io.github.nhtuan10.modular.api.exception;

public class SerializationRuntimeException extends RuntimeException {
    public SerializationRuntimeException(Exception e) {
        super(e);
    }

    public SerializationRuntimeException(String message, Exception e) {
        super(message, e);
    }
}
