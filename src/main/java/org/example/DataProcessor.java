package org.example;

import org.apache.spark.sql.*;
import org.apache.spark.sql.SaveMode;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class DataProcessor {

    //displays data from the Erasmus.csv file
    public void displayData(){
        //create spark session
        SparkSession spark = SparkSession.builder()
                .appName("Erasmus")
                .master("local[*]")
                .getOrCreate();

        //reads the Erasmus.csv file ,prints the schema and displays the data from the file.
        DataFrameReader reader = spark.read();
        reader.option("header", "true");

        Dataset<Row> df = reader.csv("src/main/resources/Erasmus.csv");
        df.printSchema();
        df.show(100,false); //Integer.Max to display the entire data in the file

        //stop spark session
        spark.stop();
    }



    //displays filtered data from the Erasmus.csv file, ordered by receiving country code , sending country code and number of participants from each sending country
    public  void orderByReceivingCountry(){
        //create spark session
        SparkSession spark = SparkSession.builder()
                .appName("Erasmus")
                .master("local[*]")
                .getOrCreate();

        //reads the Erasmus.csv file
        DataFrameReader reader = spark.read();
        reader.option("header", "true");

        Dataset<Row> df = reader.csv("src/main/resources/Erasmus.csv");

        //Filters the receiving countries by country code
        df=df.filter(functions.col("Receiving Country Code").isin("DE","IT","FR"));

        Dataset<Row> groupedData = df.groupBy("Receiving Country Code", "Sending Country Code")
                .agg(functions.count("Participant Age").alias("Number of Participants"))
                .orderBy("Receiving Country Code", "Sending Country Code");

        groupedData.show(100,false); //Integer.Max to display the entire data in the file


        //stop spark session
        spark.stop();
    }

    // Saves data from the filtered countries to the database.
    public  void saveToDatabase() {

        //creates spark session
        SparkSession spark = SparkSession.builder()
                .appName("Erasmus")
                .master("local[*]")
                .getOrCreate();

        //Reads the Erasmus.csv file
        DataFrameReader reader = spark.read();
        reader.option("header", "true");

        Dataset<Row> df = reader.csv("src/main/resources/Erasmus.csv");

        // Filters the data by the country code
        df = df.filter(functions.col("Receiving Country Code").isin("DE", "FR", "IT"));

        Dataset<Row> groupedDataToBeSaved = df.groupBy("Receiving Country Code", "Sending Country Code")
                .agg(functions.count("Participant Age").alias("Number of Participants"))
                .orderBy("Receiving Country Code", "Sending Country Code");
        //groupedDataToBeSaved.show(100, false);

        //creates connection to the database
        String jdbcUrl = "jdbc:mysql://localhost:3306/erasmus_mobility";
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", "root");
        connectionProperties.setProperty("password", loadPasswordFromConfigFile());



        //saves the filtered data to the database
        String[] receivingCountries = {"DE", "FR", "IT"};
        for (String country : receivingCountries) {
            Dataset<Row> filteredData = groupedDataToBeSaved.filter(functions.col("Receiving Country Code").equalTo(country));
            String tableName = country.toLowerCase()+ "_table";
            filteredData
                    .write()
                    .mode(SaveMode.Overwrite)
                    .jdbc(jdbcUrl, tableName, connectionProperties);
        }

        spark.stop();

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
