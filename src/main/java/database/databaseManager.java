package database;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    private static final String URL =
            "jdbc:mysql://localhost:3306/csv_analyser";

    private static final String USER = "root";

    private static final String PASSWORD =
            "test";

    public static Connection getConnection()
            throws SQLException {

        return DriverManager.getConnection(
                URL,
                USER,
                PASSWORD
        );
    }

    public static void insertDataset(
            String filename,
            int rowCount,
            int columnCount
    ) throws SQLException {

        String sql =
                "INSERT INTO datasets " +
                "(filename, row_count, column_count) " +
                "VALUES (?, ?, ?)";

        try (
            Connection connection = getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setString(1, filename);
            statement.setInt(2, rowCount);
            statement.setInt(3, columnCount);

            statement.executeUpdate();

            System.out.println(
                    "Dataset inserted successfully!"
            );
        }
    }
    public static List<String> getDatasets()
        throws SQLException {

    List<String> datasets = new ArrayList<>();

    String sql =
            "SELECT dataset_id, filename, row_count, column_count " +
            "FROM datasets";

    try (
        Connection connection = getConnection();

        PreparedStatement statement =
                connection.prepareStatement(sql);

        ResultSet resultSet =
                statement.executeQuery()
    ) {

        while (resultSet.next()) {

            int id =
                    resultSet.getInt("dataset_id");

            String filename =
                    resultSet.getString("filename");

            int rows =
                    resultSet.getInt("row_count");

            int columns =
                    resultSet.getInt("column_count");

            String dataset =
                    id + " | " +
                    filename + " | Rows: " +
                    rows + " | Columns: " +
                    columns;

            datasets.add(dataset);
        }
    }

    return datasets;
}
}