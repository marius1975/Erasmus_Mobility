package org.example;

import org.apache.spark.sql.*;
import org.apache.spark.sql.SaveMode;
//import javax.validation.groups.Default;
import java.util.Properties;
public class CSV_File_Processing_Methods {

    //displays data from the Erasmus.csv file
    public void displayData(){
        //create spark session
        SparkSession spark = SparkSession.builder()
                .appName("Erasmus")
                .master("local[*]")
                .getOrCreate();

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
    public  void saveToDatabase() {

        SparkSession spark = SparkSession.builder()
                .appName("Erasmus")
                .master("local[*]")
                .getOrCreate();

        DataFrameReader reader = spark.read();
        reader.option("header", "true");

        Dataset<Row> df = reader.csv("src/main/resources/Erasmus.csv");

        df = df.filter(functions.col("Receiving Country Code").isin("DE", "FR", "IT"));

        Dataset<Row> groupedDataToBeSaved = df.groupBy("Receiving Country Code", "Sending Country Code")
                .agg(functions.count("Participant Age").alias("Number of Participants"))
                .orderBy("Receiving Country Code", "Sending Country Code");
        //groupedDataToBeSaved.show(100, false);

        String jdbcUrl = "jdbc:mysql://localhost:3306/erasmus_mobility";
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("user", "root");
        connectionProperties.setProperty("password", "parolamea1");

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

}
