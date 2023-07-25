package org.example;

import org.apache.spark.sql.*;
import org.apache.spark.sql.SaveMode;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


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


    //display data from the Erasmus.csv file
    public String displayData() {

        //prints the schema and displays the data from the file.
        DataFrameReader reader = spark.read();
        reader.option("header", "true");

        Dataset<Row> df = reader.csv("src/main/resources/Erasmus.csv");
        df.printSchema();
        df.show(100, false); //Integer.Max to display the entire data in the file

        // Perform the data processing and collect the results into a list
        List<String> results = df.toJSON().collectAsList();

        return results.toString();
    }


    //display filtered data from the Erasmus.csv file, ordered by receiving country code , sending country code and number of participants from each sending country
    public String orderByReceivingCountry() {

        //read the Erasmus.csv file
        DataFrameReader reader = spark.read();
        reader.option("header", "true");

        Dataset<Row> df = reader.csv("src/main/resources/Erasmus.csv");

        //Filter the receiving countries by country code
        df = df.filter(functions.col("Receiving Country Code").isin("DE", "IT", "FR"));

        Dataset<Row> groupedData = df.groupBy("Receiving Country Code", "Sending Country Code")
                .agg(functions.count("Participant Age").alias("Number of Participants"))
                .orderBy("Receiving Country Code", "Sending Country Code");

        groupedData.show(100, false); //Integer.Max to display the entire data in the file


        List<String> results = groupedData.toJSON().collectAsList();

        return results.toString();
    }

    // Save data from the filtered countries to the database.
    public List<String> saveToDatabase() {

        //Read the Erasmus.csv file
        DataFrameReader reader = spark.read();
        reader.option("header", "true");

        Dataset<Row> df = reader.csv("src/main/resources/Erasmus.csv");

        // Filter the data by the country code
        df = df.filter(functions.col("Receiving Country Code").isin("DE", "FR", "IT"));

        Dataset<Row> groupedDataToBeSaved = df.groupBy("Receiving Country Code", "Sending Country Code")
                .agg(functions.count("Participant Age").alias("Number of Participants"))
                .orderBy("Receiving Country Code", "Sending Country Code");
        //groupedDataToBeSaved.show(100, false);

        //Create connection to the database
        String jdbcUrl = "jdbc:mysql://localhost:3306/erasmus_mobility";
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", "root");
        connectionProperties.setProperty("password", loadPasswordFromConfigFile());


        List<String> results = new ArrayList<>();

        //Save the filtered data to the database
        String[] receivingCountries = {"DE", "FR", "IT"};
        Dataset<Row> filteredData = null;
        for (String country : receivingCountries) {
            filteredData = groupedDataToBeSaved
                    .filter(functions.col("Receiving Country Code").equalTo(country))
                    .drop("Receiving Country Code");
            String tableName = country.toLowerCase() + "_table";
            filteredData
                    .write()
                    .mode(SaveMode.Overwrite)
                    .jdbc(jdbcUrl, tableName, connectionProperties);

            List<String> dataForCountry = filteredData.toJSON().collectAsList();///schimbat results cu dataForcountry

            results.add("{\"country\": \"" + country + "\", \"data\": " + dataForCountry.toString() + "}");

        }

        return results;

    }

    //retrieves the password saved in the file on PC
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
