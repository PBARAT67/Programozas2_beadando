package hu.mik.prog2.converter.service;

import hu.mik.prog2.converter.exception.ConverterException;
import hu.mik.prog2.converter.model.ConversionHistoryItem;
import hu.mik.prog2.converter.model.FormatType;
import hu.mik.prog2.converter.persistence.JsonHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryServiceTest {

    @TempDir
    Path tempDir;

    private Path historyFile;
    private HistoryService historyService;

    @BeforeEach
    void setUp() {
        historyFile = tempDir.resolve("history.json");
        historyService = new HistoryService(new JsonHistoryRepository(historyFile));
    }

    @Test
    void testRecordAndSortNatural() {
        LocalDateTime now = LocalDateTime.now();
        ConversionHistoryItem item1 = new ConversionHistoryItem(
                "id1", now.minusHours(2), "fileA.csv", "fileA.json",
                FormatType.CSV, FormatType.JSON, 500, 10, 50, true, null);
        ConversionHistoryItem item2 = new ConversionHistoryItem(
                "id2", now.minusHours(1), "fileB.csv", "fileB.xml",
                FormatType.CSV, FormatType.XML, 1500, 30, 120, true, null);
        ConversionHistoryItem item3 = new ConversionHistoryItem(
                "id3", now, "fileC.json", "fileC.csv",
                FormatType.JSON, FormatType.CSV, 200, 5, 20, false, "syntax error");

        historyService.record(item1);
        historyService.record(item2);
        historyService.record(item3);

        assertEquals(3, historyService.getCount());
        assertEquals(2, historyService.getSuccessfulCount());
        assertEquals(1, historyService.getFailedCount());

        // Test Comparable natural sort (newest first)
        List<ConversionHistoryItem> natural = historyService.getHistorySortedNatural();
        assertEquals("id3", natural.get(0).getId());
        assertEquals("id2", natural.get(1).getId());
        assertEquals("id1", natural.get(2).getId());

        // Test Comparator by duration (ascending: 20ms, 50ms, 120ms)
        List<ConversionHistoryItem> byDuration = historyService.getHistorySorted(ConversionHistoryItem.BY_DURATION);
        assertEquals(20, byDuration.get(0).getDurationMillis());
        assertEquals(50, byDuration.get(1).getDurationMillis());
        assertEquals(120, byDuration.get(2).getDurationMillis());

        // Test Comparator by file size (ascending: 200, 500, 1500)
        List<ConversionHistoryItem> bySize = historyService.getHistorySorted(ConversionHistoryItem.BY_FILE_SIZE);
        assertEquals(200, bySize.get(0).getFileSizeBytes());
        assertEquals(500, bySize.get(1).getFileSizeBytes());
        assertEquals(1500, bySize.get(2).getFileSizeBytes());
    }

    @Test
    void testPersistenceReload() {
        ConversionHistoryItem item = new ConversionHistoryItem(
                "persisted-id", LocalDateTime.now(), "in.csv", "out.json",
                FormatType.CSV, FormatType.JSON, 1024, 25, 42, true, null);
        historyService.record(item);

        // Create new service pointing to the same file (simulating application restart)
        HistoryService reloadedService = new HistoryService(new JsonHistoryRepository(historyFile));
        assertEquals(1, reloadedService.getCount());
        ConversionHistoryItem loaded = reloadedService.getHistorySortedNatural().get(0);
        assertEquals("persisted-id", loaded.getId());
        assertEquals("in.csv", loaded.getSourcePath());
        assertEquals("out.json", loaded.getTargetPath());
        assertTrue(loaded.isSuccess());
    }

    @Test
    void testClearHistory() throws ConverterException {
        historyService.record(new ConversionHistoryItem(
                "id1", LocalDateTime.now(), "a.csv", "a.json",
                FormatType.CSV, FormatType.JSON, 100, 5, 10, true, null));
        assertEquals(1, historyService.getCount());

        historyService.clearHistory();
        assertEquals(0, historyService.getCount());

        HistoryService fresh = new HistoryService(new JsonHistoryRepository(historyFile));
        assertEquals(0, fresh.getCount());
    }
}
