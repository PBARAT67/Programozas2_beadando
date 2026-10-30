package hu.mik.prog2.converter.exception;

/**
 * Base checked exception for all domain-specific errors in the file converter system.
 */
public class ConverterException extends Exception {
    
    public ConverterException(String message) {
        super(message);
    }

    public ConverterException(String message, Throwable cause) {
        super(message, cause);
    }
}
