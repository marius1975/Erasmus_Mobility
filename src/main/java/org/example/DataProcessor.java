

package org.example;

import org.apache.hadoop.shaded.com.google.gson.Gson;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.util.*;


public class DataProcessor {

    private static SparkSession spark;

    public DataProcessor() {
        // Create a Spark session when the DataProcessor is initialized
        spark = SparkSession.builder()
                .appName("Erasmus")
                .master("local[*]")
                .getOrCreate();
    }

    // Close the Spark session when the DataProcessor is no longer needed
    public void close() {
        spark.stop();
    }


    public void registerUser(String username, String password) {
        // Store user information in the erasmus_user_registration database
        String jdbcUrl = "jdbc:mysql://localhost:3306/erasmus_user_registration";
        String dbUsername = "root";
        String dbPassword = loadPasswordFromConfigFile(); // Replace with your database password

        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword)) {
            String query = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, password);
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle database-related errors
        }
    }

    // Authenticate user by checking against the database
    public boolean authenticateUser(String username, String password) {
        String jdbcUrl = "jdbc:mysql://localhost:3306/erasmus_user_registration";
        String dbUsername = "root";
        String dbPassword = loadPasswordFromConfigFile(); // Replace with your database password

        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword)) {
            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, password);

            ResultSet resultSet = statement.executeQuery();
            return resultSet.next(); // User found in the database
        } catch (Exception e) {
            e.printStackTrace();
            // Handle database-related errors
            return false;
        }
    }

    private static Connection createConnectionToDatabase() throws SQLException {
        String jdbcUrl = "jdbc:mysql://localhost:3306/erasmus_data";
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", "root");
        connectionProperties.setProperty("password", loadPasswordFromConfigFile());
        return DriverManager.getConnection(jdbcUrl, connectionProperties);
    }


    // Helper method to establish a connection to the erasmus_mobility database
    private static Connection createConnectionToErasmusMobilityDatabase() throws SQLException {
        String jdbcUrl = "jdbc:mysql://localhost:3306/erasmus_mobility"; // Update the database name
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", "root");
        connectionProperties.setProperty("password", loadPasswordFromConfigFile());

        return DriverManager.getConnection(jdbcUrl, connectionProperties);
    }


    public String orderByReceivingCountry(String loggedInUserId, String selectedTableName, String userInput) throws SQLException {
        List<Map<String, String>> tableData = new ArrayList<>();

        // Check if loggedInUserId, tableName, or userInput are null or empty
        if (loggedInUserId == null || loggedInUserId.isEmpty() || selectedTableName == null || selectedTableName.isEmpty() || userInput == null || userInput.isEmpty()) {
            // Handle the case where loggedInUserId, tableName, or userInput is null or empty
            return "Invalid user ID, table name, or input"; // You can return an error message or handle it as needed.
        }

        try (Connection connection = createConnectionToDatabase()) {
            // Prepare and execute a query to fetch grouped and aggregated data from the specified table
            String query = "SELECT user_id, `Receiving Country Code`, `Sending Country Code`, COUNT(`Participant Age`) AS `Number of Participants`" +
                    " FROM " + selectedTableName +
                    " WHERE user_id = ? AND `Receiving Country Code` IN (";

            // Split the userInput on spaces to get individual country codes
            String[] countryCodes = userInput.split(" ");

            // Create a comma-separated string for the IN clause
            String countryCodeList = String.join(",", Collections.nCopies(countryCodes.length, "?"));
            query += countryCodeList + ")" +
                    " GROUP BY user_id, `Receiving Country Code`, `Sending Country Code`" +
                    " ORDER BY user_id, `Receiving Country Code`, `Sending Country Code`";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, loggedInUserId);

                // Set the individual country codes as parameters
                for (int i = 0; i < countryCodes.length; i++) {
                    preparedStatement.setString(i + 2, countryCodes[i].trim()); // Trim and add the code
                }

                ResultSet resultSet = preparedStatement.executeQuery();

                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (resultSet.next()) {
                    Map<String, String> rowData = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        String columnValue = resultSet.getString(i);
                        rowData.put(columnName, columnValue);
                    }
                    tableData.add(rowData);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle any exceptions here
        }

        // Convert the list of table data to a JSON string
        Gson gson = new Gson();
        String jsonData = gson.toJson(tableData);

        return jsonData;
    }



    public String orderByParticipantAge(String loggedInUserID, String selectedTableName, String participantsAge) throws SQLException {
        List<Map<String, String>> tableData = new ArrayList<>();

        // Create a database connection
        try (Connection connection = createConnectionToDatabase()) {
            // Use PreparedStatement to safely include the table name in the SQL query
            String query = "SELECT user_id, `Participant Age`, `Receiving Country Code`, `Sending Country Code`, COUNT(*) as `Number of Participants` FROM " + selectedTableName + " WHERE user_id = ? AND `Participant Age` IN (";

            // Split the userInput on spaces to get individual age values
            String[] ageValues = participantsAge.split(" ");

            // Create a comma-separated string for the IN clause
            String ageList = String.join(",", Collections.nCopies(ageValues.length, "?"));
            query += ageList + ")";

            // Group by the specified columns and calculate the count
            query += " GROUP BY user_id, `Participant Age`, `Receiving Country Code`, `Sending Country Code`";

            // Create and execute the prepared statement
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, loggedInUserID);

                // Set the individual age values as parameters
                for (int i = 0; i < ageValues.length; i++) {
                    preparedStatement.setString(i + 2, ageValues[i].trim()); // Trim and add the age value
                }

                ResultSet resultSet = preparedStatement.executeQuery();

                while (resultSet.next()) {
                    Map<String, String> rowData = new HashMap<>();
                    rowData.put("user_id", resultSet.getString("user_id"));
                    rowData.put("Participant Age", resultSet.getString("Participant Age"));
                    rowData.put("Receiving Country Code", resultSet.getString("Receiving Country Code"));
                    rowData.put("Sending Country Code", resultSet.getString("Sending Country Code"));
                    rowData.put("Number of Participants", resultSet.getString("Number of Participants"));
                    tableData.add(rowData);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle any exceptions here
        }

        // Convert the list of table data to a JSON string
        Gson gson = new Gson();
        String jsonData = gson.toJson(tableData);

        return jsonData;
    }



    public List<String> saveToDatabase(String loggedInUserId, String selectedTableName, String userInput) {
        String[] receivingCountryCodes = userInput.split(" ");
        List<String> results = new ArrayList<>();

        // Check if selectedTableName, loggedInUserId, or userInput is null or empty
        if (selectedTableName == null || selectedTableName.isEmpty() || loggedInUserId == null || loggedInUserId.isEmpty() || userInput == null || userInput.isEmpty()) {
            // Handle the case where selectedTableName, loggedInUserId, or userInput is null or empty
            results.add("Invalid table name, user ID, or input");
            return results;
        }

        try (Connection connection = createConnectionToDatabase()) {
            // Create a DataFrameReader
            DataFrameReader reader = spark.read();
            reader.option("header", "true");

            // Create connection to the database
            String jdbcUrl = "jdbc:mysql://localhost:3306/erasmus_data";
            Properties connectionProperties = new Properties();
            connectionProperties.setProperty("user", "root");
            connectionProperties.setProperty("password", loadPasswordFromConfigFile());

            // Read the selected table
            Dataset<Row> df = reader.jdbc(jdbcUrl, selectedTableName,connectionProperties);

            // Filter data for the logged-in user
            df = df.filter(functions.col("user_id").equalTo(loggedInUserId));

            // Filter the data by the country codes
            df = df.filter(functions.col("Receiving Country Code").isin(receivingCountryCodes));

            // Group and aggregate data
            Dataset<Row> groupedDataToBeSaved = df.groupBy("user_id", "Receiving Country Code", "Sending Country Code")
                    .agg(functions.count("Participant Age").alias("Number of Participants"))
                    .orderBy("user_id", "Receiving Country Code", "Sending Country Code");

            // Save the filtered data to the database for each country
            String jdbcUrl_2 = "jdbc:mysql://localhost:3306/erasmus_mobility";
            for (String country : receivingCountryCodes) {
                // Create a table name based on the country
                String tableName = country.toLowerCase() + "_table";

                // Filter data for the current country
                Dataset<Row> dataForCountry = groupedDataToBeSaved
                        .filter(functions.col("Receiving Country Code").equalTo(country))
                        .drop("Receiving Country Code");

                // Save data to the corresponding table
                dataForCountry.write()
                        .mode(SaveMode.Overwrite)
                        .jdbc(jdbcUrl_2, tableName, connectionProperties);

                // Collect and format data for the current country
                List<String> dataAsJson = dataForCountry.toJSON().collectAsList();
                results.add("{\"country\": \"" + country + "\", \"data\": " + dataAsJson.toString() + "}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle any exceptions here
            results.add("Error while saving data: " + e.getMessage());
        }

        return results;
    }


   public List<String> saveToDatabaseByAge(String loggedInUserID, String selectedTableName, String participantsAge) {
       List<String> results = new ArrayList<>();

       // Split the age input into individual age values
       String[] ageValues = participantsAge.split(" ");

       // Check if selectedTableName, loggedInUserId, or participantsAge is null or empty
       if (selectedTableName == null || selectedTableName.isEmpty() || loggedInUserID == null || loggedInUserID.isEmpty() || participantsAge == null || participantsAge.isEmpty()) {
           // Handle the case where tableName, loggedInUserId, or participantsAge is null or empty
           results.add("Invalid table name, user ID, or age input");
           return results;
       }

       try (Connection connection = createConnectionToDatabase()) {
           // Create a DataFrameReader
           DataFrameReader reader = spark.read();
           reader.option("header", "true");

           // Create connection to the database
           String jdbcUrl = "jdbc:mysql://localhost:3306/erasmus_data";
           Properties connectionProperties = new Properties();
           connectionProperties.setProperty("user", "root");
           connectionProperties.setProperty("password", loadPasswordFromConfigFile());

           // Read the selected table
           Dataset<Row> df = reader.jdbc(jdbcUrl, selectedTableName, connectionProperties);

           // Filter data for the logged-in user
           df = df.filter(functions.col("user_id").equalTo(loggedInUserID));

           for (String age : ageValues) {
               // Filter the data by age
               Dataset<Row> ageFilteredData = df.filter(functions.col("Participant Age").equalTo(age.trim()));

               // Group and aggregate data for the current age
               Dataset<Row> groupedDataToBeSaved = ageFilteredData.groupBy("user_id", "Receiving Country Code", "Sending Country Code")
                       .agg(functions.count("Participant Age").alias("Number of Participants"))
                       .orderBy("user_id", "Receiving Country Code", "Sending Country Code");

               // Save the filtered data to the database for the current age group
               String jdbcUrl_2 = "jdbc:mysql://localhost:3306/erasmus_mobility";
               // Create a table name based on the age
               String ageTableName = selectedTableName + "_age_" + age.trim();

               // Filter data for the current age group
               Dataset<Row> dataForAgeGroup = groupedDataToBeSaved.drop("Participant Age");

               // Save data to the corresponding table
               dataForAgeGroup.write()
                       .mode(SaveMode.Overwrite)
                       .jdbc(jdbcUrl_2, ageTableName, connectionProperties);

               // Collect and format data for the current age group
               List<String> dataAsJson = dataForAgeGroup.toJSON().collectAsList();
               results.add("{\"age\": \"" + age + "\", \"data\": " + dataAsJson.toString() + "}");
           }
       } catch (SQLException e) {
           e.printStackTrace();
           // Handle any exceptions here
           results.add("Error while saving data: " + e.getMessage());
       }

       return results;
   }



   /* public String aggregateParticipantsBySendingCountry(String loggedInUserId) {
        String jdbcUrl = "jdbc:mysql://localhost:3306/erasmus_mobility";
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", "root");
        connectionProperties.setProperty("password", loadPasswordFromConfigFile());

        List<String> tableNames = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, connectionProperties)) {
            // Filter data for the logged-in user
            String query = "SELECT table_name FROM information_schema.TABLES WHERE table_schema = 'erasmus_mobility'";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                tableNames.add(resultSet.getString("table_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error occurred while retrieving table names.";
        }

        List<Dataset<Row>> datasets = new ArrayList<>();

        for (String tableName : tableNames) {
            Dataset<Row> df = spark.read().jdbc(jdbcUrl, tableName, connectionProperties);

            // Filter data for the logged-in user and table
            df = df.filter(functions.col("user_id").equalTo(loggedInUserId));

            datasets.add(df);
        }

        if (datasets.isEmpty()) {
            return "No data found for the logged-in user.";
        }

        Dataset<Row> aggregatedData = datasets.stream()
                .reduce((df1, df2) -> df1.union(df2))
                .orElseThrow(() -> new IllegalStateException("No data found"))
                .groupBy("user_id", "Sending Country Code")
                .agg(functions.sum("Number of Participants").alias("Total Participants"))
                .orderBy("user_id", "Sending Country Code");

        // Convert the aggregatedData to JSON format
        List<String> results = aggregatedData.toJSON().collectAsList();

        // Return the JSON-formatted results
        return results.toString();
    }*/



    public String participantsBySendingCountryGraph(String loggedInUserId, String selectedTableName) {
        String jdbcUrl = "jdbc:mysql://localhost:3306/erasmus_data"; // Point to erasmus_data database
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", "root");
        connectionProperties.setProperty("password", loadPasswordFromConfigFile());

        List<Dataset<Row>> datasets = new ArrayList<>();

        try (Connection connection = createConnectionToDatabase()) {
            // Check if the selected table exists
            String checkQuery = "SELECT COUNT(*) as tableExists FROM information_schema.TABLES WHERE table_schema = 'erasmus_data' AND table_name = ?";
            PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
            checkStatement.setString(1, selectedTableName);
            ResultSet checkResultSet = checkStatement.executeQuery();
            checkResultSet.next();
            int tableExists = checkResultSet.getInt("tableExists");

            if (tableExists == 0) {
                return "Selected table does not exist.";
            }

            // Read data from the selected table
            Dataset<Row> df = spark.read().jdbc(jdbcUrl, selectedTableName, connectionProperties);

            // Filter data for the logged-in user
            df = df.filter(functions.col("user_id").equalTo(loggedInUserId));

            datasets.add(df);

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error occurred while retrieving table data.";
        }

        if (datasets.isEmpty()) {
            return "No data found for the logged-in user.";
        }

        // Adjust the column names to match your table schema
        Dataset<Row> aggregatedData = datasets.stream()
                .reduce((df1, df2) -> df1.union(df2))
                .orElseThrow(() -> new IllegalStateException("No data found"))
                .groupBy("user_id", "Sending Country Code")
                .agg(
                        functions.count("Participant Age").alias("Total Participants"),
                        functions.expr("(COUNT(`Participant Age`) / SUM(COUNT(`Participant Age`)) OVER (PARTITION BY user_id)) * 100").alias("Percentage")
                )
                .orderBy("user_id", "Sending Country Code");

        // Convert the aggregatedData to JSON format
        List<String> results = aggregatedData.toJSON().collectAsList();

        // Return the JSON-formatted results
        return results.toString();
    }

    public static List<String> insertFileIntoDatabase(String uploadedFile, String loggedInUserId, String tableName) {


        String uploadsDirectory = "uploads"; // Relative path to the uploads directory

        File uploadsDirFile = new File(uploadsDirectory);
        if (!uploadsDirFile.exists()) {
            uploadsDirFile.mkdirs();
        }

        String fullPath = uploadsDirectory + File.separator + uploadedFile;
        File uploadedFileObj = new File(fullPath);

        if (!uploadedFileObj.exists()) {
            return Collections.singletonList("File does not exist.");
        }

        DataFrameReader reader = spark.read();
        reader.option("header", "true");

        Dataset<Row> df;

        if (uploadedFile.endsWith(".csv")) {
            df = reader.csv(fullPath);
        } else if (uploadedFile.endsWith(".xlsx")) {
            // Handle Excel files using Apache POI
            try (FileInputStream excelFile = new FileInputStream(uploadedFileObj)) {
                Workbook workbook = new XSSFWorkbook(excelFile);
                Sheet sheet = workbook.getSheetAt(0);

                // Create a list to hold the column names
                List<String> columnNames = new ArrayList<>();

                // Read column names from the first row
                org.apache.poi.ss.usermodel.Row headerRow = sheet.getRow(0);
                for (Cell cell : headerRow) {
                    String columnName;
                    if (cell.getCellType() == CellType.STRING) {
                        columnName = cell.getStringCellValue();
                    } else if (cell.getCellType() == CellType.NUMERIC) {
                        // Handle numeric values differently (convert to string)
                        columnName = String.valueOf(cell.getNumericCellValue());
                    } else {
                        // Handle other cell types as needed
                        columnName = ""; // Change this as needed
                    }
                    columnNames.add(columnName);
                }

                // Read data rows into a list of maps
                List<Map<String, String>> data = new ArrayList<>();
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    org.apache.poi.ss.usermodel.Row dataRow = sheet.getRow(i);
                    Map<String, String> rowData = new HashMap<>();
                    for (int j = 0; j < columnNames.size(); j++) {
                        Cell cell = dataRow.getCell(j);
                        String columnName = columnNames.get(j);
                        String cellValue;
                        if (cell.getCellType() == CellType.STRING) {
                            cellValue = cell.getStringCellValue();
                        } else if (cell.getCellType() == CellType.NUMERIC) {
                            // Handle numeric values differently (convert to string)
                            cellValue = String.valueOf(cell.getNumericCellValue());
                        } else {
                            // Handle other cell types as needed
                            cellValue = ""; // Change this as needed
                        }
                        rowData.put(columnName, cellValue);
                    }
                    data.add(rowData);
                }

                // Convert the list of maps to a DataFrame
                List<Row> rows = new ArrayList<>();
                for (Map<String, String> rowData : data) {
                    List<String> values = new ArrayList<>();
                    for (String columnName : columnNames) {
                        values.add(rowData.get(columnName));
                    }
                    rows.add(RowFactory.create((Object[]) values.toArray()));
                }

                StructType schema = new StructType(columnNames.stream()
                        .map(columnName -> DataTypes.createStructField(columnName, DataTypes.StringType, true))
                        .toArray(StructField[]::new));

                df = spark.createDataFrame(rows, schema);
            } catch (IOException e) {
                e.printStackTrace();
                return Collections.singletonList("Error reading Excel file.");
            }
        } else {
            return Collections.singletonList("Unsupported file format.");
        }

        df = df.withColumn("user_id", functions.lit(loggedInUserId));

        String jdbcUrl = "jdbc:mysql://localhost:3306/erasmus_data";
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", "root");
        connectionProperties.setProperty("password", loadPasswordFromConfigFile());

        try {
            // Create the specified table
            df.write()
                    .mode(SaveMode.Append)
                    .jdbc(jdbcUrl, tableName, connectionProperties);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.singletonList("Error inserting data into the database.");
        }

        List<String> uploadedData = df.toJSON().collectAsList();

        return uploadedData;
    }

       public static String insertDataIntoDatabase(String selectedTableName, String projectRef, String mobilityDuration, int participantAge, String sendingCountryCode, String receivingCountryCode, String loggedInUserID) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = createConnectionToDatabase();

            // Update the query to use the selected table name
            String query = "INSERT INTO " + selectedTableName + " (`Project Reference`, `Mobility Duration`, `Participant Age`, `Sending Country Code`, `Receiving Country Code`, `user_id`) VALUES (?, ?, ?, ?, ?, ?)";

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, projectRef);
            preparedStatement.setString(2, mobilityDuration);
            preparedStatement.setInt(3, participantAge);
            preparedStatement.setString(4, sendingCountryCode);
            preparedStatement.setString(5, receivingCountryCode);
            preparedStatement.setString(6, loggedInUserID);

            preparedStatement.executeUpdate();

            // Clean up resources
            preparedStatement.close();
            connection.close();

            return "Data inserted successfully!";
        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred while inserting data.";
        }
    }



    public static String getAllDataFromDatabase() {
        StringBuilder result = new StringBuilder();

        ResultSet resultSet;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = createConnectionToDatabase();

            String query = "SELECT * FROM erasmus_data_table";
            Statement statement = connection.createStatement();
            resultSet = statement.executeQuery(query);

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            result.append("<table border='1'><tr>");
            for (int i = 1; i <= columnCount; i++) {
                result.append("<th>").append(metaData.getColumnName(i)).append("</th>");
            }
            result.append("</tr>");

            while (resultSet.next()) {
                result.append("<tr>");
                for (int i = 1; i <= columnCount; i++) {
                    String columnValue = resultSet.getString(i);
                    result.append("<td>").append(columnValue).append("</td>");
                }
                result.append("</tr>");
            }

            result.append("</table>");

            // Clean up resources
            resultSet.close();
            statement.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred while retrieving data.";
        }

        System.out.println();
        return result.toString();

    }



    public static String getUserTables(String loggedInUserId) throws SQLException {
        List<String> tableNames = new ArrayList<>();

        try (Connection connection = createConnectionToDatabase()) {
            // Get a list of all table names in the erasmus_data database
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, null, new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");

                    // Check if the table has a user_id column with the logged-in user
                    String query = "SELECT COUNT(*) FROM " + tableName + " WHERE user_id = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                        preparedStatement.setString(1, loggedInUserId);
                        ResultSet resultSet = preparedStatement.executeQuery();
                        if (resultSet.next() && resultSet.getInt(1) > 0) {
                            tableNames.add(tableName);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle any exceptions here
        }

        // Convert the list of table names to a JSON array
        Gson gson = new Gson();
        String jsonTableNames = gson.toJson(tableNames);

        return jsonTableNames;
    }



    public static String getModifiedUserTables(String loggedInUserId) throws SQLException {
        List<String> tableNames = new ArrayList<>();

        try (Connection connection = createConnectionToErasmusMobilityDatabase()) {
            // Get a list of all table names in the erasmus_data database
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, null, new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");

                    // Check if the table has a user_id column with the logged-in user
                    String query = "SELECT COUNT(*) FROM " + tableName + " WHERE user_id = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                        preparedStatement.setString(1, loggedInUserId);
                        ResultSet resultSet = preparedStatement.executeQuery();
                        if (resultSet.next() && resultSet.getInt(1) > 0) {
                            tableNames.add(tableName);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle any exceptions here
        }

        // Convert the list of table names to a JSON array
        Gson gson = new Gson();
        String jsonTableNames = gson.toJson(tableNames);

        return jsonTableNames;
    }



    private static final String DELETE_TABLE_SQL = "DROP TABLE IF EXISTS %s"; // Use prepared statement

    public boolean deleteTable(String loggedInUserId, String tableName) {
        // Check if the user has the necessary permissions for deletion (implement your logic)

        try (Connection connection = createConnectionToDatabase()) {
            String deleteTableSQL = String.format(DELETE_TABLE_SQL, tableName);

            try (PreparedStatement preparedStatement = connection.prepareStatement(deleteTableSQL)) {
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return false; // Table deletion failed
            }

            // Now, let's delete the associated file(s) from the "uploads" folder
            String uploadFolderPath = "uploads"; // Update with the actual path

            // Define a visitor to delete files recursively
            SimpleFileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString();
                    if (fileName.equals(tableName + ".csv") || fileName.equals(tableName + ".xlsx")) {
                        Files.delete(file); // Delete the file
                    }
                    return FileVisitResult.CONTINUE;
                }
            };

            // Traverse the folder and delete matching files
            Path uploadFolder = Paths.get(uploadFolderPath);
            EnumSet<FileVisitOption> options = EnumSet.noneOf(FileVisitOption.class);
            Files.walkFileTree(uploadFolder, options, Integer.MAX_VALUE, fileVisitor);
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // Database connection error
        } catch (IOException e) {
            e.printStackTrace();
            return false; // Error while deleting files
        }

        return true; // Table deleted successfully
    }

    // Method to delete a table from the erasmus_mobility database
    public boolean deleteModifiedTable(String loggedInUserId, String tableName) {
        // Check if the user has the necessary permissions for deletion (implement your logic)

        try (Connection connection = createConnectionToErasmusMobilityDatabase()) {
            String deleteTableSQL = String.format(DELETE_TABLE_SQL, tableName);

            try (PreparedStatement preparedStatement = connection.prepareStatement(deleteTableSQL)) {
                preparedStatement.executeUpdate();
                return true; // Table deleted successfully
            } catch (SQLException e) {
                e.printStackTrace();
                return false; // Table deletion failed
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // Database connection error
        }
    }


    public static String getTableData(String loggedInUserId, String tableName) {
        List<Map<String, String>> tableData = new ArrayList<>();

        // Check if loggedInUserId and tableName are null or empty
        if (loggedInUserId == null || loggedInUserId.isEmpty() || tableName == null || tableName.isEmpty()) {
            // Handle the case where loggedInUserId or tableName is null or empty
            return "Invalid user ID or table name"; // You can return an error message or handle it as needed.
        }


        try (Connection connection = createConnectionToDatabase()) {
            // Prepare and execute a query to fetch data from the specified table
            String query = "SELECT * FROM " + tableName + " WHERE user_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, loggedInUserId);
                ResultSet resultSet = preparedStatement.executeQuery();

                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (resultSet.next()) {
                    Map<String, String> rowData = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        String columnValue = resultSet.getString(i);
                        rowData.put(columnName, columnValue);
                    }
                    tableData.add(rowData);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle any exceptions here
        }

        // Convert the list of table data to a JSON string
        Gson gson = new Gson();
        String jsonData = gson.toJson(tableData);

        return jsonData;
    }


    // Retrieves the password saved in the file on PC
    private static String loadPasswordFromConfigFile() {
        Properties config = new Properties();
        String password = "";

        try {
            FileInputStream input = new FileInputStream("C:/Users/Marius/Desktop/config.properties");
            config.load(input);
            password = config.getProperty("db.password");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return password;
    }
}
