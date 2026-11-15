package hu.mik.prog2.converter.ui;

import hu.mik.prog2.converter.exception.ConverterException;
import hu.mik.prog2.converter.model.BatchResult;
import hu.mik.prog2.converter.model.ConversionHistoryItem;
import hu.mik.prog2.converter.model.FormatType;
import hu.mik.prog2.converter.service.ConverterService;
import hu.mik.prog2.converter.service.HistoryService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive text console user interface for the converter application.
 */
public class ConsoleMenu {

    private final ConverterService converterService;
    private final HistoryService historyService;
    private final Scanner scanner;

    public ConsoleMenu(ConverterService converterService) {
        this.converterService = converterService;
        this.historyService = converterService.getHistoryService();
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        printBanner();
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = prompt("Select an option [1-7]: ");
            switch (choice.trim()) {
                case "1" -> handleSingleFileConversion();
                case "2" -> handleDirectoryBatchConversion();
                case "3" -> handleViewHistory();
                case "4" -> handleReconvertFromHistory();
                case "5" -> handleShowStatistics();
                case "6" -> handleClearHistory();
                case "7" -> {
                    System.out.println("\nExiting DataMorph Converter. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option. Please enter a number between 1 and 7.");
            }
        }
    }

    private void printBanner() {
        System.out.println("============================================================");
        System.out.println("     DataMorph File Converter - PTE MIK Prog 2 Project     ");
        System.out.println("     Supports: CSV, JSON, XML (Strategy Pattern & OOP)      ");
        System.out.println("============================================================");
    }

    private void printMainMenu() {
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("1. Convert a single file");
        System.out.println("2. Convert all files in a folder (Batch)");
        System.out.println("3. View conversion history (with sorting)");
        System.out.println("4. Re-convert a file from history");
        System.out.println("5. View conversion statistics");
        System.out.println("6. Clear conversion history");
        System.out.println("7. Exit");
    }

