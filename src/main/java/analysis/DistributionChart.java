package analysis;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DistributionChart extends JPanel {
    public enum ChartType {
        HISTOGRAM,
        PIE
    }

    private static final Color[] PALETTE = {
            new Color(37, 99, 235),
            new Color(16, 185, 129),
            new Color(245, 158, 11),
            new Color(139, 92, 246),
            new Color(236, 72, 153),
            new Color(20, 184, 166)
    };
    private static final Color GRID = new Color(225, 228, 233);
    private static final Color TEXT = new Color(75, 85, 99);
    private static final int CATEGORY_LIMIT = 8;
    private CsvDataset dataset;
    private ChartType chartType = ChartType.HISTOGRAM;
    private int primaryColumn = -1;
    private int secondaryColumn = -1;

    public DistributionChart() {
        setBackground(Color.WHITE);
    }

    public void setData(CsvDataset dataset, ChartType chartType,
            int primaryColumn, int secondaryColumn) {
        this.dataset = dataset;
        this.chartType = chartType;
        this.primaryColumn = primaryColumn;
        this.secondaryColumn = secondaryColumn;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (dataset == null || dataset.getRows().isEmpty()) {
            drawMessage(graphics, "Import a CSV file with data to view a chart.");
            return;
        }
        if (primaryColumn < 0 || primaryColumn >= dataset.getColumnNames().size()) {
            drawMessage(graphics, "Choose a column to visualize.");
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        ChartData data = chartType == ChartType.PIE
                ? buildPieData() : buildHistogramData();
        if (data.labels.isEmpty()) {
            g.setColor(TEXT);
            g.drawString("No usable values in the selected columns.", 24, 50);
            g.dispose();
            return;
        }
        drawTitle(g, data.title);
        if (chartType == ChartType.PIE) {
            drawPie(g, data);
        } else {
            drawHistogram(g, data);
        }
        g.dispose();
    }

    private void drawMessage(Graphics graphics, String message) {
        graphics.setColor(TEXT);
        graphics.drawString(message, 24, 35);
    }

    private void drawTitle(Graphics2D g, String title) {
        g.setColor(TEXT);
        g.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g.drawString(title, 24, 25);
    }

    private ChartData buildHistogramData() {
        CsvDataset.ColumnSummary summary = dataset.getSummaries().get(primaryColumn);
        int binCount = 10;
        double minimum = summary.getMinimum();
        double maximum = summary.getMaximum();
        double range = maximum - minimum;
        DecimalFormat format = new DecimalFormat("0.##");
        List<String> labels = new ArrayList<String>();
        for (int bin = 0; bin < binCount; bin++) {
            double start = minimum + range * bin / binCount;
            double end = minimum + range * (bin + 1) / binCount;
            labels.add(format.format(start) + "-" + format.format(end));
        }

        List<String> seriesNames = selectedCategoryValues();
        if (seriesNames.isEmpty()) {
            seriesNames.add("Rows");
        }
        double[][] values = new double[seriesNames.size()][binCount];
        for (List<String> row : dataset.getRows()) {
            Double number = parseNumber(row.get(primaryColumn));
            if (number == null) {
                continue;
            }
            int bin = range == 0 ? 0 : (int) ((number - minimum) / range * binCount);
            bin = Math.max(0, Math.min(binCount - 1, bin));
            int series = seriesIndex(seriesNames,
                    secondaryColumn < 0 ? "Rows" : row.get(secondaryColumn));
            if (series >= 0) {
                values[series][bin]++;
            }
        }
        String title = dataset.getColumnNames().get(primaryColumn) + " histogram";
        if (secondaryColumn >= 0) {
            title += " grouped by " + dataset.getColumnNames().get(secondaryColumn);
        }
        return new ChartData(title, labels, seriesNames, values);
    }

    private ChartData buildPieData() {
        Map<String, Double> totals = new LinkedHashMap<String, Double>();
        for (List<String> row : dataset.getRows()) {
            String category = row.get(primaryColumn);
            if (isMissing(category)) {
                continue;
            }
            double weight = 1;
            if (secondaryColumn >= 0) {
                Double number = parseNumber(row.get(secondaryColumn));
                if (number == null || number < 0) {
                    continue;
                }
                weight = number;
            }
            totals.put(category, totals.containsKey(category)
                    ? totals.get(category) + weight : weight);
        }

        List<Map.Entry<String, Double>> sorted =
                new ArrayList<Map.Entry<String, Double>>(totals.entrySet());
        Collections.sort(sorted, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> first,
                    Map.Entry<String, Double> second) {
                return Double.compare(second.getValue(), first.getValue());
            }
        });
        List<String> labels = new ArrayList<String>();
        List<String> seriesNames = Collections.singletonList("Share");
        List<Double> slices = new ArrayList<Double>();
        double otherTotal = 0;
        for (int index = 0; index < sorted.size(); index++) {
            Map.Entry<String, Double> entry = sorted.get(index);
            if (index < CATEGORY_LIMIT) {
                labels.add(entry.getKey());
                slices.add(entry.getValue());
            } else {
                otherTotal += entry.getValue();
            }
        }
        if (otherTotal > 0) {
            labels.add("Other");
            slices.add(otherTotal);
        }
        double[][] values = new double[1][slices.size()];
        for (int index = 0; index < slices.size(); index++) {
            values[0][index] = slices.get(index);
        }
        String title = dataset.getColumnNames().get(primaryColumn) + " pie chart";
        if (secondaryColumn >= 0) {
            title += " by " + dataset.getColumnNames().get(secondaryColumn) + " sum";
        } else {
            title += " by row count";
        }
        return new ChartData(title, labels, seriesNames, values);
    }

    private List<String> selectedCategoryValues() {
        if (secondaryColumn < 0) {
            return new ArrayList<String>();
        }
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (List<String> row : dataset.getRows()) {
            Double value = parseNumber(row.get(primaryColumn));
            String category = row.get(secondaryColumn);
            if (value == null || isMissing(category)) {
                continue;
            }
            counts.put(category, counts.containsKey(category) ? counts.get(category) + 1 : 1);
        }
        List<Map.Entry<String, Integer>> sorted =
                new ArrayList<Map.Entry<String, Integer>>(counts.entrySet());
        Collections.sort(sorted, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> first,
                    Map.Entry<String, Integer> second) {
                return Integer.compare(second.getValue(), first.getValue());
            }
        });
        List<String> names = new ArrayList<String>();
        for (int index = 0; index < sorted.size() && index < CATEGORY_LIMIT; index++) {
            names.add(sorted.get(index).getKey());
        }
        if (sorted.size() > CATEGORY_LIMIT) {
            names.add("Other");
        }
        return names;
    }

    private int seriesIndex(List<String> names, String category) {
        if (isMissing(category)) {
            return -1;
        }
        int index = names.indexOf(category);
        if (index >= 0) {
            return index;
        }
        return names.indexOf("Other");
    }

    private void drawHistogram(Graphics2D g, ChartData data) {
        int left = 60;
        int right = Math.max(left + 10, getWidth() - 20);
        int top = 48;
        int bottom = Math.max(top + 10, getHeight() - 75);
        g.setColor(GRID);
        g.drawLine(left, bottom, right, bottom);
        g.drawLine(left, top, left, bottom);

        double maximum = 1;
        for (int bin = 0; bin < data.labels.size(); bin++) {
            double total = 0;
            for (double[] values : data.values) {
                total += values[bin];
            }
            maximum = Math.max(maximum, total);
        }
        int availableWidth = right - left;
        int slotWidth = Math.max(1, availableWidth / data.labels.size());
        for (int bin = 0; bin < data.labels.size(); bin++) {
            double total = 0;
            for (double[] values : data.values) {
                total += values[bin];
            }
            int accumulatedHeight = 0;
            for (int series = 0; series < data.seriesNames.size(); series++) {
                double count = data.values[series][bin];
                int height = (int) ((bottom - top - 8) * (count / maximum));
                int x = left + bin * slotWidth + 3;
                int width = Math.max(1, slotWidth - 6);
                g.setColor(PALETTE[series % PALETTE.length]);
                g.fillRect(x, bottom - accumulatedHeight - height, width, height);
                accumulatedHeight += height;
            }
            g.setColor(TEXT);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            String label = abbreviate(data.labels.get(bin), slotWidth > 65 ? 11 : 6);
            g.drawString(label, left + bin * slotWidth + 2, bottom + 15);
            if (slotWidth > 35 && total > 0) {
                g.drawString(String.valueOf((int) total),
                        left + bin * slotWidth + 3, bottom - accumulatedHeight - 3);
            }
        }
        drawLegend(g, data.seriesNames, right, bottom + 42);
    }

    private void drawPie(Graphics2D g, ChartData data) {
        int diameter = Math.max(80, Math.min(getHeight() - 95, getWidth() / 2));
        int x = 35;
        int y = Math.max(38, (getHeight() - diameter) / 2);
        double total = 0;
        for (double value : data.values[0]) {
            total += value;
        }
        if (total <= 0) {
            g.setColor(TEXT);
            g.drawString("No positive values to display.", x, y + 25);
            return;
        }
        double accumulated = 0;
        int previousAngle = 0;
        for (int index = 0; index < data.labels.size(); index++) {
            double value = data.values[0][index];
            accumulated += value;
            int endAngle = index == data.labels.size() - 1
                    ? 360 : (int) Math.round(accumulated / total * 360);
            g.setColor(PALETTE[index % PALETTE.length]);
            g.fillArc(x, y, diameter, diameter, previousAngle, endAngle - previousAngle);
            previousAngle = endAngle;
        }

        int legendX = x + diameter + 30;
        int legendY = Math.max(55, y + 12);
        DecimalFormat percent = new DecimalFormat("0.0%");
        g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        for (int index = 0; index < data.labels.size(); index++) {
            int itemY = legendY + index * 23;
            if (itemY > getHeight() - 15) {
                break;
            }
            g.setColor(PALETTE[index % PALETTE.length]);
            g.fillRoundRect(legendX, itemY - 10, 12, 12, 3, 3);
            g.setColor(TEXT);
            g.drawString(abbreviate(data.labels.get(index), 28) + "  "
                    + percent.format(data.values[0][index] / total),
                    legendX + 20, itemY);
        }
    }

    private void drawLegend(Graphics2D g, List<String> names, int right, int bottom) {
        int itemX = 65;
        int itemY = bottom;
        g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        for (int index = 0; index < names.size(); index++) {
            int width = Math.min(150, g.getFontMetrics().stringWidth(names.get(index)) + 27);
            if (itemX + width > right) {
                itemX = 65;
                itemY += 17;
            }
            if (itemY > getHeight() - 5) {
                break;
            }
            g.setColor(PALETTE[index % PALETTE.length]);
            g.fillRect(itemX, itemY - 8, 10, 10);
            g.setColor(TEXT);
            g.drawString(abbreviate(names.get(index), 20), itemX + 15, itemY);
            itemX += width + 8;
        }
    }

    private static Double parseNumber(String value) {
        try {
            double parsed = Double.parseDouble(value.trim());
            return Double.isNaN(parsed) || Double.isInfinite(parsed) ? null : parsed;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean isMissing(String value) {
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.isEmpty() || normalized.equals("na")
                || normalized.equals("n/a") || normalized.equals("null")
                || normalized.equals("none");
    }

    private static String abbreviate(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, Math.max(1, limit - 1)) + "\u2026";
    }

    private static final class ChartData {
        private final String title;
        private final List<String> labels;
        private final List<String> seriesNames;
        private final double[][] values;

        private ChartData(String title, List<String> labels,
                List<String> seriesNames, double[][] values) {
            this.title = title;
            this.labels = labels;
            this.seriesNames = seriesNames;
            this.values = values;
        }
    }
}
