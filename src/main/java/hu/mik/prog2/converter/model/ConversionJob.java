package hu.mik.prog2.converter.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object representing a single file conversion job specification.
 */
public final class ConversionJob {

    private final Path sourcePath;
    private final Path targetPath;
    private final FormatType sourceFormat;
    private final FormatType targetFormat;
    private final LocalDateTime requestedAt;

    public ConversionJob(Path sourcePath, Path targetPath, FormatType sourceFormat, FormatType targetFormat) {
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath cannot be null");
        this.targetPath = Objects.requireNonNull(targetPath, "targetPath cannot be null");
        this.sourceFormat = Objects.requireNonNull(sourceFormat, "sourceFormat cannot be null");
        this.targetFormat = Objects.requireNonNull(targetFormat, "targetFormat cannot be null");
        this.requestedAt = LocalDateTime.now();
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public Path getTargetPath() {
        return targetPath;
    }

    public FormatType getSourceFormat() {
        return sourceFormat;
    }

    public FormatType getTargetFormat() {
        return targetFormat;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    @Override
    public String toString() {
        return "ConversionJob{" +
                "source=" + sourcePath.getFileName() + " (" + sourceFormat + ")" +
                " -> target=" + targetPath.getFileName() + " (" + targetFormat + ")" +
                '}';
    }
}
