package hu.mik.prog2.converter.exception;

/**
 * Exception thrown when an unrecognized or unsupported file format or extension is requested.
 */
public class UnsupportedFormatException extends ConverterException {

    private final String extensionOrFormat;

    public UnsupportedFormatException(String extensionOrFormat) {
        super("Unsupported file format or extension: '" + extensionOrFormat + "'. Supported formats: CSV, JSON, XML.");
        this.extensionOrFormat = extensionOrFormat;
    }

    public String getExtensionOrFormat() {
        return extensionOrFormat;
    }
}
