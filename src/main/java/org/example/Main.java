/*The project is a data analysis application that focuses on analyzing participant mobility in the Erasmus program.
It processes a CSV file containing information about the project references, mobility duration, participant age, sending country code, and receiving country code.
The application utilizes Apache Spark and Spark SQL to read and analyze the data
*/




package org.example;



public class Main {



    public static void main(String[] args)  {


        DataProcessor dp = new DataProcessor();


        dp.displayData();
        //dp.orderByReceivingCountry();
        //dp.saveToDatabase();
        dp.aggregateParticipantsBySendingCountry();


    }


}









