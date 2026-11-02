package analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CsvDataset {
    private final String filename;
    private final List<String> columnNames;
    private final List<List<String>> rows;
    private final List<ColumnSummary> summaries;
    private final int missingValueCount;
    private final int duplicateRowCount;

    private CsvDataset(String filename, List<String> columnNames, List<List<String>> rows) {
        this.filename = filename;
        this.columnNames = Collections.unmodifiableList(columnNames);
        this.rows = Collections.unmodifiableList(rows);
        this.missingValueCount = countMissingValues(rows);
        this.duplicateRowCount = countDuplicateRows(rows);

        List<ColumnSummary> calculatedSummaries = new ArrayList<ColumnSummary>();
        for (int column = 0; column < columnNames.size(); column++) {
            calculatedSummaries.add(summarizeColumn(column, rows));
        }
        this.summaries = Collections.unmodifiableList(calculatedSummaries);
    }

    public static CsvDataset load(Path path) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IOException("The selected path is not a readable file.");
        }

        char delimiter;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String firstLine = reader.readLine();
            delimiter = detectDelimiter(firstLine == null ? "" : firstLine);
        }

        List<List<String>> records = new ArrayList<List<String>>();
        try (PushbackReader reader = new PushbackReader(
                Files.newBufferedReader(path, StandardCharsets.UTF_8), 1)) {
            parseRecords(reader, delimiter, records);
        }

        if (records.isEmpty() || (records.size() == 1 && records.get(0).size() == 1
                && records.get(0).get(0).trim().isEmpty())) {
            throw new IOException("The selected CSV file is empty.");
        }

        List<String> headers = records.remove(0);
        int columnCount = headers.size();
        for (List<String> record : records) {
            columnCount = Math.max(columnCount, record.size());
        }
        if (columnCount == 0) {
            throw new IOException("The CSV file does not contain any columns.");
        }

        List<String> columnNames = makeUniqueHeaders(headers, columnCount);
        List<List<String>> rows = new ArrayList<List<String>>();
        for (List<String> record : records) {
            if (record.size() == 1 && record.get(0).trim().isEmpty()) {
                continue;
            }
            List<String> normalized = new ArrayList<String>(columnCount);
            for (int column = 0; column < columnCount; column++) {
                normalized.add(column < record.size() ? record.get(column) : "");
            }
            rows.add(Collections.unmodifiableList(normalized));
        }

        return new CsvDataset(path.getFileName().toString(), columnNames, rows);
    }

    private static void parseRecords(PushbackReader reader, char delimiter,
            List<List<String>> records) throws IOException {
        List<String> row = new ArrayList<String>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        boolean firstCharacter = true;
        int value;

        while ((value = reader.read()) != -1) {
            char character = (char) value;
            if (firstCharacter) {
                firstCharacter = false;
                if (character == '\uFEFF') {
                    continue;
                }
            }

            if (quoted) {
                if (character == '"') {
                    int next = reader.read();
                    if (next == '"') {
                        cell.append('"');
                    } else {
                        quoted = false;
                        if (next != -1) {
                            reader.unread(next);
                        }
                    }
                } else {
                    cell.append(character);
                }
            } else if (character == '"' && cell.length() == 0) {
                quoted = true;
            } else if (character == delimiter) {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (character == '\n' || character == '\r') {
                if (character == '\r') {
                    int next = reader.read();
                    if (next != '\n' && next != -1) {
                        reader.unread(next);
                    }
                }
                row.add(cell.toString());
                cell.setLength(0);
                if (!(row.size() == 1 && row.get(0).trim().isEmpty())) {
                    records.add(row);
                }
                row = new ArrayList<String>();
            } else {
                cell.append(character);
            }
        }

        if (quoted) {
            throw new IOException("The CSV file ends inside a quoted field.");
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            records.add(row);
        }
    }

    private static char detectDelimiter(String line) {
        char[] candidates = { ',', ';', '\t', '|' };
        char best = ',';
        int bestCount = -1;
        boolean quoted = false;
        String sample = line.startsWith("\uFEFF") ? line.substring(1) : line;

        for (char candidate : candidates) {
            int count = 0;
            quoted = false;
            for (int index = 0; index < sample.length(); index++) {
                char character = sample.charAt(index);
                if (character == '"') {
                    if (quoted && index + 1 < sample.length()
                            && sample.charAt(index + 1) == '"') {
                        index++;
                    } else {
                        quoted = !quoted;
                    }
                } else if (!quoted && character == candidate) {
                    count++;
                }
            }
            if (count > bestCount) {
                bestCount = count;
                best = candidate;
            }
        }
        return best;
    }

    private static List<String> makeUniqueHeaders(List<String> headers, int columnCount) {
        List<String> names = new ArrayList<String>();
        Set<String> used = new HashSet<String>();
        for (int column = 0; column < columnCount; column++) {
            String base = column < headers.size() ? headers.get(column).trim() : "";
            if (base.isEmpty()) {
                base = "Column " + (column + 1);
            }
            String name = base;
            int suffix = 2;
            while (!used.add(name.toLowerCase(java.util.Locale.ROOT))) {
                name = base + " (" + suffix++ + ")";
            }
            names.add(name);
        }
        return names;
    }

    private static int countMissingValues(List<List<String>> rows) {
        int count = 0;
        for (List<String> row : rows) {
            for (String value : row) {
                if (isMissing(value)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countDuplicateRows(List<List<String>> rows) {
        Set<List<String>> seen = new HashSet<List<String>>();
        int duplicates = 0;
        for (List<String> row : rows) {
            if (!seen.add(row)) {
                duplicates++;
            }
        }
        return duplicates;
    }

    private static ColumnSummary summarizeColumn(int column, List<List<String>> rows) {
        int missing = 0;
        int nonMissingCount = 0;
        int distinctCount;
        List<Double> numbers = new ArrayList<Double>();
        Map<String, Integer> frequencies = new LinkedHashMap<String, Integer>();

        for (List<String> row : rows) {
            String value = row.get(column);
            if (isMissing(value)) {
                missing++;
                continue;
            }
            nonMissingCount++;
            Double number = parseNumber(value);
            if (number != null) {
                numbers.add(number);
            }
            frequencies.put(value, frequencies.containsKey(value)
                    ? frequencies.get(value) + 1 : 1);
        }
        distinctCount = frequencies.size();
        List<Map.Entry<String, Integer>> topValues =
                new ArrayList<Map.Entry<String, Integer>>(frequencies.entrySet());
        Collections.sort(topValues, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> first,
                    Map.Entry<String, Integer> second) {
                int byCount = Integer.compare(second.getValue(), first.getValue());
                return byCount != 0 ? byCount : first.getKey().compareTo(second.getKey());
            }
        });
        if (topValues.size() > 5) {
            topValues = new ArrayList<Map.Entry<String, Integer>>(topValues.subList(0, 5));
        }

        boolean numeric = nonMissingCount > 0 && numbers.size() == nonMissingCount;
        double mean = Double.NaN;
        double median = Double.NaN;
        double minimum = Double.NaN;
        double maximum = Double.NaN;
        double standardDeviation = Double.NaN;
        if (numeric) {
            Collections.sort(numbers);
            double sum = 0;
            for (Double number : numbers) {
                sum += number;
            }
            mean = sum / numbers.size();
            int middle = numbers.size() / 2;
            median = numbers.size() % 2 == 0
                    ? (numbers.get(middle - 1) + numbers.get(middle)) / 2.0
                    : numbers.get(middle);
            minimum = numbers.get(0);
            maximum = numbers.get(numbers.size() - 1);
            double squaredDifferences = 0;
            for (Double number : numbers) {
                double difference = number - mean;
                squaredDifferences += difference * difference;
            }
            standardDeviation = Math.sqrt(squaredDifferences / numbers.size());
        }
        return new ColumnSummary(missing, distinctCount, numeric, mean, median,
                minimum, maximum, standardDeviation, topValues);
    }

    private static boolean isMissing(String value) {
        if (value == null) {
            return true;
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.isEmpty() || normalized.equals("na")
                || normalized.equals("n/a") || normalized.equals("null")
                || normalized.equals("none");
    }

    private static Double parseNumber(String value) {
        try {
            double parsed = Double.parseDouble(value.trim());
            return Double.isNaN(parsed) || Double.isInfinite(parsed) ? null : parsed;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public List<Relationship> getNumericRelationships() {
        List<Integer> numericColumns = new ArrayList<Integer>();
        for (int index = 0; index < summaries.size(); index++) {
            if (summaries.get(index).isNumeric()) {
                numericColumns.add(index);
            }
        }

        List<Relationship> relationships = new ArrayList<Relationship>();
        for (int first = 0; first < numericColumns.size(); first++) {
            for (int second = first + 1; second < numericColumns.size(); second++) {
                int left = numericColumns.get(first);
                int right = numericColumns.get(second);
                Relationship relationship = correlate(left, right);
                if (relationship != null) {
                    relationships.add(relationship);
                }
            }
        }
        Collections.sort(relationships, new Comparator<Relationship>() {
            @Override
            public int compare(Relationship first, Relationship second) {
                return Double.compare(Math.abs(second.getCorrelation()),
                        Math.abs(first.getCorrelation()));
            }
        });
        if (relationships.size() > 10) {
            return new ArrayList<Relationship>(relationships.subList(0, 10));
        }
        return relationships;
    }

    private Relationship correlate(int firstColumn, int secondColumn) {
        List<Double> firstValues = new ArrayList<Double>();
        List<Double> secondValues = new ArrayList<Double>();
        for (List<String> row : rows) {
            Double first = parseNumber(row.get(firstColumn));
            Double second = parseNumber(row.get(secondColumn));
            if (first != null && second != null) {
                firstValues.add(first);
                secondValues.add(second);
            }
        }
        if (firstValues.size() < 2) {
            return null;
        }
        double firstMean = mean(firstValues);
        double secondMean = mean(secondValues);
        double covariance = 0;
        double firstVariance = 0;
        double secondVariance = 0;
        for (int index = 0; index < firstValues.size(); index++) {
            double firstDifference = firstValues.get(index) - firstMean;
            double secondDifference = secondValues.get(index) - secondMean;
            covariance += firstDifference * secondDifference;
            firstVariance += firstDifference * firstDifference;
            secondVariance += secondDifference * secondDifference;
        }
        if (firstVariance == 0 || secondVariance == 0) {
            return null;
        }
        return new Relationship(columnNames.get(firstColumn), columnNames.get(secondColumn),
                covariance / Math.sqrt(firstVariance * secondVariance), firstValues.size());
    }

    private static double mean(List<Double> values) {
        double sum = 0;
        for (Double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    public String getFilename() {
        return filename;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public List<ColumnSummary> getSummaries() {
        return summaries;
    }

    public int getMissingValueCount() {
        return missingValueCount;
    }

    public int getDuplicateRowCount() {
        return duplicateRowCount;
    }

    public static final class ColumnSummary {
        private final int missingCount;
        private final int distinctCount;
        private final boolean numeric;
        private final double mean;
        private final double median;
        private final double minimum;
        private final double maximum;
        private final double standardDeviation;
        private final List<Map.Entry<String, Integer>> topValues;

        private ColumnSummary(int missingCount, int distinctCount, boolean numeric,
                double mean, double median, double minimum, double maximum,
                double standardDeviation, List<Map.Entry<String, Integer>> topValues) {
            this.missingCount = missingCount;
            this.distinctCount = distinctCount;
            this.numeric = numeric;
            this.mean = mean;
            this.median = median;
            this.minimum = minimum;
            this.maximum = maximum;
            this.standardDeviation = standardDeviation;
            this.topValues = Collections.unmodifiableList(topValues);
        }

        public int getMissingCount() { return missingCount; }
        public int getDistinctCount() { return distinctCount; }
        public boolean isNumeric() { return numeric; }
        public double getMean() { return mean; }
        public double getMedian() { return median; }
        public double getMinimum() { return minimum; }
        public double getMaximum() { return maximum; }
        public double getStandardDeviation() { return standardDeviation; }
        public List<Map.Entry<String, Integer>> getTopValues() { return topValues; }
    }

    public static final class Relationship {
        private final String firstColumn;
        private final String secondColumn;
        private final double correlation;
        private final int sampleSize;

        private Relationship(String firstColumn, String secondColumn,
                double correlation, int sampleSize) {
            this.firstColumn = firstColumn;
            this.secondColumn = secondColumn;
            this.correlation = correlation;
            this.sampleSize = sampleSize;
        }

        public String getFirstColumn() { return firstColumn; }
        public String getSecondColumn() { return secondColumn; }
        public double getCorrelation() { return correlation; }
        public int getSampleSize() { return sampleSize; }
    }
}
