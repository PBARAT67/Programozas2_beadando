package hu.mik.prog2.converter.persistence;

import hu.mik.prog2.converter.exception.ConverterException;
import hu.mik.prog2.converter.model.ConversionHistoryItem;
import hu.mik.prog2.converter.model.FormatType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Concrete persistence implementation that saves and restores ConversionHistoryItem
 * instances as JSON using strict try-with-resources.
 */
public class JsonHistoryRepository implements HistoryRepository {

    private final Path storagePath;

    public JsonHistoryRepository(Path storagePath) {
        this.storagePath = Objects.requireNonNull(storagePath, "storagePath cannot be null");
    }

    @Override
    public List<ConversionHistoryItem> loadAll() throws ConverterException {
        if (!Files.exists(storagePath)) {
            return new ArrayList<>();
        }

        List<ConversionHistoryItem> items = new ArrayList<>();
        // try-with-resources pattern for clean resource management
        try (BufferedReader reader = Files.newBufferedReader(storagePath, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            String content = sb.toString().trim();
            if (content.isEmpty() || content.equals("[]")) {
                return items;
            }

            List<Map<String, String>> parsedObjects = parseJsonArray(content);
            for (Map<String, String> obj : parsedObjects) {
                try {
                    String id = obj.get("id");
                    String tsStr = obj.get("timestamp");
                    LocalDateTime ts = (tsStr != null && !tsStr.isBlank())
                            ? LocalDateTime.parse(tsStr)
                            : LocalDateTime.now();

                    String sourcePath = obj.getOrDefault("sourcePath", "");
                    String targetPath = obj.getOrDefault("targetPath", "");
                    FormatType srcFmt = FormatType.valueOf(obj.getOrDefault("sourceFormat", "CSV"));
                    FormatType tgtFmt = FormatType.valueOf(obj.getOrDefault("targetFormat", "JSON"));
                    long fileSize = Long.parseLong(obj.getOrDefault("fileSizeBytes", "0"));
                    int recordCount = Integer.parseInt(obj.getOrDefault("recordCount", "0"));
                    long duration = Long.parseLong(obj.getOrDefault("durationMillis", "0"));
                    boolean success = Boolean.parseBoolean(obj.getOrDefault("success", "false"));
                    String error = obj.getOrDefault("errorMessage", "");

                    items.add(new ConversionHistoryItem(id, ts, sourcePath, targetPath, srcFmt, tgtFmt,
                            fileSize, recordCount, duration, success, error));
                } catch (Exception e) {
                    // Skip malformed record, continue with valid ones
                }
            }
        } catch (IOException e) {
            throw new ConverterException("Failed to load conversion history from: " + storagePath, e);
        }

        return items;
    }

    @Override
    public synchronized void saveAll(List<ConversionHistoryItem> items) throws ConverterException {
        if (items == null) {
            items = Collections.emptyList();
        }

        try {
            if (storagePath.getParent() != null && !Files.exists(storagePath.getParent())) {
                Files.createDirectories(storagePath.getParent());
            }

            // try-with-resources
            try (BufferedWriter writer = Files.newBufferedWriter(storagePath, StandardCharsets.UTF_8)) {
                writer.write("[\n");
                for (int i = 0; i < items.size(); i++) {
                    ConversionHistoryItem item = items.get(i);
                    writer.write("  {\n");
                    writer.write("    \"id\": \"" + escape(item.getId()) + "\",\n");
                    writer.write("    \"timestamp\": \"" + escape(item.getTimestamp().toString()) + "\",\n");
                    writer.write("    \"sourcePath\": \"" + escape(item.getSourcePath()) + "\",\n");
                    writer.write("    \"targetPath\": \"" + escape(item.getTargetPath()) + "\",\n");
                    writer.write("    \"sourceFormat\": \"" + item.getSourceFormat().name() + "\",\n");
                    writer.write("    \"targetFormat\": \"" + item.getTargetFormat().name() + "\",\n");
                    writer.write("    \"fileSizeBytes\": " + item.getFileSizeBytes() + ",\n");
                    writer.write("    \"recordCount\": " + item.getRecordCount() + ",\n");
                    writer.write("    \"durationMillis\": " + item.getDurationMillis() + ",\n");
                    writer.write("    \"success\": " + item.isSuccess() + ",\n");
                    writer.write("    \"errorMessage\": \"" + escape(item.getErrorMessage()) + "\"\n");
                    writer.write("  }");
                    if (i < items.size() - 1) {
                        writer.write(",");
                    }
                    writer.write("\n");
                }
                writer.write("]\n");
                writer.flush();
            }
        } catch (IOException e) {
            throw new ConverterException("Failed to save conversion history to: " + storagePath, e);
        }
    }

    @Override
    public synchronized void append(ConversionHistoryItem item) throws ConverterException {
        if (item == null) return;
        List<ConversionHistoryItem> all = loadAll();
        all.add(item);
        saveAll(all);
    }

    @Override
    public synchronized void clear() throws ConverterException {
        try {
            if (Files.exists(storagePath)) {
                Files.delete(storagePath);
            }
        } catch (IOException e) {
            throw new ConverterException("Failed to clear history file: " + storagePath, e);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private List<Map<String, String>> parseJsonArray(String json) {
        List<Map<String, String>> list = new ArrayList<>();
        int i = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (i < 0 || end < 0) return list;

        String inner = json.substring(i + 1, end).trim();
        if (inner.isEmpty()) return list;

        int depth = 0;
        int objStart = -1;
        boolean inQuote = false;

        for (int p = 0; p < inner.length(); p++) {
            char c = inner.charAt(p);
            if (c == '"' && (p == 0 || inner.charAt(p - 1) != '\\')) {
                inQuote = !inQuote;
            } else if (!inQuote) {
                if (c == '{') {
                    if (depth == 0) objStart = p;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && objStart >= 0) {
                        String objStr = inner.substring(objStart + 1, p);
                        list.add(parseObjectProperties(objStr));
                        objStart = -1;
                    }
                }
            }
        }
        return list;
    }

    private Map<String, String> parseObjectProperties(String objStr) {
        Map<String, String> map = new LinkedHashMap<>();
        String[] lines = objStr.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : lines) {
            int colonIdx = pair.indexOf(':');
            if (colonIdx > 0) {
                String k = stripQuotes(pair.substring(0, colonIdx).trim());
                String v = stripQuotes(pair.substring(colonIdx + 1).trim());
                map.put(k, v);
            }
        }
        return map;
    }

    private String stripQuotes(String val) {
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
            val = val.substring(1, val.length() - 1);
        }
        return val.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n").replace("\\r", "\r");
    }
}
