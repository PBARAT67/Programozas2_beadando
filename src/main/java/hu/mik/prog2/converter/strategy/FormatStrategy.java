package hu.mik.prog2.converter.strategy;

import hu.mik.prog2.converter.exception.ConverterException;
import hu.mik.prog2.converter.model.Document;
import hu.mik.prog2.converter.model.FormatType;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Strategy interface defining the contract for parsing and serializing documents
 * in a specific file format.
 * Expresses a stable abstraction for Strategy and Polymorphism requirements.
 */
public interface FormatStrategy {

    /**
     * Parses an input stream into an in-memory Document representation.
     *
     * @param inputStream the source stream to read from
     * @return the parsed Document
     * @throws ConverterException if parsing fails or structure is invalid
     */
    Document parse(InputStream inputStream) throws ConverterException;

    /**
     * Serializes an in-memory Document into an output stream.
     *
     * @param document     the document to write
     * @param outputStream the destination stream to write to
     * @throws ConverterException if writing fails
     */
    void write(Document document, OutputStream outputStream) throws ConverterException;

    /**
     * Returns the format type handled by this strategy.
     */
    FormatType getFormatType();
}
