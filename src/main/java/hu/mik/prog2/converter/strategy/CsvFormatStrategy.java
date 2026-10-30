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
import java.util.List;
import java.util.Map;

/**
 * Concrete strategy for parsing and serializing CSV (Comma-Separated Values) format.
 * Fully supports RFC 4180 rules including quoted fields, escaped quotes, and commas.
 */
public class CsvFormatStrategy extends AbstractFormatStrategy {

    private final char delimiter;

    public CsvFormatStrategy() {
        this(',');
    }

    public CsvFormatStrategy(char delimiter) {
        super(FormatType.CSV);
        this.delimiter = delimiter;
    }

    @Override
    public Document parse(InputStream inputStream) throws ConverterException {
        BufferedReader reader = createReader(inputStream);
        List<List<String>> allRows = new ArrayList<>();

        try {
            String currentLine;
            int lineNumber = 0;
            StringBuilder multiLineBuffer = new StringBuilder();
            boolean insideQuotes = false;

            while ((currentLine = reader.readLine()) != null) {
                lineNumber++;
                if (!insideQuotes && currentLine.trim().isEmpty()) {
                    continue; // Skip empty lines between records
                }

                if (multiLineBuffer.length() > 0) {
                    multiLineBuffer.append("\n");
                }
                multiLineBuffer.append(currentLine);

                // Count non-escaped quotes to determine if multiline continues
                insideQuotes = isInsideQuotes(multiLineBuffer.toString());
                if (!insideQuotes) {
                    List<String> parsedRow = parseCsvLine(multiLineBuffer.toString(), delimiter);
                    allRows.add(parsedRow);
                    multiLineBuffer.setLength(0);
                }
            }

            if (insideQuotes) {
                throw new InvalidFileStructureException("Unclosed quote detected at end of CSV file.", lineNumber);
            }
        } catch (IOException e) {
            throw new ConverterException("Failed to read CSV input stream: " + e.getMessage(), e);
        }

        if (allRows.isEmpty()) {
            throw new InvalidFileStructureException("CSV file is empty; expected at least a header row.");
        }

        List<String> headerTokens = allRows.get(0);
        DocumentHeader header = new DocumentHeader(headerTokens);

        List<DocumentRecord> records = new ArrayList<>(allRows.size() - 1);
        for (int r = 1; r < allRows.size(); r++) {
            List<String> rowTokens = allRows.get(r);
            Map<String, String> rowMap = new LinkedHashMap<>();

            for (int c = 0; c < header.size(); c++) {
                String colName = header.getColumn(c);
                String cellValue = (c < rowTokens.size()) ? rowTokens.get(c) : "";
                rowMap.put(colName, cellValue);
            }
            records.add(new DocumentRecord(rowMap));
        }

        return new Document(header, records);
    }

    @Override
    public void write(Document document, OutputStream outputStream) throws ConverterException {
        validateDocumentForWriting(document);
        BufferedWriter writer = createWriter(outputStream);

        try {
            DocumentHeader header = document.getHeader();
            // Write header
            for (int i = 0; i < header.size(); i++) {
                writer.write(escapeCell(header.getColumn(i), delimiter));
                if (i < header.size() - 1) {
                    writer.write(delimiter);
                }
            }
            writer.newLine();

            // Write data rows
            for (DocumentRecord record : document.getRecords()) {
                for (int i = 0; i < header.size(); i++) {
                    String col = header.getColumn(i);
                    String val = record.getValueOrDefault(col, "");
                    writer.write(escapeCell(val, delimiter));
                    if (i < header.size() - 1) {
                        writer.write(delimiter);
                    }
                }
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            throw new ConverterException("Failed to write CSV output: " + e.getMessage(), e);
        }
    }

    private boolean isInsideQuotes(String text) {
        boolean inside = false;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '"') {
                inside = !inside;
            }
        }
        return inside;
    }

    private List<String> parseCsvLine(String line, char delim) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote "" -> "
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delim && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens;
    }

    private String escapeCell(String value, char delim) {
        if (value == null) return "";
        boolean needsQuotes = value.indexOf(delim) >= 0 ||
                value.indexOf('"') >= 0 ||
                value.indexOf('\n') >= 0 ||
                value.indexOf('\r') >= 0;

        if (!needsQuotes) {
            return value;
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
