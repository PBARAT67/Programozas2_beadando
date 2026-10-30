package hu.mik.prog2.converter.exception;

/**
 * Exception thrown when a file's structure violates domain rules
 * (e.g., missing header, mismatched column count, malformed tags or braces).
 */
public class InvalidFileStructureException extends ConverterException {

    private final int lineNumber;

    public InvalidFileStructureException(String message) {
        super(message);
        this.lineNumber = -1;
    }

    public InvalidFileStructureException(String message, int lineNumber) {
        super("At line " + lineNumber + ": " + message);
        this.lineNumber = lineNumber;
    }

    public InvalidFileStructureException(String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = -1;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
