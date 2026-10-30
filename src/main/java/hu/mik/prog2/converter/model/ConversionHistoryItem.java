package hu.mik.prog2.converter.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing a persistent record of a file conversion attempt.
 * Implements Comparable for natural ordering (newest first), and provides
 * static Comparators for alternative sorting criteria.
 */
public final class ConversionHistoryItem implements Comparable<ConversionHistoryItem> {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String id;
    private final LocalDateTime timestamp;
    private final String sourcePath;
    private final String targetPath;
    private final FormatType sourceFormat;
    private final FormatType targetFormat;
    private final long fileSizeBytes;
    private final int recordCount;
    private final long durationMillis;
    private final boolean success;
    private final String errorMessage;

    /**
     * Standard constructor for recording a new conversion.
     */
    public ConversionHistoryItem(String id,
                                 LocalDateTime timestamp,
                                 String sourcePath,
                                 String targetPath,
                                 FormatType sourceFormat,
                                 FormatType targetFormat,
                                 long fileSizeBytes,
                                 int recordCount,
                                 long durationMillis,
                                 boolean success,
                                 String errorMessage) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
        this.timestamp = (timestamp == null) ? LocalDateTime.now() : timestamp;
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath cannot be null");
        this.targetPath = Objects.requireNonNull(targetPath, "targetPath cannot be null");
        this.sourceFormat = Objects.requireNonNull(sourceFormat, "sourceFormat cannot be null");
        this.targetFormat = Objects.requireNonNull(targetFormat, "targetFormat cannot be null");
        this.fileSizeBytes = Math.max(0, fileSizeBytes);
        this.recordCount = Math.max(0, recordCount);
        this.durationMillis = Math.max(0, durationMillis);
        this.success = success;
        this.errorMessage = (errorMessage == null) ? "" : errorMessage;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public FormatType getSourceFormat() {
        return sourceFormat;
    }

    public FormatType getTargetFormat() {
        return targetFormat;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Natural order: newest first (descending timestamp).
     */
    @Override
    public int compareTo(ConversionHistoryItem other) {
        if (other == null) return 1;
        return other.timestamp.compareTo(this.timestamp);
    }

    // Static Comparators demonstrating flexible sorting criteria
    public static final Comparator<ConversionHistoryItem> BY_DURATION =
            Comparator.comparingLong(ConversionHistoryItem::getDurationMillis);

    public static final Comparator<ConversionHistoryItem> BY_FILE_SIZE =
            Comparator.comparingLong(ConversionHistoryItem::getFileSizeBytes);

    public static final Comparator<ConversionHistoryItem> BY_SOURCE_NAME =
            Comparator.comparing(item -> item.getSourcePath().toLowerCase());

    public static final Comparator<ConversionHistoryItem> BY_STATUS =
            Comparator.comparing(ConversionHistoryItem::isSuccess).reversed();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConversionHistoryItem that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s (%s->%s) | %d records | %d ms | %s",
                timestamp.format(FORMATTER),
                sourcePath,
                targetPath,
                sourceFormat,
                targetFormat,
                recordCount,
                durationMillis,
                success ? "SUCCESS" : "FAILED: " + errorMessage);
    }
}
