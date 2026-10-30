# UML Class Diagram - DataMorph File Converter

This document provides a viewable UML class diagram rendered with Mermaid. The editable PlantUML source is available at `docs/uml-class-diagram.puml`.

```mermaid
classDiagram
    direction TB

    class FormatType {
        <<enumeration>>
        CSV
        JSON
        XML
        +getExtension() String
        +getMimeType() String
        +fromFileNameOrExtension(name) FormatType
    }

    class DocumentHeader {
        -List~String~ columns
        +DocumentHeader(columns)
        +getColumns() List~String~
        +size() int
        +getColumn(index) String
        +contains(name) boolean
    }

    class DocumentRecord {
        -Map~String, String~ data
        +DocumentRecord(data)
        +getValue(column) String
        +getValueOrDefault(column, defaultVal) String
        +getData() Map~String, String~
        +size() int
    }

    class Document {
        -DocumentHeader header
        -List~DocumentRecord~ records
        +Document(header, records)
        +getHeader() DocumentHeader
        +getRecords() List~DocumentRecord~
        +getRecordCount() int
        +getColumnCount() int
        +isEmpty() boolean
    }

    class ConversionJob {
        -Path sourcePath
        -Path targetPath
        -FormatType sourceFormat
        -FormatType targetFormat
        -LocalDateTime requestedAt
        +getSourcePath() Path
        +getTargetPath() Path
        +getSourceFormat() FormatType
        +getTargetFormat() FormatType
    }

    class ConversionHistoryItem {
        -String id
        -LocalDateTime timestamp
        -String sourcePath
        -String targetPath
        -FormatType sourceFormat
        -FormatType targetFormat
        -long fileSizeBytes
        -int recordCount
        -long durationMillis
        -boolean success
        -String errorMessage
        +compareTo(other) int
        +getId() String
        +getTimestamp() LocalDateTime
        +isSuccess() boolean
    }

    class BatchResult~T~ {
        -List~T~ successfulItems
        -List~T~ failedItems
        -List~String~ failureMessages
        +addSuccess(item) void
        +addFailure(item, msg) void
        +getSuccessfulItems() List~T~
        +getFailedItems() List~T~
        +getTotalCount() int
        +isAllSuccessful() boolean
    }

    class FormatStrategy {
        <<interface>>
        +parse(in) Document
        +write(doc, out) void
        +getFormatType() FormatType
    }

    class AbstractFormatStrategy {
        <<abstract>>
        #Charset DEFAULT_CHARSET
        -FormatType formatType
        +getFormatType() FormatType
        #createReader(in) BufferedReader
        #createWriter(out) BufferedWriter
        #validateDocumentForWriting(doc) void
    }

    class CsvFormatStrategy {
        -char delimiter
        +parse(in) Document
        +write(doc, out) void
    }

    class JsonFormatStrategy {
        +parse(in) Document
        +write(doc, out) void
    }

    class XmlFormatStrategy {
        +parse(in) Document
        +write(doc, out) void
    }

    class FormatStrategyFactory {
        -Map~FormatType, FormatStrategy~ strategyRegistry
        +registerStrategy(strategy) void
        +getStrategy(formatType) FormatStrategy
        +getStrategyForPath(path) FormatStrategy
    }

    class HistoryRepository {
        <<interface>>
        +loadAll() List~ConversionHistoryItem~
        +saveAll(items) void
        +append(item) void
        +clear() void
    }

    class JsonHistoryRepository {
        -Path storagePath
        +loadAll() List~ConversionHistoryItem~
        +saveAll(items) void
        +append(item) void
        +clear() void
    }

    class HistoryService {
        -HistoryRepository repository
        -List~ConversionHistoryItem~ historyList
        +record(item) void
        +getHistorySortedNatural() List~ConversionHistoryItem~
        +getHistorySorted(cmp) List~ConversionHistoryItem~
        +clearHistory() void
        +getCount() int
    }

    class ConverterService {
        -FormatStrategyFactory strategyFactory
        -HistoryService historyService
        +convert(job) ConversionHistoryItem
        +convert(src, tgt) ConversionHistoryItem
        +convertBatch(jobs) BatchResult~ConversionHistoryItem~
        +convertDirectory(srcDir, tgtDir, fmt) BatchResult~ConversionHistoryItem~
    }

    class ConsoleMenu {
        -ConverterService converterService
        -HistoryService historyService
        -Scanner scanner
        +run() void
    }

    FormatStrategy <|.. AbstractFormatStrategy : implements
    AbstractFormatStrategy <|-- CsvFormatStrategy : extends
    AbstractFormatStrategy <|-- JsonFormatStrategy : extends
    AbstractFormatStrategy <|-- XmlFormatStrategy : extends

    HistoryRepository <|.. JsonHistoryRepository : implements

    Document *-- DocumentHeader : composition (1 to 1)
    Document *-- DocumentRecord : composition (1 to 0..*)
    HistoryService o-- ConversionHistoryItem : aggregation (1 to 0..*)

    ConverterService --> FormatStrategyFactory : uses
    ConverterService --> HistoryService : records to
    HistoryService --> HistoryRepository : delegates persistence
    FormatStrategyFactory ..> FormatStrategy : creates
    ConsoleMenu --> ConverterService : interacts
```
