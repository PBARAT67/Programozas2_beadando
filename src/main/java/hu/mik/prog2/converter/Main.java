package hu.mik.prog2.converter;

import hu.mik.prog2.converter.model.ConversionHistoryItem;
import hu.mik.prog2.converter.model.FormatType;
import hu.mik.prog2.converter.persistence.HistoryRepository;
import hu.mik.prog2.converter.persistence.JsonHistoryRepository;
import hu.mik.prog2.converter.service.ConverterService;
import hu.mik.prog2.converter.service.HistoryService;
import hu.mik.prog2.converter.strategy.FormatStrategyFactory;
import hu.mik.prog2.converter.ui.ConsoleMenu;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main application entry point for the DataMorph File Converter system.
 * Initializes core services and launches interactive console menu or handles CLI arguments.
 */
public class Main {

    public static void main(String[] args) {
        Path historyPath = Paths.get("history.json");
        HistoryRepository repository = new JsonHistoryRepository(historyPath);
        HistoryService historyService = new HistoryService(repository);
        FormatStrategyFactory strategyFactory = new FormatStrategyFactory();
        ConverterService converterService = new ConverterService(strategyFactory, historyService);

        if (args.length == 0) {
            // Interactive Console Mode
            ConsoleMenu menu = new ConsoleMenu(converterService);
            menu.run();
        } else {
            // CLI Non-Interactive Mode
            handleCliArguments(args, converterService, historyService);
        }
    }

    private static void handleCliArguments(String[] args, ConverterService converterService, HistoryService historyService) {
        String first = args[0];

        if (first.equalsIgnoreCase("--help") || first.equalsIgnoreCase("-h")) {
            printCliHelp();
            return;
        }

        if (first.equalsIgnoreCase("--history")) {
            List<ConversionHistoryItem> items = historyService.getHistorySortedNatural();
            System.out.println("=== Conversion History ===");
            for (ConversionHistoryItem item : items) {
                System.out.println(item);
            }
            return;
        }

        if (first.equalsIgnoreCase("--batch") && args.length >= 4) {
            Path srcDir = Paths.get(args[1]);
            Path tgtDir = Paths.get(args[2]);
            try {
                FormatType targetFmt = FormatType.fromFileNameOrExtension(args[3]);
                System.out.printf("Running batch conversion: %s -> %s [%s]%n", srcDir, tgtDir, targetFmt);
                var result = converterService.convertDirectory(srcDir, tgtDir, targetFmt);
                System.out.printf("Batch finished: %d succeeded, %d failed.%n",
                        result.getSuccessCount(), result.getFailureCount());
            } catch (Exception e) {
                System.err.println("Batch error: " + e.getMessage());
                System.exit(1);
            }
            return;
        }

        if (args.length >= 2) {
            Path src = Paths.get(args[0]);
            Path tgt = Paths.get(args[1]);
            try {
                ConversionHistoryItem result = converterService.convert(src, tgt);
                if (result.isSuccess()) {
                    System.out.printf("Successfully converted '%s' -> '%s' (%d records, %d ms)%n",
                            src, tgt, result.getRecordCount(), result.getDurationMillis());
                } else {
                    System.err.println("Conversion failed: " + result.getErrorMessage());
                    System.exit(1);
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
            }
            return;
        }

        System.err.println("Invalid arguments. Use --help to view available usage options.");
        System.exit(1);
    }

    private static void printCliHelp() {
        System.out.println("DataMorph File Converter - CLI Usage:");
        System.out.println("  Interactive mode:     java -jar datamorph-converter-1.0.0.jar");
        System.out.println("  Single file:          java -jar datamorph-converter-1.0.0.jar <sourceFile> <targetFile>");
        System.out.println("  Batch directory:      java -jar datamorph-converter-1.0.0.jar --batch <srcDir> <tgtDir> <format>");
        System.out.println("  View history:         java -jar datamorph-converter-1.0.0.jar --history");
        System.out.println("  Help:                 java -jar datamorph-converter-1.0.0.jar --help");
    }
}
