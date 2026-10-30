package hu.mik.prog2.converter.service;

import hu.mik.prog2.converter.exception.ConverterException;
import hu.mik.prog2.converter.model.BatchResult;
import hu.mik.prog2.converter.model.ConversionHistoryItem;
import hu.mik.prog2.converter.model.FormatType;
import hu.mik.prog2.converter.persistence.JsonHistoryRepository;
import hu.mik.prog2.converter.strategy.FormatStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConverterServiceTest {

    private ConverterService converterService;
    private HistoryService historyService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        Path historyPath = tempDir.resolve("test_history.json");
        JsonHistoryRepository repository = new JsonHistoryRepository(historyPath);
        historyService = new HistoryService(repository);
        FormatStrategyFactory factory = new FormatStrategyFactory();
        converterService = new ConverterService(factory, historyService);
    }

    @Test
    void testCsvToJsonRoundtrip() throws IOException, ConverterException {
        Path csvFile = tempDir.resolve("data.csv");
        Files.writeString(csvFile, "id,name,department\n101,John,Engineering\n102,Jane,Design\n");

        Path jsonFile = tempDir.resolve("output.json");
        ConversionHistoryItem result = converterService.convert(csvFile, jsonFile);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getRecordCount());
        assertTrue(Files.exists(jsonFile));
        String jsonContent = Files.readString(jsonFile);
        assertTrue(jsonContent.contains("\"name\": \"John\""));
        assertTrue(jsonContent.contains("\"department\": \"Design\""));

        // Now convert JSON to XML
        Path xmlFile = tempDir.resolve("output.xml");
        ConversionHistoryItem xmlResult = converterService.convert(jsonFile, xmlFile);
        assertTrue(xmlResult.isSuccess());
        assertEquals(2, xmlResult.getRecordCount());
        assertTrue(Files.exists(xmlFile));
        String xmlContent = Files.readString(xmlFile);
        assertTrue(xmlContent.contains("<name>Jane</name>"));

        // Verify history tracking
        assertEquals(2, historyService.getCount());
        assertEquals(2, historyService.getSuccessfulCount());
    }

    @Test
    void testConversionFailsOnMissingSourceFile() throws Exception {
        Path missing = tempDir.resolve("non_existent.csv");
        Path target = tempDir.resolve("out.json");

        ConversionHistoryItem result = converterService.convert(missing, target);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertEquals(1, historyService.getFailedCount());
    }

    @Test
    void testConvertDirectoryBatch() throws IOException, ConverterException {
        Path batchSrcDir = tempDir.resolve("input_dir");
        Path batchTgtDir = tempDir.resolve("output_dir");
        Files.createDirectories(batchSrcDir);

        Files.writeString(batchSrcDir.resolve("file1.csv"), "col1,col2\nA,B\nC,D\n");
        Files.writeString(batchSrcDir.resolve("file2.csv"), "id,val\n1,100\n2,200\n");

        BatchResult<ConversionHistoryItem> batchResult =
                converterService.convertDirectory(batchSrcDir, batchTgtDir, FormatType.JSON);

        assertEquals(2, batchResult.getTotalCount());
        assertEquals(2, batchResult.getSuccessCount());
        assertTrue(batchResult.isAllSuccessful());
        assertTrue(Files.exists(batchTgtDir.resolve("file1.json")));
        assertTrue(Files.exists(batchTgtDir.resolve("file2.json")));
    }
}
