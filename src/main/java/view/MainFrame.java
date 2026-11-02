package view;

import analysis.CsvDataset;
import analysis.DistributionChart;
import analysis.DistributionChart.ChartType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainFrame extends JFrame {
    private static final Color BACKGROUND = new Color(245, 247, 250);
    private static final Color SIDEBAR = new Color(31, 41, 55);
    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color TEXT = new Color(31, 41, 55);
    private static final Color SECONDARY_TEXT = new Color(107, 114, 128);
    private static final int PREVIEW_ROW_LIMIT = 1000;

    private final JLabel rowValue = new JLabel("0");
    private final JLabel columnValue = new JLabel("0");
    private final JLabel missingValue = new JLabel("0");
    private final JLabel duplicateValue = new JLabel("0");
    private final JLabel fileLabel = new JLabel("No dataset loaded");
    private final JButton importButton = new JButton("Import CSV");
    private final JTable dataTable = new JTable();
    private final JTextArea analysisText = new JTextArea();
    private final JComboBox<String> chartType = new JComboBox<String>(
            new String[] { "Histogram", "Pie chart" });
    private final JComboBox<String> chartPrimaryColumn = new JComboBox<String>();
    private final JComboBox<String> chartSecondaryColumn = new JComboBox<String>();
    private final JLabel chartPrimaryLabel = new JLabel("Numeric column");
    private final JLabel chartSecondaryLabel = new JLabel("Group by (optional)");
    private final JLabel chartHint = new JLabel(
            "Histogram: numeric distribution, optionally grouped by a category. "
                    + "Pie: category shares by count or numeric-value sum.");
    private final DistributionChart distributionChart = new DistributionChart();
    private final JTabbedPane tabs = new JTabbedPane();
    private CsvDataset dataset;
    private boolean updatingChartControls;

    public MainFrame() {
        setTitle("CSV Analyzer");
        setSize(1250, 800);
        setMinimumSize(new Dimension(950, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout());

        add(createHeader(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);
        add(createContent(), BorderLayout.CENTER);
        setVisible(true);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 224, 230)),
                new EmptyBorder(10, 24, 10, 20)));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("CSV ANALYZER");
        title.setFont(new Font("Segoe UI", Font.BOLD, 21));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel("Explore data, distributions, and relationships");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(SECONDARY_TEXT);
        titlePanel.add(title);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        importButton.addActionListener(event -> openCSVFile());
        stylePrimaryButton(importButton);
        header.add(importButton, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(205, 0));
        sidebar.setBackground(SIDEBAR);
        sidebar.setBorder(new EmptyBorder(24, 14, 20, 14));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        JLabel menuTitle = new JLabel("WORKSPACE");
        menuTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        menuTitle.setForeground(new Color(156, 163, 175));
        menuTitle.setBorder(new EmptyBorder(0, 10, 14, 0));
        sidebar.add(menuTitle);
        sidebar.add(createMenuButton("Dashboard", 0));
        sidebar.add(createMenuButton("Analysis", 1));
        sidebar.add(createMenuButton("Charts", 2));
        sidebar.add(Box.createVerticalGlue());
        JButton databaseButton = createMenuButton("Saved datasets", -1);
        databaseButton.addActionListener(event -> showDatabaseRecords());
        sidebar.add(databaseButton);
        return sidebar;
    }

    private JButton createMenuButton(String text, final int tabIndex) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setForeground(new Color(229, 231, 235));
        button.setBackground(SIDEBAR);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setBorder(new EmptyBorder(10, 12, 10, 10));
        button.setFocusPainted(false);
        button.addActionListener(event -> {
            if (tabIndex >= 0) {
                tabs.setSelectedIndex(tabIndex);
            }
        });
        return button;
    }

    private JPanel createContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BACKGROUND);
        content.setBorder(new EmptyBorder(18, 22, 18, 22));
        tabs.addTab("Dashboard", createDashboard());
        tabs.addTab("Analysis", createAnalysisPanel());
        tabs.addTab("Charts", createChartsPanel());
        content.add(tabs, BorderLayout.CENTER);
        return content;
    }

    private JPanel createDashboard() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(8, 4, 8, 4));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("Dataset overview");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 24));
        heading.setForeground(TEXT);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        top.add(heading);
        fileLabel.setForeground(SECONDARY_TEXT);
        fileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fileLabel.setBorder(new EmptyBorder(4, 0, 14, 0));
        fileLabel.setAlignmentX(LEFT_ALIGNMENT);
        top.add(fileLabel);

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 102));
        cards.add(createCard("ROWS", rowValue));
        cards.add(createCard("COLUMNS", columnValue));
        cards.add(createCard("MISSING VALUES", missingValue));
        cards.add(createCard("DUPLICATE ROWS", duplicateValue));
        cards.setAlignmentX(LEFT_ALIGNMENT);
        top.add(cards);
        panel.add(top, BorderLayout.NORTH);

        JPanel preview = new JPanel(new BorderLayout(0, 8));
        preview.setBackground(Color.WHITE);
        preview.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 228, 233)),
                new EmptyBorder(12, 14, 12, 14)));
        JLabel previewTitle = new JLabel("Data preview");
        previewTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        previewTitle.setForeground(TEXT);
        preview.add(previewTitle, BorderLayout.NORTH);
        dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        dataTable.setRowHeight(27);
        dataTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dataTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        preview.add(new JScrollPane(dataTable), BorderLayout.CENTER);
        panel.add(preview, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAnalysisPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel heading = new JLabel("Automated analysis");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        heading.setForeground(TEXT);
        heading.setBorder(new EmptyBorder(0, 0, 12, 0));
        panel.add(heading, BorderLayout.NORTH);
        analysisText.setEditable(false);
        analysisText.setFont(new Font("Consolas", Font.PLAIN, 13));
        analysisText.setForeground(TEXT);
        analysisText.setText("Import a CSV file to see column profiles, missing values, "
                + "distributions, and numeric relationships.");
        panel.add(new JScrollPane(analysisText), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createChartsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));
        JPanel toolbar = new JPanel(new BorderLayout(0, 12));
        toolbar.setOpaque(false);
        JLabel heading = new JLabel("Column distribution");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        heading.setForeground(TEXT);
        toolbar.add(heading, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        controls.setOpaque(false);
        chartType.setPreferredSize(new Dimension(145, 30));
        chartPrimaryColumn.setPreferredSize(new Dimension(190, 30));
        chartSecondaryColumn.setPreferredSize(new Dimension(205, 30));
        controls.add(createChartControl("Visualization", chartType));
        controls.add(createChartControl(chartPrimaryLabel, chartPrimaryColumn));
        controls.add(createChartControl(chartSecondaryLabel, chartSecondaryColumn));
        toolbar.add(controls, BorderLayout.CENTER);

        chartType.addActionListener(event -> refreshChartControls());
        chartPrimaryColumn.addActionListener(event -> {
            if (!updatingChartControls) {
                refreshSecondaryColumns();
                updateChart();
            }
        });
        chartSecondaryColumn.addActionListener(event -> {
            if (!updatingChartControls) {
                updateChart();
            }
        });
        chartHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        chartHint.setForeground(SECONDARY_TEXT);
        toolbar.add(chartHint, BorderLayout.SOUTH);
        panel.add(toolbar, BorderLayout.NORTH);
        distributionChart.setBorder(BorderFactory.createLineBorder(new Color(225, 228, 233)));
        panel.add(distributionChart, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createChartControl(String label, JComboBox<String> selector) {
        return createChartControl(new JLabel(label), selector);
    }

    private JPanel createChartControl(JLabel label, JComboBox<String> selector) {
        JPanel control = new JPanel();
        control.setOpaque(false);
        control.setLayout(new BoxLayout(control, BoxLayout.Y_AXIS));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(SECONDARY_TEXT);
        label.setBorder(new EmptyBorder(0, 0, 3, 0));
        control.add(label);
        control.add(selector);
        return control;
    }

    private JPanel createCard(String title, JLabel value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 228, 233)),
                new EmptyBorder(12, 15, 12, 12)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        titleLabel.setForeground(SECONDARY_TEXT);
        value.setFont(new Font("Segoe UI", Font.BOLD, 26));
        value.setForeground(PRIMARY);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(value);
        return card;
    }

    private void stylePrimaryButton(JButton button) {
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
    }

    private void openCSVFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a CSV file");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "CSV and text files (*.csv, *.tsv, *.txt)", "csv", "tsv", "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadFile(chooser.getSelectedFile());
        }
    }

    private void loadFile(final File file) {
        importButton.setEnabled(false);
        importButton.setText("Loading...");
        new SwingWorker<CsvDataset, Void>() {
            @Override
            protected CsvDataset doInBackground() throws Exception {
                return CsvDataset.load(file.toPath());
            }

            @Override
            protected void done() {
                importButton.setEnabled(true);
                importButton.setText("Import CSV");
                try {
                    dataset = get();
                    updateDatasetViews();
                    tabs.setSelectedIndex(0);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("CSV loading was interrupted.", exception);
                } catch (ExecutionException exception) {
                    showError("Could not read the selected CSV file.",
                            exception.getCause() == null ? exception : exception.getCause());
                }
            }
        }.execute();
    }

    private void updateDatasetViews() {
        rowValue.setText(String.valueOf(dataset.getRows().size()));
        columnValue.setText(String.valueOf(dataset.getColumnNames().size()));
        missingValue.setText(String.valueOf(dataset.getMissingValueCount()));
        duplicateValue.setText(String.valueOf(dataset.getDuplicateRowCount()));
        fileLabel.setText(dataset.getFilename() + "  |  " + dataset.getRows().size()
                + " rows loaded" + (dataset.getRows().size() > PREVIEW_ROW_LIMIT
                ? " (showing first " + PREVIEW_ROW_LIMIT + " in preview)" : ""));

        int previewSize = Math.min(dataset.getRows().size(), PREVIEW_ROW_LIMIT);
        Object[][] previewRows = new Object[previewSize][dataset.getColumnNames().size()];
        for (int row = 0; row < previewSize; row++) {
            previewRows[row] = dataset.getRows().get(row).toArray();
        }
        dataTable.setModel(new DefaultTableModel(previewRows,
                dataset.getColumnNames().toArray()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        updateAnalysis();
        refreshChartControls();
    }

    private void refreshChartControls() {
        if (dataset == null) {
            return;
        }
        boolean histogram = chartType.getSelectedIndex() == 0;
        chartPrimaryLabel.setText(histogram ? "Numeric column" : "Category column");
        chartSecondaryLabel.setText(histogram
                ? "Group by category (optional)" : "Sum numeric values (optional)");
        chartHint.setText(histogram
                ? "Shows numeric value counts in 10 bins; optionally split by a category."
                : "Shows category shares by row count or numeric sum. Missing/negative weights are excluded.");

        updatingChartControls = true;
        String oldPrimary = (String) chartPrimaryColumn.getSelectedItem();
        chartPrimaryColumn.removeAllItems();
        for (int column = 0; column < dataset.getColumnNames().size(); column++) {
            if (!histogram || dataset.getSummaries().get(column).isNumeric()) {
                chartPrimaryColumn.addItem(dataset.getColumnNames().get(column));
            }
        }
        if (oldPrimary != null && containsItem(chartPrimaryColumn, oldPrimary)) {
            chartPrimaryColumn.setSelectedItem(oldPrimary);
        }
        updatingChartControls = false;
        refreshSecondaryColumns();
        updateChart();
    }

    private void refreshSecondaryColumns() {
        if (dataset == null) {
            return;
        }
        boolean histogram = chartType.getSelectedIndex() == 0;
        String oldSecondary = (String) chartSecondaryColumn.getSelectedItem();
        String primary = (String) chartPrimaryColumn.getSelectedItem();
        updatingChartControls = true;
        chartSecondaryColumn.removeAllItems();
        chartSecondaryColumn.addItem("(None)");
        for (int column = 0; column < dataset.getColumnNames().size(); column++) {
            String name = dataset.getColumnNames().get(column);
            if (name.equals(primary)) {
                continue;
            }
            boolean eligible = histogram
                    ? !dataset.getSummaries().get(column).isNumeric()
                    : dataset.getSummaries().get(column).isNumeric();
            if (eligible) {
                chartSecondaryColumn.addItem(name);
            }
        }
        if (oldSecondary != null && containsItem(chartSecondaryColumn, oldSecondary)) {
            chartSecondaryColumn.setSelectedItem(oldSecondary);
        }
        updatingChartControls = false;
    }

    private boolean containsItem(JComboBox<String> selector, String value) {
        for (int index = 0; index < selector.getItemCount(); index++) {
            if (selector.getItemAt(index).equals(value)) {
                return true;
            }
        }
        return false;
    }

    private void updateAnalysis() {
        StringBuilder report = new StringBuilder();
        report.append("DATASET SUMMARY\n");
        report.append("File: ").append(dataset.getFilename()).append('\n');
        report.append("Rows: ").append(dataset.getRows().size())
                .append("    Columns: ").append(dataset.getColumnNames().size())
                .append("    Missing cells: ").append(dataset.getMissingValueCount())
                .append("    Duplicate rows: ").append(dataset.getDuplicateRowCount())
                .append("\n\nCOLUMN PROFILES\n");

        DecimalFormat numbers = new DecimalFormat("0.####");
        for (int index = 0; index < dataset.getColumnNames().size(); index++) {
            CsvDataset.ColumnSummary summary = dataset.getSummaries().get(index);
            int present = dataset.getRows().size() - summary.getMissingCount();
            double missingPercent = dataset.getRows().isEmpty() ? 0
                    : summary.getMissingCount() * 100.0 / dataset.getRows().size();
            report.append("\n").append(dataset.getColumnNames().get(index))
                    .append(" [").append(summary.isNumeric() ? "numeric" : "categorical")
                    .append("]\n");
            report.append("  Non-missing: ").append(present)
                    .append("   Missing: ").append(summary.getMissingCount())
                    .append(" (").append(numbers.format(missingPercent)).append("%)")
                    .append("   Distinct: ").append(summary.getDistinctCount()).append('\n');
            if (summary.isNumeric()) {
                report.append("  Mean: ").append(numbers.format(summary.getMean()))
                        .append("   Median: ").append(numbers.format(summary.getMedian()))
                        .append("   Std dev: ").append(numbers.format(summary.getStandardDeviation()))
                        .append("   Min: ").append(numbers.format(summary.getMinimum()))
                        .append("   Max: ").append(numbers.format(summary.getMaximum()))
                        .append('\n');
            } else if (!summary.getTopValues().isEmpty()) {
                report.append("  Most common: ");
                for (Map.Entry<String, Integer> entry : summary.getTopValues()) {
                    report.append(entry.getKey()).append(" (").append(entry.getValue())
                            .append("), ");
                }
                report.setLength(report.length() - 2);
                report.append('\n');
            }
        }

        report.append("\nNUMERIC RELATIONSHIPS (Pearson correlation)\n");
        List<CsvDataset.Relationship> relationships = dataset.getNumericRelationships();
        if (relationships.isEmpty()) {
            report.append("No variable pairs with a defined numeric correlation were found.\n");
        } else {
            for (CsvDataset.Relationship relationship : relationships) {
                report.append("  ").append(relationship.getFirstColumn()).append(" <-> ")
                        .append(relationship.getSecondColumn()).append(": r = ")
                        .append(numbers.format(relationship.getCorrelation()))
                        .append(" (n=").append(relationship.getSampleSize()).append(")\n");
            }
            report.append("Correlation indicates linear association, not causation.\n");
        }
        analysisText.setText(report.toString());
        analysisText.setCaretPosition(0);
    }

    private void updateChart() {
        if (dataset == null) {
            return;
        }
        String primary = (String) chartPrimaryColumn.getSelectedItem();
        String secondary = (String) chartSecondaryColumn.getSelectedItem();
        int primaryIndex = primary == null ? -1 : dataset.getColumnNames().indexOf(primary);
        int secondaryIndex = secondary == null || secondary.equals("(None)")
                ? -1 : dataset.getColumnNames().indexOf(secondary);
        ChartType selectedType = chartType.getSelectedIndex() == 1
                ? ChartType.PIE : ChartType.HISTOGRAM;
        distributionChart.setData(dataset, selectedType, primaryIndex, secondaryIndex);
    }

    private void showDatabaseRecords() {
        try {
            List<String> datasets = database.databaseManager.getDatasets();
            if (datasets.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No saved datasets were found.",
                        "Saved datasets", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            StringBuilder message = new StringBuilder("Datasets stored in MySQL:\n\n");
            for (String storedDataset : datasets) {
                message.append(storedDataset).append('\n');
            }
            JOptionPane.showMessageDialog(this, message.toString(),
                    "Saved datasets", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException exception) {
            showError("Could not load saved datasets. Check the optional MySQL settings.",
                    exception);
        }
    }

    private void showError(String message, Throwable exception) {
        String details = exception.getMessage();
        JOptionPane.showMessageDialog(this,
                message + (details == null ? "" : "\n\n" + details),
                "CSV Analyzer", JOptionPane.ERROR_MESSAGE);
    }
}
