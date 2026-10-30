package hu.mik.prog2.converter.strategy;

import hu.mik.prog2.converter.exception.ConverterException;
import hu.mik.prog2.converter.exception.InvalidFileStructureException;
import hu.mik.prog2.converter.model.Document;
import hu.mik.prog2.converter.model.DocumentHeader;
import hu.mik.prog2.converter.model.DocumentRecord;
import hu.mik.prog2.converter.model.FormatType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Concrete strategy for parsing and serializing JSON (JavaScript Object Notation).
 * Parses an array of JSON objects into a Document and writes formatted JSON.
 */
public class JsonFormatStrategy extends AbstractFormatStrategy {

    public JsonFormatStrategy() {
        super(FormatType.JSON);
    }

    @Override
    public Document parse(InputStream inputStream) throws ConverterException {
        String jsonText;
        try (BufferedReader reader = createReader(inputStream)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            jsonText = sb.toString().trim();
        } catch (IOException e) {
            throw new ConverterException("Failed to read JSON stream: " + e.getMessage(), e);
        }

        if (jsonText.isEmpty()) {
            throw new InvalidFileStructureException("JSON content is empty.");
        }

        SimpleJsonParser parser = new SimpleJsonParser(jsonText);
        List<Map<String, String>> objectList = parser.parseArrayOfObjects();

        if (objectList.isEmpty()) {
            throw new InvalidFileStructureException("JSON array contains no records.");
        }

        // Collect distinct headers in order of appearance
        Set<String> headerSet = new LinkedHashSet<>();
        for (Map<String, String> obj : objectList) {
            headerSet.addAll(obj.keySet());
        }

        if (headerSet.isEmpty()) {
            throw new InvalidFileStructureException("No valid fields/columns found in JSON records.");
        }

        DocumentHeader header = new DocumentHeader(new ArrayList<>(headerSet));
        List<DocumentRecord> records = new ArrayList<>(objectList.size());
        for (Map<String, String> obj : objectList) {
            records.add(new DocumentRecord(obj));
        }

        return new Document(header, records);
    }

    @Override
    public void write(Document document, OutputStream outputStream) throws ConverterException {
        validateDocumentForWriting(document);
        BufferedWriter writer = createWriter(outputStream);

        try {
            DocumentHeader header = document.getHeader();
            List<DocumentRecord> records = document.getRecords();

            writer.write("[\n");
            for (int r = 0; r < records.size(); r++) {
                DocumentRecord record = records.get(r);
                writer.write("  {\n");
                for (int c = 0; c < header.size(); c++) {
                    String col = header.getColumn(c);
                    String val = record.getValueOrDefault(col, "");
                    writer.write("    \"" + escapeJson(col) + "\": \"" + escapeJson(val) + "\"");
                    if (c < header.size() - 1) {
                        writer.write(",");
                    }
                    writer.write("\n");
                }
                writer.write("  }");
                if (r < records.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }
            writer.write("]\n");
            writer.flush();
        } catch (IOException e) {
            throw new ConverterException("Failed to write JSON output: " + e.getMessage(), e);
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < ' ') {
                        String hex = String.format("\\u%04x", (int) ch);
                        sb.append(hex);
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Lightweight recursive-descent parser for JSON arrays of key-value objects.
     */
    private static class SimpleJsonParser {
        private final String text;
        private int pos = 0;

        SimpleJsonParser(String text) {
            this.text = text;
        }

        List<Map<String, String>> parseArrayOfObjects() throws InvalidFileStructureException {
            skipWhitespace();
            expect('[');
            List<Map<String, String>> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }

            while (true) {
                skipWhitespace();
                Map<String, String> obj = parseObject();
                list.add(obj);
                skipWhitespace();
                char next = peek();
                if (next == ',') {
                    pos++;
                } else if (next == ']') {
                    pos++;
                    break;
                } else {
                    throw new InvalidFileStructureException("Expected ',' or ']' in JSON array, got: '" + next + "' at pos " + pos);
                }
            }
            return list;
        }

        private Map<String, String> parseObject() throws InvalidFileStructureException {
            skipWhitespace();
            expect('{');
            Map<String, String> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                String value = parsePrimitiveOrString();
                map.put(key, value);
                skipWhitespace();
                char next = peek();
                if (next == ',') {
                    pos++;
                } else if (next == '}') {
                    pos++;
                    break;
                } else {
                    throw new InvalidFileStructureException("Expected ',' or '}' in JSON object, got: '" + next + "' at pos " + pos);
                }
            }
            return map;
        }

        private String parsePrimitiveOrString() throws InvalidFileStructureException {
            skipWhitespace();
            char c = peek();
            if (c == '"') {
                return parseString();
            }
            if (c == '{' || c == '[') {
                throw new InvalidFileStructureException("Nested objects/arrays are not supported in flat tabular model at pos " + pos);
            }
            // Parse unquoted boolean, number, or null
            int start = pos;
            while (pos < text.length() && text.charAt(pos) != ',' && text.charAt(pos) != '}' && !Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
            String raw = text.substring(start, pos);
            if ("null".equals(raw)) {
                return "";
            }
            return raw;
        }

        private String parseString() throws InvalidFileStructureException {
            skipWhitespace();
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < text.length()) {
                char c = text.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (pos >= text.length()) {
                        throw new InvalidFileStructureException("Unfinished escape sequence in JSON string.");
                    }
                    char esc = text.charAt(pos++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 > text.length()) {
                                throw new InvalidFileStructureException("Invalid unicode escape in JSON string.");
                            }
                            String hex = text.substring(pos, pos + 4);
                            pos += 4;
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException e) {
                                throw new InvalidFileStructureException("Invalid unicode hex digits: " + hex);
                            }
                        }
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new InvalidFileStructureException("Unclosed string in JSON input.");
        }

        private void skipWhitespace() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            if (pos >= text.length()) return '\0';
            return text.charAt(pos);
        }

        private void expect(char expected) throws InvalidFileStructureException {
            if (pos >= text.length()) {
                throw new InvalidFileStructureException("Unexpected end of JSON input, expected: '" + expected + "'");
            }
            char actual = text.charAt(pos++);
            if (actual != expected) {
                throw new InvalidFileStructureException("Expected '" + expected + "' but found '" + actual + "' at pos " + (pos - 1));
            }
        }
    }
}
