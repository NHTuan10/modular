package io.github.nhtuan10.modular.api.exception;

public class ModularSerializationException extends RuntimeException {
    public ModularSerializationException(Exception e) {
        super(e);
    }

    public ModularSerializationException(String message, Exception e) {
        super(message, e);
    }
}
