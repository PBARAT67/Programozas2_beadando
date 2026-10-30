package hu.mik.prog2.converter.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single row or entry in a Document.
 * Stores field values mapped by column name.
 */
public final class DocumentRecord {

    private final Map<String, String> data;

    /**
     * Constructs a DocumentRecord from a map of column names to values.
     *
     * @param data map of column names to values
     */
    public DocumentRecord(Map<String, String> data) {
        if (data == null) {
            this.data = Collections.emptyMap();
        } else {
            Map<String, String> cleanData = new LinkedHashMap<>(data.size());
            for (Map.Entry<String, String> entry : data.entrySet()) {
                String key = entry.getKey() != null ? entry.getKey().trim() : "";
                String val = entry.getValue() != null ? entry.getValue() : "";
                cleanData.put(key, val);
            }
            this.data = Collections.unmodifiableMap(cleanData);
        }
    }

    /**
     * Retrieves the value for a given column name.
     *
     * @param column column name
     * @return cell value, or null if column does not exist
     */
    public String getValue(String column) {
        if (column == null) return null;
        return data.get(column.trim());
    }

    /**
     * Retrieves the value for a given column name with a default fallback.
     */
    public String getValueOrDefault(String column, String defaultValue) {
        String val = getValue(column);
        return val != null ? val : defaultValue;
    }

    /**
     * Returns an unmodifiable map of all fields in this record.
     */
    public Map<String, String> getData() {
        return data;
    }

    public int size() {
        return data.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentRecord that)) return false;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    @Override
    public String toString() {
        return "DocumentRecord" + data;
    }
}
