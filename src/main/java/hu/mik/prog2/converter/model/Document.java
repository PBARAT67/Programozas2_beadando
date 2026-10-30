package hu.mik.prog2.converter.model;

import hu.mik.prog2.converter.exception.InvalidFileStructureException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the in-memory intermediate representation (IR) of tabular/structured data.
 * Demonstrates composition: a Document is composed of a DocumentHeader and DocumentRecords.
 */
public final class Document {

    private final DocumentHeader header;
    private final List<DocumentRecord> records;

    /**
     * Constructs a Document with the specified header and list of records.
     *
     * @param header  the document header
     * @param records the list of records
     * @throws InvalidFileStructureException if header is null
     */
    public Document(DocumentHeader header, List<DocumentRecord> records) throws InvalidFileStructureException {
        if (header == null) {
            throw new InvalidFileStructureException("Document header cannot be null.");
        }
        this.header = header;

        if (records == null) {
            this.records = Collections.emptyList();
        } else {
            this.records = Collections.unmodifiableList(new ArrayList<>(records));
        }
    }

    public DocumentHeader getHeader() {
        return header;
    }

    public List<DocumentRecord> getRecords() {
        return records;
    }

    public int getRecordCount() {
        return records.size();
    }

    public int getColumnCount() {
        return header.size();
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Document document)) return false;
        return Objects.equals(header, document.header) && Objects.equals(records, document.records);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, records);
    }

    @Override
    public String toString() {
        return "Document{" +
                "columns=" + header.size() +
                ", records=" + records.size() +
                '}';
    }
}
