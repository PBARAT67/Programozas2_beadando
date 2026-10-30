package hu.mik.prog2.converter.model;

import hu.mik.prog2.converter.exception.UnsupportedFormatException;

import java.util.Arrays;
import java.util.Locale;

/**
 * Enumeration of supported file formats in the converter system.
 */
public enum FormatType {
    CSV("csv", "text/csv"),
    JSON("json", "application/json"),
    XML("xml", "application/xml");

    private final String extension;
    private final String mimeType;

    FormatType(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    /**
     * Resolves format from a file path or extension string.
     *
     * @param fileNameOrExtension file name, path, or extension
     * @return the corresponding FormatType
     * @throws UnsupportedFormatException if format cannot be resolved
     */
    public static FormatType fromFileNameOrExtension(String fileNameOrExtension) throws UnsupportedFormatException {
        if (fileNameOrExtension == null || fileNameOrExtension.trim().isEmpty()) {
            throw new UnsupportedFormatException("empty");
        }

        String cleaned = fileNameOrExtension.trim().toLowerCase(Locale.ROOT);
        int dotIdx = cleaned.lastIndexOf('.');
        String ext = (dotIdx >= 0 && dotIdx < cleaned.length() - 1) ? cleaned.substring(dotIdx + 1) : cleaned;

        for (FormatType format : values()) {
            if (format.extension.equalsIgnoreCase(ext)) {
                return format;
            }
        }

        throw new UnsupportedFormatException(ext);
    }
}
