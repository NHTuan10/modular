package io.github.nhtuan10.modular.api.exception;

public class AnnotationProcessingRuntimeException extends RuntimeException {
    public AnnotationProcessingRuntimeException(String message) {
        super(message);
    }

    public AnnotationProcessingRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public AnnotationProcessingRuntimeException(String moduleName, String message) {
        super(message);
    }

    public AnnotationProcessingRuntimeException(String moduleName, String message, Throwable cause) {
        super(message, cause);
    }
}
