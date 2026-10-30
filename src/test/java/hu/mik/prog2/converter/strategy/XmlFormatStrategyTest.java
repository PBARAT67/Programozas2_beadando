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

class XmlFormatStrategyTest {

    private XmlFormatStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new XmlFormatStrategy();
    }

    @Test
    void testParseBasicXml() throws ConverterException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <document>
                    <record>
                        <id>1</id>
                        <title>Introduction to Java</title>
                    </record>
                    <record>
                        <id>2</id>
                        <title>Advanced OOP</title>
                    </record>
                </document>
                """;
        ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        Document doc = strategy.parse(in);

        assertNotNull(doc);
        assertEquals(2, doc.getRecordCount());
        assertEquals(List.of("id", "title"), doc.getHeader().getColumns());
        assertEquals("Introduction to Java", doc.getRecords().get(0).getValue("title"));
        assertEquals("2", doc.getRecords().get(1).getValue("id"));
    }

    @Test
    void testWriteXml() throws ConverterException {
        DocumentHeader header = new DocumentHeader(List.of("user", "score"));
        Map<String, String> m1 = new LinkedHashMap<>();
        m1.put("user", "player1");
        m1.put("score", "950");
        Document doc = new Document(header, List.of(new DocumentRecord(m1)));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        strategy.write(doc, out);

        String result = out.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("<document>"));
        assertTrue(result.contains("<record>"));
        assertTrue(result.contains("<user>player1</user>"));
        assertTrue(result.contains("<score>950</score>"));
    }

    @Test
    void testInvalidXmlThrowsException() {
        String invalidXml = "<document><record><unclosed></record></document>";
        ByteArrayInputStream in = new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8));
        assertThrows(InvalidFileStructureException.class, () -> strategy.parse(in));
    }
}
