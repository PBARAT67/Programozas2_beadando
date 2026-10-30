package hu.mik.prog2.converter.strategy;

import hu.mik.prog2.converter.exception.ConverterException;
import hu.mik.prog2.converter.exception.InvalidFileStructureException;
import hu.mik.prog2.converter.model.Document;
import hu.mik.prog2.converter.model.FormatType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Abstract base class for format strategies providing shared stream utilities
 * and validation routines.
 */
public abstract class AbstractFormatStrategy implements FormatStrategy {

    protected static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private final FormatType formatType;

    protected AbstractFormatStrategy(FormatType formatType) {
        this.formatType = Objects.requireNonNull(formatType, "formatType cannot be null");
    }

    @Override
    public FormatType getFormatType() {
        return formatType;
    }

    protected BufferedReader createReader(InputStream in) {
        return new BufferedReader(new InputStreamReader(in, DEFAULT_CHARSET));
    }

    protected BufferedWriter createWriter(OutputStream out) {
        return new BufferedWriter(new OutputStreamWriter(out, DEFAULT_CHARSET));
    }

    protected void validateDocumentForWriting(Document document) throws ConverterException {
        if (document == null) {
            throw new InvalidFileStructureException("Cannot write a null document.");
        }
        if (document.getHeader() == null || document.getHeader().getColumns().isEmpty()) {
            throw new InvalidFileStructureException("Cannot write document without header columns.");
        }
    }
}