    private void handleSingleFileConversion() {
        System.out.println("\n-- Single File Conversion --");
        String srcStr = prompt("Source file path (e.g. sample.csv): ");
        if (srcStr.isBlank()) {
            System.out.println("Source path cannot be empty.");
            return;
        }

        String tgtStr = prompt("Target file path (e.g. output.json): ");
        if (tgtStr.isBlank()) {
            System.out.println("Target path cannot be empty.");
            return;
        }

        Path src = Paths.get(srcStr);
        Path tgt = Paths.get(tgtStr);

        try {
            System.out.println("Converting: " + src + " -> " + tgt + " ...");
            ConversionHistoryItem result = converterService.convert(src, tgt);
            if (result.isSuccess()) {
                System.out.printf("SUCCESS! Converted %d records in %d ms (%d bytes).%n",
                        result.getRecordCount(), result.getDurationMillis(), result.getFileSizeBytes());
            } else {
                System.out.println("CONVERSION FAILED: " + result.getErrorMessage());
            }
        } catch (ConverterException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handleDirectoryBatchConversion() {
        System.out.println("\n-- Directory Batch Conversion --");
        String srcDirStr = prompt("Source folder path: ");
        String tgtDirStr = prompt("Target folder path: ");
        String formatStr = prompt("Target format (csv / json / xml): ");

        try {
            FormatType targetFormat = FormatType.fromFileNameOrExtension(formatStr);
            Path srcDir = Paths.get(srcDirStr);
            Path tgtDir = Paths.get(tgtDirStr);

            System.out.println("Processing batch in: " + srcDir + " -> " + tgtDir + " [" + targetFormat + "] ...");
            BatchResult<ConversionHistoryItem> batchResult = converterService.convertDirectory(srcDir, tgtDir, targetFormat);

            System.out.printf("Batch completed: Total=%d, Success=%d, Failed=%d%n",
                    batchResult.getTotalCount(), batchResult.getSuccessCount(), batchResult.getFailureCount());

            for (ConversionHistoryItem item : batchResult.getSuccessfulItems()) {
                System.out.println("  [OK] " + item.getSourcePath() + " -> " + item.getTargetPath());
            }
            for (int i = 0; i < batchResult.getFailedItems().size(); i++) {
                ConversionHistoryItem failed = batchResult.getFailedItems().get(i);
                String msg = batchResult.getFailureMessages().get(i);
                System.out.println("  [FAILED] " + failed.getSourcePath() + ": " + msg);
            }
        } catch (ConverterException e) {
            System.out.println("Batch error: " + e.getMessage());
        }
    }

    private void handleViewHistory() {
        System.out.println("\n-- Conversion History Sorting --");
        System.out.println("1. Natural Order (Newest first - Comparable)");
        System.out.println("2. Sort by Execution Duration (Shortest first)");
        System.out.println("3. Sort by File Size (Smallest first)");
        System.out.println("4. Sort by Source File Name (Alphabetical)");
        System.out.println("5. Sort by Status (Successful first)");
        String sortChoice = prompt("Select sorting order [1-5]: ");

        List<ConversionHistoryItem> items;
        switch (sortChoice.trim()) {
            case "2" -> items = historyService.getHistorySorted(ConversionHistoryItem.BY_DURATION);
            case "3" -> items = historyService.getHistorySorted(ConversionHistoryItem.BY_FILE_SIZE);
            case "4" -> items = historyService.getHistorySorted(ConversionHistoryItem.BY_SOURCE_NAME);
            case "5" -> items = historyService.getHistorySorted(ConversionHistoryItem.BY_STATUS);
            default -> items = historyService.getHistorySortedNatural();
        }

        if (items.isEmpty()) {
            System.out.println("No conversion history recorded yet.");
            return;
        }

        System.out.printf("%n%-20s | %-12s | %-10s | %-10s | %-8s | %-6s | %s%n",
                "Timestamp", "Format", "Records", "Size", "Duration", "Status", "Source -> Target");
        System.out.println("---------------------------------------------------------------------------------------------------------");
        for (ConversionHistoryItem item : items) {
            String fmt = item.getSourceFormat() + "->" + item.getTargetFormat();
            String status = item.isSuccess() ? "OK" : "FAILED";
            System.out.printf("%-20s | %-12s | %-10d | %-10s | %-6d ms | %-6s | %s -> %s%n",
                    item.getTimestamp().format(ConversionHistoryItem.FORMATTER),
                    fmt,
                    item.getRecordCount(),
                    item.getFileSizeBytes() + " B",
                    item.getDurationMillis(),
                    status,
                    Paths.get(item.getSourcePath()).getFileName(),
                    Paths.get(item.getTargetPath()).getFileName());
        }

        String reconvertPrompt = prompt("\nWould you like to re-convert one of these files? (y/N): ");
        if (reconvertPrompt.equalsIgnoreCase("y")) {
            handleReconvertFromHistory();
        }
    }

    private void handleReconvertFromHistory() {
        System.out.println("\n-- Re-convert a File from History --");
        List<ConversionHistoryItem> items = historyService.getHistorySortedNatural();
        if (items.isEmpty()) {
            System.out.println("No conversion history recorded yet.");
            return;
        }

        System.out.println("Available past conversions (newest first):");
        for (int i = 0; i < items.size(); i++) {
            ConversionHistoryItem it = items.get(i);
            System.out.printf("[%d] %s -> %s (%s->%s) | %s%n",
                    i + 1,
                    Paths.get(it.getSourcePath()).getFileName(),
                    Paths.get(it.getTargetPath()).getFileName(),
                    it.getSourceFormat(),
                    it.getTargetFormat(),
                    it.isSuccess() ? "OK" : "FAILED");
        }

        String selStr = prompt("Select item number to re-convert [1-" + items.size() + "] (or 0 to cancel): ");
        int sel;
        try {
            sel = Integer.parseInt(selStr.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return;
        }

        if (sel <= 0 || sel > items.size()) {
            System.out.println("Operation canceled.");
            return;
        }

        ConversionHistoryItem selected = items.get(sel - 1);
        System.out.println("Selected conversion: " + selected.getSourcePath() + " -> " + selected.getTargetPath());
        System.out.println("1. Use original source file: " + selected.getSourcePath());
        System.out.println("2. Use previous output file: " + selected.getTargetPath());
        String fileChoice = prompt("Choose file to convert [1 or 2]: ");

        String chosenPathStr = fileChoice.trim().equals("2") ? selected.getTargetPath() : selected.getSourcePath();
        Path chosenPath = Paths.get(chosenPathStr);

        if (!Files.exists(chosenPath)) {
            System.out.println("File does not exist on disk: " + chosenPath);
            return;
        }

        String tgtStr = prompt("New target file path (e.g. reconverted_output.xml): ");
        if (tgtStr.isBlank()) {
            System.out.println("Target path cannot be empty.");
            return;
        }

        Path tgtPath = Paths.get(tgtStr);
        try {
            System.out.println("Re-converting: " + chosenPath + " -> " + tgtPath + " ...");
            ConversionHistoryItem result = converterService.convert(chosenPath, tgtPath);
            if (result.isSuccess()) {
                System.out.printf("SUCCESS! Re-converted %d records in %d ms (%d bytes).%n",
                        result.getRecordCount(), result.getDurationMillis(), result.getFileSizeBytes());
            } else {
                System.out.println("RE-CONVERSION FAILED: " + result.getErrorMessage());
            }
        } catch (ConverterException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handleShowStatistics() {
        System.out.println("\n-- Conversion Statistics --");
        int total = historyService.getCount();
        long success = historyService.getSuccessfulCount();
        long failed = historyService.getFailedCount();
        long bytes = historyService.getTotalBytesProcessed();

        System.out.println("Total conversions:  " + total);
        System.out.println("Successful:         " + success);
        System.out.println("Failed:             " + failed);
        System.out.printf("Success rate:       %.1f%%%n", total > 0 ? (success * 100.0 / total) : 0.0);
        System.out.printf("Total data processed: %.2f KB (%d bytes)%n", bytes / 1024.0, bytes);
    }

    private void handleClearHistory() {
        String confirm = prompt("Are you sure you want to clear all history? (y/N): ");
        if (confirm.equalsIgnoreCase("y")) {
            try {
                historyService.clearHistory();
                System.out.println("History successfully cleared.");
            } catch (ConverterException e) {
                System.out.println("Failed to clear history: " + e.getMessage());
            }
        } else {
            System.out.println("Operation canceled.");
        }
    }

    private String prompt(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }
}
