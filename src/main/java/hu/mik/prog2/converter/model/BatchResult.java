package hu.mik.prog2.converter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generic container representing the outcome of a batch operation over elements of type T.
 * Demonstrates deliberate generic design in the domain model.
 *
 * @param <T> the type of items processed in the batch
 */
public final class BatchResult<T> {

    private final List<T> successfulItems;
    private final List<T> failedItems;
    private final List<String> failureMessages;

    public BatchResult() {
        this.successfulItems = new ArrayList<>();
        this.failedItems = new ArrayList<>();
        this.failureMessages = new ArrayList<>();
    }

    public void addSuccess(T item) {
        successfulItems.add(item);
    }

    public void addFailure(T item, String message) {
        failedItems.add(item);
        failureMessages.add(message != null ? message : "Unknown error");
    }

    public List<T> getSuccessfulItems() {
        return Collections.unmodifiableList(successfulItems);
    }

    public List<T> getFailedItems() {
        return Collections.unmodifiableList(failedItems);
    }

    public List<String> getFailureMessages() {
        return Collections.unmodifiableList(failureMessages);
    }

    public int getTotalCount() {
        return successfulItems.size() + failedItems.size();
    }

    public int getSuccessCount() {
        return successfulItems.size();
    }

    public int getFailureCount() {
        return failedItems.size();
    }

    public boolean isAllSuccessful() {
        return failedItems.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("BatchResult[total=%d, success=%d, failed=%d]",
                getTotalCount(), getSuccessCount(), getFailureCount());
    }
}
