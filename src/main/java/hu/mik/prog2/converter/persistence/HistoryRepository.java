package hu.mik.prog2.converter.persistence;

import hu.mik.prog2.converter.exception.ConverterException;
import hu.mik.prog2.converter.model.ConversionHistoryItem;

import java.util.List;

/**
 * Persistence abstraction for storing and loading conversion history records
 * between application runs.
 */
public interface HistoryRepository {

    /**
     * Loads all persisted conversion history items.
     *
     * @return list of history items, or empty list if no history exists
     * @throws ConverterException if reading fails
     */
    List<ConversionHistoryItem> loadAll() throws ConverterException;

    /**
     * Persists all given history items to persistent storage.
     *
     * @param items the history items to save
     * @throws ConverterException if writing fails
     */
    void saveAll(List<ConversionHistoryItem> items) throws ConverterException;

    /**
     * Appends a single new history item to the persistent storage.
     *
     * @param item the new item to persist
     * @throws ConverterException if saving fails
     */
    void append(ConversionHistoryItem item) throws ConverterException;

    /**
     * Clears all persisted history.
     *
     * @throws ConverterException if clearing fails
     */
    void clear() throws ConverterException;
}
