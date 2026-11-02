# CSV Analyzer

CSV Analyzer is a Java Swing desktop application for importing and exploring CSV datasets. Analysis runs locally; MySQL is optional and is only used for the legacy saved-dataset list.

## Features

- Reads UTF-8 CSV files with comma, semicolon, tab, or pipe delimiters.
- Supports quoted delimiters, escaped quotes, and quoted fields spanning multiple lines.
- Handles UTF-8 byte-order marks, duplicate/blank column names, and rows with missing trailing cells.
- Reports row/column counts, missing values, duplicate rows, distinct values, and per-column profiles.
- Detects numeric columns and calculates mean, median, standard deviation, minimum, and maximum.
- Calculates Pearson correlations for numeric column pairs, plots selectable numeric histograms (optionally grouped by a category), and creates pie charts by category using row counts or the sum of a selected numeric column.
- Previews up to 1,000 rows while retaining the complete dataset in memory for analysis.

Missing values are blank cells or the case-insensitive markers `NA`, `N/A`, `null`, and `none`. Numeric correlations describe linear association and do not imply causation.

## Requirements

- Java 8 or later
- MySQL Connector/J is only needed to use the optional saved-dataset screen.

## Build and run

From the project root on Windows:

```powershell
New-Item -ItemType Directory -Force out
javac -encoding UTF-8 -cp "lib\mysql-connector-j-26.7.0.jar" -d out src\main\java\analysis\CsvDataset.java src\main\java\analysis\DistributionChart.java src\main\java\database\databaseManager.java src\main\java\view\MainFrame.java src\main\java\Main.java
java -cp "out;lib\mysql-connector-j-26.7.0.jar" Main
```

The database driver can be omitted from the classpath when using only local CSV analysis:

```powershell
javac -encoding UTF-8 -d out src\main\java\analysis\CsvDataset.java src\main\java\analysis\DistributionChart.java src\main\java\database\databaseManager.java src\main\java\view\MainFrame.java src\main\java\Main.java
java -cp out Main
```

## Optional MySQL configuration

The default connection URL is `jdbc:mysql://localhost:3306/csv_analyser` and the default username is `root`. Configure credentials through environment variables rather than source code:

```powershell
$env:CSV_ANALYZER_DB_URL = "jdbc:mysql://localhost:3306/csv_analyser"
$env:CSV_ANALYZER_DB_USER = "root"
$env:CSV_ANALYZER_DB_PASSWORD = "your-local-password"
```

The optional database screen expects a `datasets` table with `dataset_id`, `filename`, `row_count`, and `column_count` columns. CSV import and analysis do not require a database.

## Run the analysis checks

```powershell
New-Item -ItemType Directory -Force out
javac -encoding UTF-8 -d out src\main\java\analysis\CsvDataset.java src\test\java\analysis\CsvDatasetTest.java
java -cp out analysis.CsvDatasetTest
```
