# DataMorph File Converter (PTE MIK - Prog 2)

**DataMorph** is a robust, object-oriented Java application designed to convert structured and tabular data between different file formats (**CSV**, **JSON**, and **XML**). Built as a semester assignment for the **Prog 2 (Java OOP)** course at **Pécsi Tudományegyetem Műszaki és Informatikai Kar (PTE MIK)**.

---

## 1. Project Summary & Problem Statement

### The Problem
In modern software workflows, data is frequently exported, imported, and shared in incompatible formats: spreadsheets prefer CSV, REST APIs and web frontends rely on JSON, and legacy enterprise software or configurations demand XML. Converting between these formats is often error-prone, requiring separate ad-hoc scripts with inconsistent error handling and no audit trail.

### The Solution
DataMorph solves this by providing:
1. A unified **Intermediate Representation (`Document`)** consisting of structured headers and records.
2. Dynamic format resolution using **Factory Method Pattern** (`FormatStrategyFactory`) providing interchangeable formatting strategies to parse and serialize CSV, JSON, and XML.
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
| **Design Pattern** | **Factory Pattern / Factory Method** (`FormatStrategyFactory`) as primary pattern for object creation & **Strategy Pattern** for interchangeable format algorithms. |
| **Testing** | 18 automated **JUnit 5** tests covering parsing, writing, exceptions, batch processing, sorting, and history lookups. |
| **Build** | Standard Maven project with reproducible build (`mvn clean package`). |

---

## 3. Design Pattern Explanation

### Pattern Chosen: Factory Pattern / Factory Method (GoF)
- **Problem it solves:** In a multi-format conversion system, client components (such as `ConverterService` or `ConsoleMenu`) should not be tightly coupled to concrete parser/serializer implementations (`CsvFormatStrategy`, `JsonFormatStrategy`, `XmlFormatStrategy`). Instantiating these classes directly with `new` across the application would create brittle code, duplicate format-detection logic, and violate the Dependency Inversion and Open/Closed Principles.
- **Participating Classes:**
  - `FormatStrategyFactory`: The Factory class providing `getStrategy(FormatType)` and the resolution method `getStrategyForPath(Path)`.
  - `FormatStrategy`: The common product abstraction/interface returned by the factory.
  - `AbstractFormatStrategy`: Base product class providing shared stream and validation capabilities.
  - `CsvFormatStrategy`, `JsonFormatStrategy`, `XmlFormatStrategy`: Concrete products instantiated and registered within the factory.
  - `ConverterService`: The client that consumes format handlers solely through the factory abstraction without knowing concrete product types.
- **Benefits in this Design:**
  - **Decoupling Creation from Usage:** Client code never calls `new CsvFormatStrategy()`; it asks the factory for a handler matching a path or format enum.
  - **Single Responsibility Principle (SRP):** File extension resolution, MIME matching, and strategy lifecycle management reside entirely in one place.
  - **Extensibility (Open/Closed Principle):** Supporting a new format (e.g. YAML) requires only registering a new `FormatStrategy` product into the factory registry without modifying any client logic.

### Supporting Pattern: Strategy Pattern
- The created products implement `FormatStrategy`, allowing interchangeable parsing and writing behavior polymorphically at runtime.

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

### UML Class Diagram Formats (`docs/`)
The diagram is provided in multiple complementary formats for maximum viewability and grading convenience:
- **`docs/uml-viewer.html`**: **Interactive zoomable viewer** in pure HTML/JS (zoom in/out with mouse wheel or buttons from 10% to 600%, drag to pan). Works 100% offline in any web browser.
- **`docs/uml-class-diagram.svg`**: **Vector graphic format** (infinitely scalable without blur, zoomable in any browser with `Ctrl + Scroll`).
- **`docs/uml-class-diagram.png`**: High-resolution raster image.
- **`docs/uml-class-diagram.puml`**: Editable PlantUML source code.
- **`docs/uml-class-diagram.md`**: GitHub-viewable Mermaid class diagram.

---

## 5. Prerequisites & Build Instructions

### Prerequisites
- **Java Development Kit (JDK) 17** or newer.
- **Apache Maven 3.8+**.

### Build and Run Tests
Clone the repository and build the project from clean checkout:

```bash
# Clean, compile, run all 18 JUnit 5 tests, and package executable JAR
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
4. Re-convert a file from history
5. View conversion statistics
6. Clear conversion history
7. Exit
Select an option [1-7]:
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
