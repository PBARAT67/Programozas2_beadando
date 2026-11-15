# DataMorph File Converter (PTE MIK - Prog 2)

**DataMorph** is a robust, object-oriented Java application designed to convert structured and tabular data between different file formats (**CSV**, **JSON**, and **XML**). Built as a semester assignment for the **Prog 2 (Java OOP)** course at **Pécsi Tudományegyetem Műszaki és Informatikai Kar (PTE MIK)**.

---

## 1. Project Summary & Problem Statement

### The Problem
In modern software workflows, data is frequently exported, imported, and shared in incompatible formats: spreadsheets prefer CSV, REST APIs and web frontends rely on JSON, and legacy enterprise software or configurations demand XML. Converting between these formats is often error-prone, requiring separate ad-hoc scripts with inconsistent error handling and no audit trail.

### The Solution
DataMorph solves this by providing:
1. A unified **Intermediate Representation (`Document`)** consisting of structured headers and records.
2. Interchangeable formatting strategies (**Strategy Pattern**) to parse and serialize CSV, JSON, and XML.
3. An audit logging system that records execution time, record counts, file sizes, and status, and **persists data between application runs** in JSON format.
4. Flexible sorting and filtering capabilities using `Comparable` and `Comparator`.
5. An intuitive interactive console interface alongside scriptable CLI command-line arguments.

---

## 2. Requirements & Evidence in the Solution

| Requirement | Implementation Details in DataMorph |
| :--- | :--- |
| **Encapsulation** | All fields across models (`Document`, `DocumentHeader`, `DocumentRecord`, `ConversionHistoryItem`) are `private`. Constructors validate parameters (e.g. non-empty column names, non-negative file sizes), and getters return unmodifiable collections or defensive copies. |
| **Abstraction** | `FormatStrategy` interface establishes a stable contract (`parse`, `write`, `getFormatType`). `HistoryRepository` defines persistence abstraction. |
| **Inheritance & Polymorphism** | `AbstractFormatStrategy` acts as common base type, extended by `CsvFormatStrategy`, `JsonFormatStrategy`, and `XmlFormatStrategy`. Overridden `parse()` and `write()` methods execute polymorphically at runtime via `ConverterService`. |
| **Relationships** | - **Composition (1 to 1):** `Document` owns a `DocumentHeader`.<br>- **Composition (1 to 0..\*):** `Document` owns `DocumentRecord` items.<br>- **Aggregation (1 to 0..\*):** `HistoryService` manages `ConversionHistoryItem` instances.<br>- **Association:** `ConverterService` associates with `FormatStrategyFactory` and `HistoryService`. |
| **Collections & Generics** | Heavy use of `List<DocumentRecord>`, `Map<String, String>`, and `Set<String>`. Custom generic class `BatchResult<T>` captures batch outcomes. Sorting implemented with `Comparable<ConversionHistoryItem>` (natural sort: newest first) and multiple `Comparator<ConversionHistoryItem>` lambdas (by duration, size, name, status). |
| **Exceptions** | Domain exception hierarchy: `ConverterException` (checked base), `UnsupportedFormatException`, and `InvalidFileStructureException` (stores line number and structural failure causes). |
| **Persistence** | `JsonHistoryRepository` restores and persists conversion history between runs to `history.json`. All file I/O uses strict `try-with-resources`. |
| **Design Pattern** | **Strategy Pattern** (interchangeable format algorithms) & **Factory Method** (`FormatStrategyFactory`). |
| **Testing** | 17 automated **JUnit 5** tests covering parsing, writing, exceptions, batch processing, and sorting. |
| **Build** | Standard Maven project with reproducible build (`mvn clean package`). |

---

## 3. Design Pattern Explanation

### Pattern Chosen: Strategy Pattern (GoF)
- **Problem it solves:** Different file formats require fundamentally different parsing and writing algorithms. Hardcoding format conversions using `if-else` or `switch` statements inside business logic would violate the Open/Closed Principle (OCP) and lead to fragile, tightly-coupled code.
- **Participating Classes:**
  - `FormatStrategy`: The Strategy interface defining `Document parse(InputStream in)` and `void write(Document doc, OutputStream out)`.
  - `AbstractFormatStrategy`: Base class with shared stream utilities.
  - `CsvFormatStrategy`, `JsonFormatStrategy`, `XmlFormatStrategy`: Concrete strategy implementations containing format-specific parsing and serialization logic.
  - `ConverterService`: The Context class that executes conversions polymorphically without knowing the concrete format implementation details.
- **Benefits in this Design:**
  - Adding a new format (e.g., YAML, TSV) requires only creating a new class implementing `FormatStrategy` without modifying existing conversion code.
  - Format-specific logic is isolated and independently unit-testable.

### Supporting Pattern: Factory Method
- `FormatStrategyFactory` centralizes the resolution and instantiation of format strategies from file extensions or MIME types.

---

## 4. Architecture & Package Structure

