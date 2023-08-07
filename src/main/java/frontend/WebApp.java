/*

package frontend;


import org.example.DataProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static spark.Spark.*;

public class WebApp {

    public static void main(String[] args) {
        // Set the port for Spark
        port(8082);

        // Serve the index.html page
        get("/", (req, res) -> {
            res.type("text/html");
            return renderHTML();
        });

        // Define routes for the API and call the methods from DataProcessor class
        DataProcessor dp = new DataProcessor();

        get("/api/displayData", (req, res) -> displayData(dp));
        get("/api/orderByReceivingCountry", (req, res) -> orderByReceivingCountry(dp));
        get("/api/saveToDatabase", (req, res) -> saveToDatabase(dp));
    }

    private static String renderHTML() {
        try {
            // Read index.html content
            InputStream inputStream = WebApp.class.getResourceAsStream("/index.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder htmlContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                htmlContent.append(line);
            }
            reader.close();

            return htmlContent.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "<html><head></head><body><h1>Error: Failed to load HTML</h1></body></html>";
        }
    }


    private static String displayData(DataProcessor dp) {
        String result = dp.displayData();
        return result;
    }

    private static String orderByReceivingCountry(DataProcessor dp) {
        String result = dp.orderByReceivingCountry();
        return result;
    }

    private static List<String> saveToDatabase(DataProcessor dp) {
        List<String> results = dp.saveToDatabase();
        return results;
    }
}
*/
package frontend;
import org.example.DataProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


import static spark.Spark.*;

public class WebApp {

    public static void main(String[] args) {
        // Set the port for Spark
        port(8082);

        // Serve the index.html page
        get("/", (req, res) -> {
            res.type("text/html");
            return renderHTML();
        });

        // Define routes for the API and call the methods from DataProcessor class
        DataProcessor dp = new DataProcessor();

        get("/api/displayData", (req, res) -> dp.displayData());
        get("/api/orderByReceivingCountry", (req, res) -> {
            String userInput = req.queryParams("countryCodes");
            return dp.orderByReceivingCountry(userInput);
        });
        get("/api/saveToDatabase", (req, res) -> {
            String userInput = req.queryParams("countryCodes");
            return dp.saveToDatabase(userInput);
        });
        // Define a route for fetching the sending country with the most students
        get("/api/aggregateParticipantsBySendingCountry", (req, res) -> dp.aggregateParticipantsBySendingCountry());



    }

    private static String renderHTML() {
        try {
            // Read index.html content
            InputStream inputStream = WebApp.class.getResourceAsStream("/index.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder htmlContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                htmlContent.append(line);
            }
            reader.close();

            return htmlContent.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "<html><head></head><body><h1>Error: Failed to load HTML</h1></body></html>";
        }
    }
}
