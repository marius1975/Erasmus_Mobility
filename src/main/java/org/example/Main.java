/*The project is a data analysis application that focuses on analyzing participant mobility in the Erasmus program.
It processes a CSV file containing information about the project references, mobility duration, participant age, sending country code, and receiving country code.
The application utilizes Apache Spark and Spark SQL to read and analyze the data
*/
package org.example;

import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;


public class Main {




    public static void main(String[] args) {


        displayData();
        orderByReceivingCountry();

    }



    //displays data from the Erasmus.csv file
    public static void displayData(){
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
    public static void orderByReceivingCountry(){
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

}
