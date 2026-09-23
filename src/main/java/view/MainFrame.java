package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainFrame extends JFrame {

    // Main colors
    private final Color BACKGROUND = new Color(245, 247, 250);
    private final Color SIDEBAR = new Color(31, 41, 55);
    private final Color PRIMARY = new Color(37, 99, 235);
    private final Color WHITE = Color.WHITE;
    private final Color TEXT = new Color(31, 41, 55);
    private final Color SECONDARY_TEXT = new Color(107, 114, 128);

    private JLabel rowValue;
    private JLabel columnValue;
    private JLabel missingValue;

    private JTable dataTable;

    public MainFrame() {

        setTitle("CSV Analyzer");
        setSize(1200, 750);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        getContentPane().setBackground(BACKGROUND);

        createHeader();
        createSidebar();
        createMainContent();

        setVisible(true);
    }

    // ==============================
    // HEADER
    // ==============================

    private void createHeader() {

        JPanel header = new JPanel(new BorderLayout());

        header.setPreferredSize(new Dimension(1200, 75));
        header.setBackground(WHITE);

        header.setBorder(
                BorderFactory.createMatteBorder(
                        0, 0, 1, 0,
                        new Color(220, 224, 230)
                )
        );

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(
                titlePanel,
                BoxLayout.Y_AXIS
        ));

        titlePanel.setBackground(WHITE);
        titlePanel.setBorder(
                new EmptyBorder(10, 25, 10, 10)
        );

        JLabel title = new JLabel("CSV ANALYZER");

        title.setFont(
                new Font("Segoe UI",
                        Font.BOLD,
                        22)
        );

        title.setForeground(TEXT);

        JLabel subtitle =
                new JLabel("Data Analysis & Management");

        subtitle.setFont(
                new Font("Segoe UI",
                        Font.PLAIN,
                        12)
        );

        subtitle.setForeground(SECONDARY_TEXT);

        titlePanel.add(title);
        titlePanel.add(subtitle);

        header.add(titlePanel, BorderLayout.WEST);

        // Database status

        JPanel statusPanel = new JPanel(
                new FlowLayout(
                        FlowLayout.RIGHT,
                        20,
                        25
                )
        );

        statusPanel.setBackground(WHITE);

        JLabel status = new JLabel(
                "●  Database Connected"
        );

        status.setFont(
                new Font("Segoe UI",
                        Font.BOLD,
                        13)
        );

        status.setForeground(
                new Color(22, 163, 74)
        );

        statusPanel.add(status);

        header.add(
                statusPanel,
                BorderLayout.EAST
        );

        add(header, BorderLayout.NORTH);
    }

    // ==============================
    // SIDEBAR
    // ==============================

    private void createSidebar() {

        JPanel sidebar = new JPanel();

        sidebar.setPreferredSize(
                new Dimension(210, 0)
        );

        sidebar.setBackground(SIDEBAR);

        sidebar.setLayout(
                new BoxLayout(
                        sidebar,
                        BoxLayout.Y_AXIS
                )
        );

        sidebar.setBorder(
                new EmptyBorder(25, 15, 20, 15)
        );

        JLabel menuTitle =
                new JLabel("MENU");

        menuTitle.setFont(
                new Font("Segoe UI",
                        Font.BOLD,
                        11)
        );

        menuTitle.setForeground(
                new Color(156, 163, 175)
        );

        menuTitle.setBorder(
                new EmptyBorder(0, 10, 15, 0)
        );

        sidebar.add(menuTitle);

        sidebar.add(createMenuButton(
                "📁   Import CSV"
        ));

        sidebar.add(createMenuButton(
                "📋   Data Preview"
        ));

        sidebar.add(createMenuButton(
                "📊   Analysis"
        ));

        sidebar.add(createMenuButton(
                "📈   Charts"
        ));

        JButton databaseButton =
        createMenuButton("🗄   Database");

        databaseButton.addActionListener(
                e -> showDatabaseRecords()
        );

sidebar.add(databaseButton);

        sidebar.add(Box.createVerticalGlue());

        sidebar.add(createMenuButton(
                "⚙   Settings"
        ));

        add(sidebar, BorderLayout.WEST);
    }

    private JButton createMenuButton(String text) {

        JButton button = new JButton(text);

        button.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        45
                )
        );

        button.setHorizontalAlignment(
                SwingConstants.LEFT
        );

        button.setForeground(
                new Color(229, 231, 235)
        );

        button.setBackground(SIDEBAR);

        button.setFont(
                new Font("Segoe UI",
                        Font.PLAIN,
                        14)
        );

        button.setBorder(
                new EmptyBorder(10, 12, 10, 10)
        );

        button.setFocusPainted(false);

        button.setCursor(
                new Cursor(Cursor.HAND_CURSOR)
        );

        return button;
        
    }

    // ==============================
    // MAIN CONTENT
    // ==============================

    private void createMainContent() {

        JPanel mainPanel = new JPanel(
                new BorderLayout()
        );

        mainPanel.setBackground(BACKGROUND);

        mainPanel.setBorder(
                new EmptyBorder(
                        25,
                        25,
                        25,
                        25
                )
        );

        JPanel content = new JPanel();

        content.setBackground(BACKGROUND);

        content.setLayout(
                new BoxLayout(
                        content,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel welcome =
                new JLabel(
                        "Welcome to CSV Analyzer"
                );

        welcome.setFont(
                new Font("Segoe UI",
                        Font.BOLD,
                        25)
        );

        welcome.setForeground(TEXT);

        welcome.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        content.add(welcome);

        JLabel description =
                new JLabel(
                        "Import a CSV file to begin analyzing your data."
                );

        description.setFont(
                new Font("Segoe UI",
                        Font.PLAIN,
                        14)
        );

        description.setForeground(
                SECONDARY_TEXT
        );

        description.setBorder(
                new EmptyBorder(5, 0, 20, 0)
        );

        description.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        content.add(description);

        // Statistics cards

        JPanel cards = new JPanel(
                new GridLayout(
                        1,
                        3,
                        15,
                        0
                )
        );

        cards.setBackground(BACKGROUND);

        cards.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        110
                )
        );

        cards.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        rowValue = new JLabel("0");

        columnValue = new JLabel("0");

        missingValue = new JLabel("0");

        cards.add(createCard(
                "ROWS",
                rowValue
        ));

        cards.add(createCard(
                "COLUMNS",
                columnValue
        ));

        cards.add(createCard(
                "MISSING VALUES",
                missingValue
        ));

        content.add(cards);

        content.add(
                Box.createVerticalStrut(25)
        );

        // Dataset preview title

        JLabel previewTitle =
                new JLabel("Dataset Preview");

        previewTitle.setFont(
                new Font("Segoe UI",
                        Font.BOLD,
                        18)
        );

        previewTitle.setForeground(TEXT);

        previewTitle.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        content.add(previewTitle);

        content.add(
                Box.createVerticalStrut(10)
        );

        // Table

        String[] columns = {
                "Column 1",
                "Column 2",
                "Column 3"
        };

        Object[][] data = {
                {
                        "No dataset",
                        "loaded",
                        "yet"
                }
        };

        dataTable = new JTable(
                data,
                columns
        );

        dataTable.setRowHeight(35);

        dataTable.setFont(
                new Font("Segoe UI",
                        Font.PLAIN,
                        13)
        );

        dataTable.getTableHeader()
                .setFont(
                        new Font("Segoe UI",
                                Font.BOLD,
                                13)
                );

        JScrollPane scrollPane =
                new JScrollPane(dataTable);

        scrollPane.setPreferredSize(
                new Dimension(800, 220)
        );

        scrollPane.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        content.add(scrollPane);

        content.add(
                Box.createVerticalStrut(20)
        );

        // Buttons

        JPanel actionPanel = new JPanel(
                new FlowLayout(
                        FlowLayout.LEFT,
                        10,
                        0
                )
        );

        actionPanel.setBackground(BACKGROUND);

        actionPanel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        JButton importButton =
                new JButton("Import CSV");

        JButton analyzeButton =
                new JButton("Analyze Dataset");
        
        importButton.addActionListener(e -> openCSVFile());

        stylePrimaryButton(importButton);

        styleSecondaryButton(analyzeButton);

        actionPanel.add(importButton);
        actionPanel.add(analyzeButton);

        content.add(actionPanel);

        mainPanel.add(
                content,
                BorderLayout.CENTER
        );

        add(mainPanel, BorderLayout.CENTER);
    }

    // ==============================
    // STATISTIC CARD
    // ==============================

    private JPanel createCard(
            String title,
            JLabel value
    ) {

        JPanel card = new JPanel();

        card.setLayout(
                new BoxLayout(
                        card,
                        BoxLayout.Y_AXIS
                )
        );

        card.setBackground(WHITE);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(225, 228, 233)
                        ),
                        new EmptyBorder(
                                15,
                                20,
                                15,
                                20
                        )
                )
        );

        JLabel titleLabel =
                new JLabel(title);

        titleLabel.setFont(
                new Font("Segoe UI",
                        Font.BOLD,
                        11)
        );

        titleLabel.setForeground(
                SECONDARY_TEXT
        );

        value.setFont(
                new Font("Segoe UI",
                        Font.BOLD,
                        28)
        );

        value.setForeground(PRIMARY);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(value);

        return card;
    }

    // ==============================
    // BUTTON STYLES
    // ==============================

    private void stylePrimaryButton(
            JButton button
    ) {

        button.setBackground(PRIMARY);
        button.setForeground(WHITE);

        button.setFont(
                new Font("Segoe UI",
                        Font.BOLD,
                        13)
        );

        button.setFocusPainted(false);

        button.setBorder(
                new EmptyBorder(
                        12,
                        22,
                        12,
                        22
                )
        );
    }
    private void openCSVFile() {

    JFileChooser fileChooser = new JFileChooser();

    fileChooser.setDialogTitle("Select CSV File");

    int result = fileChooser.showOpenDialog(this);

    if (result == JFileChooser.APPROVE_OPTION) {

        java.io.File selectedFile =
                fileChooser.getSelectedFile();

        JOptionPane.showMessageDialog(
                this,
                "Selected file:\n" +
                selectedFile.getAbsolutePath(),
                "CSV Selected",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}

    private void styleSecondaryButton(
            JButton button
    ) {

        button.setBackground(WHITE);
        button.setForeground(TEXT);

        button.setFont(
                new Font("Segoe UI",
                        Font.BOLD,
                        13)
        );

        button.setFocusPainted(false);

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(210, 214, 220)
                        ),
                        new EmptyBorder(
                                11,
                                21,
                                11,
                                21
                        )
                )
        );
    }
    private void showDatabaseRecords() {

    try {

        java.util.List<String> datasets =
                database.DatabaseManager.getDatasets();

        if (datasets.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "No datasets found in the database.",
                    "Database",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        StringBuilder message =
                new StringBuilder();

        message.append(
                "Datasets stored in MySQL:\n\n"
        );

        for (String dataset : datasets) {

            message.append(dataset);
            message.append("\n");
        }

        JOptionPane.showMessageDialog(
                this,
                message.toString(),
                "Database Records",
                JOptionPane.INFORMATION_MESSAGE
        );

    } catch (Exception e) {

        JOptionPane.showMessageDialog(
                this,
                "Database error:\n" +
                e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
}