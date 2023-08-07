

package org.example;
import org.apache.spark.sql.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;


public class DataProcessor {

    private final SparkSession spark;

    public DataProcessor() {
        // Create a Spark session when the DataProcessor is initialized
        this.spark = SparkSession.builder()
                .appName("Erasmus")
                .master("local[*]")
                .getOrCreate();
    }

    // Close the Spark session when the DataProcessor is no longer needed
    public void close() {
        this.spark.stop();
    }

    // Display data from the Erasmus.csv file
    public String displayData() {
        // Prints the schema and displays the data from the file.
        DataFrameReader reader = spark.read();
        reader.option("header", "true");

        Dataset<Row> df = reader.csv("src/main/resources/Erasmus.csv"); //path to the csv file
        df.printSchema();
        df.show(100, false); // Integer.Max to display the entire data in the file

        // Perform the data processing and collect the results into a list
        List<String> results = df.toJSON().collectAsList();

        return results.toString();
    }

    // Display filtered data from the Erasmus.csv file, ordered by receiving country code, sending country code, and the number of participants from each sending country
    public String orderByReceivingCountry(String userInput) {
        String[] receivingCountryCodes = userInput.split(" ");
        List<String> receivingCountryCodeList = new ArrayList<>();

        // Trim and add the country codes to the list
        for (String code : receivingCountryCodes) {
            receivingCountryCodeList.add(code.trim());
        }

        // Read the Erasmus.csv file
        DataFrameReader reader = spark.read();
        reader.option("header", "true");

        Dataset<Row> df = reader.csv("src/main/resources/Erasmus.csv"); // path to the csv file

        // Filter the receiving countries by country codes
        df = df.filter(functions.col("Receiving Country Code").isin(receivingCountryCodeList.toArray()));

        Dataset<Row> groupedData = df.groupBy("Receiving Country Code", "Sending Country Code")
                .agg(functions.count("Participant Age").alias("Number of Participants"))
                .orderBy("Receiving Country Code", "Sending Country Code");

        groupedData.show(100, false); // Integer.Max to display the entire data in the file

        List<String> results = groupedData.toJSON().collectAsList();

        return results.toString();
    }

    // Save data from the filtered countries to the database
    public List<String> saveToDatabase(String userInput) {
        String[] receivingCountryCodes = userInput.split(" ");
        List<String> receivingCountryCodeList = new ArrayList<>();

        // Trim and add the country codes to the list
        for (String code : receivingCountryCodes) {
            receivingCountryCodeList.add(code.trim());
        }

        // Read the Erasmus.csv file
        DataFrameReader reader = spark.read();
        reader.option("header", "true");

        Dataset<Row> df = reader.csv("src/main/resources/Erasmus.csv");

        // Filter the data by the country code
        df = df.filter(functions.col("Receiving Country Code").isin(receivingCountryCodeList.toArray()));

        Dataset<Row> groupedDataToBeSaved = df.groupBy("Receiving Country Code", "Sending Country Code")
                .agg(functions.count("Participant Age").alias("Number of Participants"))
                .orderBy("Receiving Country Code", "Sending Country Code");

        // Create connection to the database
        String jdbcUrl = "jdbc:mysql://localhost:3306/erasmus_mobility";
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", "root"); // use your username
        connectionProperties.setProperty("password", loadPasswordFromConfigFile()); //use your password

        List<String> results = new ArrayList<>();

        // Save the filtered data to the database
        Dataset<Row> filteredData;
        for (String country : receivingCountryCodes) {
            filteredData = groupedDataToBeSaved
                    .filter(functions.col("Receiving Country Code").equalTo(country))
                    .drop("Receiving Country Code");
            String tableName = country.toLowerCase() + "_table";
            filteredData
                    .write()
                    .mode(SaveMode.Overwrite)
                    .jdbc(jdbcUrl, tableName, connectionProperties);

            List<String> dataForCountry = filteredData.toJSON().collectAsList();

            results.add("{\"country\": \"" + country + "\", \"data\": " + dataForCountry.toString() + "}");
        }

        return results;
    }




    // Displays aggregated data from each sending country ,from all the tables saved in the database.

    public String aggregateParticipantsBySendingCountry() {
        String jdbcUrl = "jdbc:mysql://localhost:3306/erasmus_mobility";
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", "root");
        connectionProperties.setProperty("password", loadPasswordFromConfigFile());

        List<String> tableNames = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, connectionProperties)) {
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
            datasets.add(df);
        }

        Dataset<Row> aggregatedData = datasets.stream()
                .reduce((df1, df2) -> df1.union(df2))
                .orElseThrow(() -> new IllegalStateException("No data found"))
                .groupBy("Sending Country Code")
                .agg(functions.sum("Number of Participants").alias("Total Participants"))
                .orderBy("Sending Country Code");

        // Convert the aggregatedData to JSON format
        List<String> results = aggregatedData.toJSON().collectAsList();

        // Return the JSON-formatted results
        return results.toString();
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
