package hu.mik.prog2.converter.strategy;

import hu.mik.prog2.converter.exception.ConverterException;
import hu.mik.prog2.converter.exception.InvalidFileStructureException;
import hu.mik.prog2.converter.model.Document;
import hu.mik.prog2.converter.model.DocumentHeader;
import hu.mik.prog2.converter.model.DocumentRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvFormatStrategyTest {

    private CsvFormatStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new CsvFormatStrategy();
    }

    @Test
    void testParseBasicCsv() throws ConverterException {
        String csv = "id,name,city\n1,Alice,Pecs\n2,Bob,Budapest";
        ByteArrayInputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        Document doc = strategy.parse(in);

        assertNotNull(doc);
        assertEquals(3, doc.getColumnCount());
        assertEquals(List.of("id", "name", "city"), doc.getHeader().getColumns());
        assertEquals(2, doc.getRecordCount());
        assertEquals("Alice", doc.getRecords().get(0).getValue("name"));
        assertEquals("Budapest", doc.getRecords().get(1).getValue("city"));
    }

    @Test
    void testParseQuotedAndEscapedCsv() throws ConverterException {
        String csv = "id,description,price\n101,\"Item with, comma\",45.50\n102,\"He said: \"\"Hello\"\"\",99.00";
        ByteArrayInputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        Document doc = strategy.parse(in);

        assertEquals(2, doc.getRecordCount());
        assertEquals("Item with, comma", doc.getRecords().get(0).getValue("description"));
        assertEquals("He said: \"Hello\"", doc.getRecords().get(1).getValue("description"));
    }

    @Test
    void testWriteCsv() throws ConverterException {
        DocumentHeader header = new DocumentHeader(List.of("id", "title"));
        DocumentRecord r1 = new DocumentRecord(Map.of("id", "1", "title", "Book, Vol. 1"));
        DocumentRecord r2 = new DocumentRecord(Map.of("id", "2", "title", "Standard Title"));
        Document doc = new Document(header, List.of(r1, r2));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        strategy.write(doc, out);

        String result = out.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("id,title"));
        assertTrue(result.contains("\"Book, Vol. 1\""));
        assertTrue(result.contains("Standard Title"));
    }

    @Test
    void testEmptyCsvThrowsException() {
        ByteArrayInputStream in = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        assertThrows(InvalidFileStructureException.class, () -> strategy.parse(in));
    }
}
