package hu.mik.prog2.converter.model;

import hu.mik.prog2.converter.exception.InvalidFileStructureException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the header (column names) of a structured document.
 * Demonstrates encapsulation through private fields, validation in constructor,
 * and defensive copying.
 */
public final class DocumentHeader {

    private final List<String> columns;

    /**
     * Constructs a DocumentHeader with validated, non-empty column names.
     *
     * @param columns list of column names
     * @throws InvalidFileStructureException if columns is null, empty, or contains blank names
     */
    public DocumentHeader(List<String> columns) throws InvalidFileStructureException {
        if (columns == null || columns.isEmpty()) {
            throw new InvalidFileStructureException("Document header must contain at least one column.");
        }

        List<String> cleanColumns = new ArrayList<>(columns.size());
        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i);
            if (col == null || col.trim().isEmpty()) {
                throw new InvalidFileStructureException("Column header at index " + i + " cannot be null or blank.");
            }
            cleanColumns.add(col.trim());
        }
        this.columns = Collections.unmodifiableList(cleanColumns);
    }

    /**
     * Returns an unmodifiable list of column names.
     */
    public List<String> getColumns() {
        return columns;
    }

    public int size() {
        return columns.size();
    }

    public String getColumn(int index) {
        return columns.get(index);
    }

    public boolean contains(String columnName) {
        if (columnName == null) return false;
        return columns.contains(columnName.trim());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentHeader that)) return false;
        return Objects.equals(columns, that.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columns);
    }

    @Override
    public String toString() {
        return "DocumentHeader" + columns;
    }
}
