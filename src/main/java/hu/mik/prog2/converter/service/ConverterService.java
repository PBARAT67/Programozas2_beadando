package hu.mik.prog2.converter.service;

import hu.mik.prog2.converter.exception.ConverterException;
import hu.mik.prog2.converter.exception.InvalidFileStructureException;
import hu.mik.prog2.converter.exception.UnsupportedFormatException;
import hu.mik.prog2.converter.model.BatchResult;
import hu.mik.prog2.converter.model.ConversionHistoryItem;
import hu.mik.prog2.converter.model.ConversionJob;
import hu.mik.prog2.converter.model.Document;
import hu.mik.prog2.converter.model.FormatType;
import hu.mik.prog2.converter.strategy.FormatStrategy;
import hu.mik.prog2.converter.strategy.FormatStrategyFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Core engine orchestrating file conversions, applying Strategy pattern polymorphism,
 * measuring performance, and logging history.
 */
public class ConverterService {

    private final FormatStrategyFactory strategyFactory;
    private final HistoryService historyService;

    public ConverterService(FormatStrategyFactory strategyFactory, HistoryService historyService) {
        this.strategyFactory = Objects.requireNonNull(strategyFactory, "strategyFactory cannot be null");
        this.historyService = Objects.requireNonNull(historyService, "historyService cannot be null");
    }

    /**
     * Executes a single conversion job and logs the result.
     *
     * @param job the conversion job specification
     * @return the recorded history item reflecting the result
     */
    public ConversionHistoryItem convert(ConversionJob job) {
        Objects.requireNonNull(job, "job cannot be null");
        Path src = job.getSourcePath();
        Path tgt = job.getTargetPath();

        long startTime = System.currentTimeMillis();
        long fileSize = 0;
        int recordCount = 0;
        boolean success = false;
        String errorMessage = null;

        try {
            if (!Files.exists(src) || !Files.isRegularFile(src)) {
                throw new InvalidFileStructureException("Source file does not exist or is not a regular file: " + src);
            }

            fileSize = Files.size(src);

            // Ensure target directory exists
            if (tgt.getParent() != null && !Files.exists(tgt.getParent())) {
                Files.createDirectories(tgt.getParent());
            }

            FormatStrategy sourceStrategy = strategyFactory.getStrategy(job.getSourceFormat());
            FormatStrategy targetStrategy = strategyFactory.getStrategy(job.getTargetFormat());

            Document document;
            // Strict try-with-resources for reading
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(src.toFile()))) {
                document = sourceStrategy.parse(in);
            }

            recordCount = document.getRecordCount();

            // Strict try-with-resources for writing
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tgt.toFile()))) {
                targetStrategy.write(document, out);
            }

            success = true;
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }

        long duration = System.currentTimeMillis() - startTime;

        ConversionHistoryItem historyItem = new ConversionHistoryItem(
                null,
                LocalDateTime.now(),
                src.toString(),
                tgt.toString(),
                job.getSourceFormat(),
                job.getTargetFormat(),
                fileSize,
                recordCount,
                duration,
                success,
                errorMessage
        );

        historyService.record(historyItem);
        return historyItem;
    }

    /**
     * Convenience method to convert a file where formats are deduced from file extensions.
     */
    public ConversionHistoryItem convert(Path sourcePath, Path targetPath) throws UnsupportedFormatException {
        FormatType srcType = FormatType.fromFileNameOrExtension(sourcePath.getFileName().toString());
        FormatType tgtType = FormatType.fromFileNameOrExtension(targetPath.getFileName().toString());
        return convert(new ConversionJob(sourcePath, targetPath, srcType, tgtType));
    }

    /**
     * Executes batch conversions across a collection of jobs.
     * Demonstrates use of custom generic BatchResult<T>.
     */
    public BatchResult<ConversionHistoryItem> convertBatch(List<ConversionJob> jobs) {
        BatchResult<ConversionHistoryItem> batchResult = new BatchResult<>();
        if (jobs == null) return batchResult;

        for (ConversionJob job : jobs) {
            ConversionHistoryItem result = convert(job);
            if (result.isSuccess()) {
                batchResult.addSuccess(result);
            } else {
                batchResult.addFailure(result, result.getErrorMessage());
            }
        }
        return batchResult;
    }

    /**
     * Converts all supported files in a directory to a specified target format.
     */
    public BatchResult<ConversionHistoryItem> convertDirectory(Path sourceDir, Path targetDir, FormatType targetFormat) throws ConverterException {
        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            throw new InvalidFileStructureException("Source path is not a valid directory: " + sourceDir);
        }

        List<ConversionJob> jobs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    try {
                        FormatType srcType = FormatType.fromFileNameOrExtension(file.getFileName().toString());
                        String baseName = getBaseName(file.getFileName().toString());
                        Path targetFile = targetDir.resolve(baseName + "." + targetFormat.getExtension());
                        jobs.add(new ConversionJob(file, targetFile, srcType, targetFormat));
                    } catch (UnsupportedFormatException ignored) {
                        // Skip files with unsupported extensions
                    }
                }
            }
        } catch (IOException e) {
            throw new ConverterException("Failed to scan directory: " + sourceDir, e);
        }

        return convertBatch(jobs);
    }

    private String getBaseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot > 0) ? fileName.substring(0, dot) : fileName;
    }

    public HistoryService getHistoryService() {
        return historyService;
    }

    public FormatStrategyFactory getStrategyFactory() {
        return strategyFactory;
    }
}