```text
hu.mik.prog2.converter/
├── Main.java                          # Entry point (CLI & interactive launcher)
├── exception/
│   ├── ConverterException.java        # Checked domain base exception
│   ├── InvalidFileStructureException.java # Malformed syntax / structural errors
│   └── UnsupportedFormatException.java    # Unrecognized file format
├── model/
│   ├── BatchResult.java               # Generic container for batch operations
│   ├── ConversionHistoryItem.java     # Comparable domain model for audit log
│   ├── ConversionJob.java             # Value object for conversion requests
│   ├── Document.java                  # Intermediate Representation (IR)
│   ├── DocumentHeader.java            # Immutable column headers
│   ├── DocumentRecord.java            # Row key-value mapping
│   └── FormatType.java                # Enum (CSV, JSON, XML)
├── persistence/
│   ├── HistoryRepository.java         # Persistence interface
│   └── JsonHistoryRepository.java     # JSON-based try-with-resources repository
├── service/
│   ├── ConverterService.java          # Core conversion engine
│   └── HistoryService.java            # History management & sorting
├── strategy/
│   ├── AbstractFormatStrategy.java    # Shared stream base
│   ├── CsvFormatStrategy.java         # CSV parser and serializer
│   ├── FormatStrategy.java            # Strategy interface contract
│   ├── FormatStrategyFactory.java     # Factory Method registry
│   ├── JsonFormatStrategy.java        # JSON parser and serializer
│   └── XmlFormatStrategy.java         # XML DOM & Transformer serializer
└── ui/
    └── ConsoleMenu.java               # Interactive console user interface
```

---

## 5. Prerequisites & Build Instructions

### Prerequisites
- **Java Development Kit (JDK) 17** or newer.
- **Apache Maven 3.8+**.

### Build and Run Tests
Clone the repository and build the project from clean checkout:

```bash
# Clean, compile, run all 17 JUnit 5 tests, and package executable JAR
mvn clean package
```

The compiled JAR will be located at:
`target/datamorph-converter-1.0.0.jar`

---

## 6. Usage & Sample Commands

### 6.1 Interactive Console Mode
Simply launch the JAR without arguments:
```bash
java -jar target/datamorph-converter-1.0.0.jar
```
This opens the interactive text menu:
```text
============================================================
     DataMorph File Converter - PTE MIK Prog 2 Project     
     Supports: CSV, JSON, XML (Strategy Pattern & OOP)      
============================================================

--- MAIN MENU ---
1. Convert a single file
2. Convert all files in a folder (Batch)
3. View conversion history (with sorting)
4. View conversion statistics
5. Clear conversion history
6. Exit
Select an option [1-6]:
```

### 6.2 Command-Line Mode (Scriptable)

#### Single File Conversion
```bash
# Convert CSV to JSON
java -jar target/datamorph-converter-1.0.0.jar samples/students.csv output/students.json

# Convert JSON to XML
java -jar target/datamorph-converter-1.0.0.jar output/students.json output/students.xml

# Convert XML back to CSV
java -jar target/datamorph-converter-1.0.0.jar output/students.xml output/students.csv
```

#### Batch Directory Conversion
```bash
# Converts all supported files in 'samples' folder to JSON in 'batch_out'
java -jar target/datamorph-converter-1.0.0.jar --batch samples batch_out json
```

#### View Past Conversion History
```bash
java -jar target/datamorph-converter-1.0.0.jar --history
```

---

## 7. Sample Data Files
Sample input files are provided in the `samples/` directory:
- `samples/students.csv`: Tabular student records with quotes and commas.
- `samples/weather.csv`: Weather observation metrics.
- `samples/products.json`: Array of product objects.
- `samples/courses.json`: Academic course records.
- `samples/books.xml`: Standard XML document records.
- `samples/tickets.xml`: Support ticket incident data.
- `samples/batch_input/`: Dedicated folder with mixed file types for batch directory conversion testing.

---

## 8. Main Design Decisions & Known Limitations

### Design Decisions
1. **Self-Contained Standard Implementation:** The JSON, XML, and CSV parsers are built using standard Java libraries (`java.xml`, `BufferedReader`, `StreamTokenizer`). This guarantees zero external runtime dependencies and ensures the application compiles and runs on any environment without third-party JAR lookup failures.
2. **Flat Intermediate Representation:** Standard tabular documents map naturally to rows and columns, allowing seamless bi-directional conversion across CSV, JSON, and XML.
3. **Defensive Copying:** All collections returned by model entities are wrapped with `Collections.unmodifiableList` or `Collections.unmodifiableMap` to prevent unintended state mutations.

### Known Limitations
- The intermediate tabular model is designed for 2D records (rows and columns). Arbitrarily deep nested JSON objects (e.g. objects containing child arrays of objects) are not mapped to flat CSV tables.
- Extremely large files exceeding available JVM heap memory must be split or run with increased `-Xmx` memory flags because the document model resides in-memory during conversion.

---

## 9. Contributors & Academic Information
- **Course:** Programozás 2 (Prog 2) - Java OOP
- **Institution:** Pécsi Tudományegyetem Műszaki és Informatikai Kar (PTE MIK)
- **License:** MIT License (see `LICENSE`)
