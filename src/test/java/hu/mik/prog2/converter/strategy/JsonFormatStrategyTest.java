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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonFormatStrategyTest {

    private JsonFormatStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new JsonFormatStrategy();
    }

    @Test
    void testParseBasicJson() throws ConverterException {
        String json = """
                [
                  {
                    "name": "Alice",
                    "role": "Developer",
                    "experience": "5"
                  },
                  {
                    "name": "Bob",
                    "role": "Architect",
                    "experience": "12"
                  }
                ]
                """;
        ByteArrayInputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        Document doc = strategy.parse(in);

        assertNotNull(doc);
        assertEquals(2, doc.getRecordCount());
        assertEquals("Alice", doc.getRecords().get(0).getValue("name"));
        assertEquals("Architect", doc.getRecords().get(1).getValue("role"));
    }

    @Test
    void testWriteJson() throws ConverterException {
        DocumentHeader header = new DocumentHeader(List.of("code", "status"));
        Map<String, String> m1 = new LinkedHashMap<>();
        m1.put("code", "200");
        m1.put("status", "OK \"Success\"");
        Document doc = new Document(header, List.of(new DocumentRecord(m1)));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        strategy.write(doc, out);

        String result = out.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("\"code\": \"200\""));
        assertTrue(result.contains("\\\"Success\\\""));
    }

    @Test
    void testMalformedJsonThrowsException() {
        String malformed = "[{\"unclosed\": \"value]";
        ByteArrayInputStream in = new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8));
        assertThrows(InvalidFileStructureException.class, () -> strategy.parse(in));
    }
}
