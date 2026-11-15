# User Stories - DataMorph File Converter

## User Story 1: Single File Conversion
- **As a** developer or data analyst,
- **I want to** convert a data file (e.g., CSV) into another format (JSON or XML),
- **So that** I can integrate data between incompatible systems without manual reformatting.
- **Acceptance Criteria:**
  - The application detects the format automatically from file extensions or prompts the user.
  - The converted file retains all data fields and values accurately.
  - Escaped characters, quotes, and commas are properly handled.
  - The conversion runtime and record count are logged in history.

## User Story 2: Batch Directory Conversion
- **As a** system administrator,
- **I want to** convert all supported files within an entire folder into a selected target format,
- **So that** I do not have to convert dozens of files one by one.
- **Acceptance Criteria:**
  - The system iterates over all regular files in the source folder.
  - Non-supported files are skipped gracefully without aborting the process.
  - A summary report shows total count, successful conversions, and any failures with reasons.

## User Story 3: Persistent Conversion History and Auditing
- **As a** user,
- **I want** the system to remember my conversion jobs across application restarts,
- **So that** I can audit past operations, check error messages for failed conversions, and track processed volumes.
- **Acceptance Criteria:**
  - Conversion results are automatically persisted to disk (`history.json`).
  - Next time the application starts, previous history items are restored.
  - The user can clear the history with explicit confirmation.

## User Story 4: History Sorting and Filtering
- **As a** user viewing conversion history,
- **I want to** sort past conversion records by different criteria (newest first, duration, file size, source name, or status),
- **So that** I can easily find slow conversions or recent operations.
- **Acceptance Criteria:**
  - Natural sorting orders items newest first using `Comparable<ConversionHistoryItem>`.
  - Additional sorting orders are available via `Comparator<ConversionHistoryItem>` (by execution time, file size, name, status).

## User Story 5: Re-converting Files from History
- **As a** user,
- **I want to** select an item from my past conversion history and re-convert it to another format,
- **So that** I don't have to re-type long file paths when chaining conversions or converting an output file further.
- **Acceptance Criteria:**
  - The system displays past conversion jobs with numbered indexes.
  - The user can pick whether to re-convert the original source file or the previous output file.
  - The user provides the new destination file path.
  - The new conversion is executed and appended to the persistent history.
