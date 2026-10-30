package hu.mik.prog2.converter.service;

import hu.mik.prog2.converter.exception.ConverterException;
import hu.mik.prog2.converter.model.ConversionHistoryItem;
import hu.mik.prog2.converter.persistence.HistoryRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Service managing in-memory conversion history and coordinating persistence.
 * Demonstrates collection manipulation and sorting using Comparable and Comparator.
 */
public class HistoryService {

    private final HistoryRepository repository;
    private final List<ConversionHistoryItem> historyList;

    public HistoryService(HistoryRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.historyList = new ArrayList<>();
        loadFromPersistence();
    }

    private void loadFromPersistence() {
        try {
            List<ConversionHistoryItem> loaded = repository.loadAll();
            historyList.clear();
            historyList.addAll(loaded);
        } catch (ConverterException e) {
            System.err.println("Warning: Could not load history from persistence: " + e.getMessage());
        }
    }

    /**
     * Records a new conversion item in memory and appends to persistent storage.
     */
    public synchronized void record(ConversionHistoryItem item) {
        if (item == null) return;
        historyList.add(item);
        try {
            repository.append(item);
        } catch (ConverterException e) {
            System.err.println("Warning: Failed to persist history item: " + e.getMessage());
        }
    }

    /**
     * Returns an unmodifiable list of history items in natural sort order (Comparable: newest first).
     */
    public synchronized List<ConversionHistoryItem> getHistorySortedNatural() {
        List<ConversionHistoryItem> copy = new ArrayList<>(historyList);
        Collections.sort(copy); // Uses Comparable<ConversionHistoryItem>
        return Collections.unmodifiableList(copy);
    }

    /**
     * Returns history items sorted by a custom Comparator.
     */
    public synchronized List<ConversionHistoryItem> getHistorySorted(Comparator<ConversionHistoryItem> comparator) {
        Objects.requireNonNull(comparator, "comparator cannot be null");
        List<ConversionHistoryItem> copy = new ArrayList<>(historyList);
        copy.sort(comparator);
        return Collections.unmodifiableList(copy);
    }

    /**
     * Clears all history in-memory and persistently.
     */
    public synchronized void clearHistory() throws ConverterException {
        historyList.clear();
        repository.clear();
    }

    public synchronized int getCount() {
        return historyList.size();
    }

    public synchronized long getSuccessfulCount() {
        return historyList.stream().filter(ConversionHistoryItem::isSuccess).count();
    }

    public synchronized long getFailedCount() {
        return historyList.stream().filter(item -> !item.isSuccess()).count();
    }

    public synchronized long getTotalBytesProcessed() {
        return historyList.stream().mapToLong(ConversionHistoryItem::getFileSizeBytes).sum();
    }
}
