package analysis;

import analysis.DistributionChart.ChartType;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CsvDatasetTest {
    public static void main(String[] args) throws Exception {
        parsesQuotedFieldsAndProfilesDataset();
        rejectsUnclosedQuotedField();
        System.out.println("CsvDataset tests passed.");
    }

    private static void parsesQuotedFieldsAndProfilesDataset() throws Exception {
        Path csv = Files.createTempFile("csv-analyzer-test", ".csv");
        try {
            String contents = "\uFEFFid;score;note;group\r\n"
                    + "1;10;\"hello; there\";A\r\n"
                    + "2;20;\"two lines\ninside\";B\r\n"
                    + "3;30;\"say \"\"hi\"\"\";A\r\n"
                    + "3;30;\"say \"\"hi\"\"\";A\r\n"
                    + "4;NA;;B\r\n";
            Files.write(csv, contents.getBytes(StandardCharsets.UTF_8));

            CsvDataset dataset = CsvDataset.load(csv);
            assertEquals(5, dataset.getRows().size(), "row count");
            assertEquals(4, dataset.getColumnNames().size(), "column count");
            assertEquals(2, dataset.getMissingValueCount(), "missing values");
            assertEquals(1, dataset.getDuplicateRowCount(), "duplicate rows");
            assertEquals("hello; there", dataset.getRows().get(0).get(2), "quoted delimiter");
            assertEquals("two lines\ninside", dataset.getRows().get(1).get(2),
                    "multiline quoted field");
            assertEquals("say \"hi\"", dataset.getRows().get(2).get(2), "escaped quote");

            CsvDataset.ColumnSummary score = dataset.getSummaries().get(1);
            assertTrue(score.isNumeric(), "numeric column detection");
            assertEquals(22.5, score.getMean(), 0.0001, "numeric mean");
            assertEquals(25.0, score.getMedian(), 0.0001, "numeric median");

            assertTrue(!dataset.getNumericRelationships().isEmpty(),
                    "numeric correlation availability");
            assertEquals(1.0, dataset.getNumericRelationships().get(0).getCorrelation(),
                    0.0001, "numeric correlation");
            assertChartRenders(dataset, ChartType.HISTOGRAM, 1, 3, "grouped histogram");
            assertChartRenders(dataset, ChartType.PIE, 3, 1, "weighted pie chart");
            assertChartRenders(dataset, ChartType.PIE, 3, -1, "row-count pie chart");
        } finally {
            Files.deleteIfExists(csv);
        }
    }

    private static void assertChartRenders(CsvDataset dataset, ChartType type,
            int primaryColumn, int secondaryColumn, String label) {
        DistributionChart chart = new DistributionChart();
        chart.setSize(900, 520);
        chart.setData(dataset, type, primaryColumn, secondaryColumn);
        BufferedImage image = new BufferedImage(900, 520, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        chart.paint(graphics);
        graphics.dispose();

        int coloredPixels = 0;
        for (int y = 30; y < image.getHeight(); y++) {
            for (int x = 10; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != Color.WHITE.getRGB()) {
                    coloredPixels++;
                }
            }
        }
        assertTrue(coloredPixels > 100, label + " rendering");
    }

    private static void rejectsUnclosedQuotedField() throws Exception {
        Path csv = Files.createTempFile("csv-analyzer-invalid", ".csv");
        try {
            Files.write(csv, "name,value\nitem,\"unfinished".getBytes(StandardCharsets.UTF_8));
            try {
                CsvDataset.load(csv);
                throw new AssertionError("Expected malformed quoted field to be rejected.");
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains("quoted field"),
                        "malformed quote error message");
            }
        } finally {
            Files.deleteIfExists(csv);
        }
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEquals(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEquals(double expected, double actual,
            double tolerance, String label) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
