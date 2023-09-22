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
import spark.Session;
import spark.Spark;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static spark.Spark.*;

public class WebApp {

    public static void main(String[] args) {
        // Set the port for Spark
        port(8082);

        // Serve static files (HTML, CSS, JavaScript)
        staticFiles.location("/resources");

        // Define routes for the API and call the methods from DataProcessor class
        DataProcessor dp = new DataProcessor();

        Spark.before((request, response) -> {
            Session session = request.session(true); // Create a new session if it doesn't exist

        });


        // Serve the index.html page
        get("/", (req, res) -> {
            res.type("text/html");
            return renderHTML("login.html"); //added this "login.html"
        });
        // API endpoint to get a list of user tables
        get("/api/getUserTables", (request, response) -> {
            String loggedInUserID = request.session().attribute("loggedInUserID");
            String tables = DataProcessor.getUserTables(loggedInUserID).toString();
            return tables;
        });

        get("/api/getModifiedUserTables", (request, response) -> {
            String loggedInUserID = request.session().attribute("loggedInUserID");
            String tables = DataProcessor.getModifiedUserTables(loggedInUserID).toString();
            return tables;
        });

        // API endpoint to get data from a specific table for a logged-in user
        get("/api/getTableData/:loggedInUserID/:tableName", (request, response) -> {
            String loggedInUserID = request.session().attribute("loggedInUserID");
            String tableName = request.params("tableName"); // Use params() to retrieve path parameters
            String tableData = DataProcessor.getTableData(loggedInUserID, tableName);
            return tableData;
        });

     /* get("/api/orderByReceivingCountry", (req, res) -> {
            String userInput = req.queryParams("countryCodes");
            Session session = req.session(); // Access the session object
            String loggedInUserID = req.session().attribute("loggedInUserID");
            String data =  dp.orderByReceivingCountry(userInput, loggedInUserID);
            return data;

        });*/

        get("/api/orderByReceivingCountry", (req, res) -> {
            String loggedInUserID = req.session().attribute("loggedInUserID"); // Assuming you have a session attribute for the logged-in user
            String tableName = req.queryParams("tableName"); // Get the selected table name from the query parameters
            String countryCodes = req.queryParams("countryCodes"); // Get the country codes from the query parameters

            String data = dp.orderByReceivingCountry(loggedInUserID, tableName, countryCodes);
            return data;
        });




        get("/api/orderByParticipantAge", (req, res) -> {
            String userInput = req.queryParams("participantsAge");
            Session session = req.session(); // Access the session object
            String tableName = req.queryParams("tableName");
            String loggedInUserID = req.session().attribute("loggedInUserID");
            String data =  dp.orderByParticipantAge(loggedInUserID,tableName,userInput);
            return data;

        });

        get("/api/saveToDatabase", (req, res) -> {
            String userInput = req.queryParams("countryCodes");
            String tableName = req.queryParams("tableName"); // Get the selected table name from the query parameters
            String loggedInUserID = req.session().attribute("loggedInUserID");
            return dp.saveToDatabase(loggedInUserID,tableName,userInput);
        });


        get("/api/saveToDatabaseByAge", (req, res) -> {
            String userInput = req.queryParams("participantsAge");
            String tableName = req.queryParams("tableName"); // Get the selected table name from the query parameters
            String loggedInUserID = req.session().attribute("loggedInUserID");
            return dp.saveToDatabaseByAge(loggedInUserID,tableName,userInput);
        });
        // Define a route for fetching the sending country with the most students
       /* get("/api/aggregateParticipantsBySendingCountry", (req, res) ->{
            String loggedInUserID = req.session().attribute("loggedInUserID");
            return dp.aggregateParticipantsBySendingCountry(loggedInUserID);
        });*/


        // Define a route for fetching the sending country with the most students
        get("/api/participantsBySendingCountryGraph", (req, res) ->{
            String tableName = req.queryParams("tableName"); // Get the selected table name from the query parameters
            String loggedInUserID = req.session().attribute("loggedInUserID");
            return dp.participantsBySendingCountryGraph(loggedInUserID,tableName);
        });


        post("/api/insertData", (req, res) -> {
            String selectedTableName = req.queryParams("selectedTableName"); // Get the selected table name
            String projectRef = req.queryParams("projectRef");
            String mobilityDuration = req.queryParams("mobilityDuration");
            int participantAge = Integer.parseInt(req.queryParams("participantAge"));
            String sendingCountryCode = req.queryParams("sendingCountryCode");
            String receivingCountryCode = req.queryParams("receivingCountryCode");
            String loggedInUserID = req.session().attribute("loggedInUserID");

            DataProcessor.insertDataIntoDatabase(selectedTableName, projectRef, mobilityDuration, participantAge, sendingCountryCode, receivingCountryCode, loggedInUserID);

            return "Data inserted successfully!";
        });



        post("/api/uploadFile", (request, response) -> {
            String loggedInUserID = request.session().attribute("loggedInUserID");

            // Construct the uploads directory for the logged-in user (relative path)
            String uploadsDirectory = "uploads"; // Relative path to the uploads directory

            // Check if the directory exists, create it if it doesn't
            File uploadsDirFile = new File(uploadsDirectory);
            if (!uploadsDirFile.exists()) {
                uploadsDirFile.mkdirs(); // Create the directory and any missing parent directories
            }

            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(uploadsDirectory));

            Part uploadedFile = request.raw().getPart("file");
            String fileName = Paths.get(uploadedFile.getSubmittedFileName()).getFileName().toString();

            // Construct the full path to the destination file
            String destinationPath = uploadsDirectory + File.separator + fileName;

            // Check if the file already exists
            File uploadedFileObj = new File(destinationPath);
            if (uploadedFileObj.exists()) {
                return "File already uploaded to the uploads folder.";
            }

            try (InputStream is = uploadedFile.getInputStream()) {
                // Copy the uploaded file to the destination
                Files.copy(is, Paths.get(destinationPath));
            } catch (IOException e) {
                // Handle the exception
                e.printStackTrace();
                return "Failed to upload the file.";
            }

            String tableName = request.queryParams("tableName"); // Get the table name from the request

            // Insert file information into the database with the specified table name
            List<String> result = DataProcessor.insertFileIntoDatabase(fileName, loggedInUserID, tableName);

            if (result.isEmpty()) {
                return "Files successfully saved!";
            } else {
                return result.get(0); // Return any error message from the data processor
            }
        });


        // Define a route to display the login form
        get("/login", (req, res) -> {
            res.type("text/html");
            return renderHTML("login.html");
        });


        // Define a route to display the index page (accessible after successful login)
        // Define a route to display the login form
        get("/index", (req, res) -> {
            res.type("text/html");
            return renderHTML("index.html");
        });

        // Register route to display the registration form
        get("/register", (req, res) -> {
            res.type("text/html");
            return renderHTML("registration.html");
        });


        post("/login", (req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");

            // Perform authentication logic by calling a method from DataProcessor class
            DataProcessor dataProcessor = new DataProcessor();
            boolean isAuthenticated = dataProcessor.authenticateUser(username, password);

            if (isAuthenticated) {
                // Authentication successful
                // Redirect to the index page after successful login

                ///adaugat
                req.session().attribute("loggedInUserID", username);
                ///

                res.redirect("/index");
            } else {
                // Authentication failed
                // Redirect back to the login page with an error message
                res.redirect("/login?error=invalid_credentials");
            }

            return null;
        });


        // Register route to handle the registration form submission
        post("/register", (req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");

            DataProcessor dataProcessor = new DataProcessor();
            dataProcessor.registerUser(username, password);

            // Redirect to the login page after successful registration
            res.redirect("/login");

            return null;
        });



        Spark.delete("/api/deleteTable", (req, res) -> {
            String tableName = req.queryParams("tableName");
            String loggedInUserID = req.session().attribute("loggedInUserID");

            // Validate user authentication and authorization here

            // Call a method to delete the table
            boolean deleted = dp.deleteTable(loggedInUserID, tableName);

            if (deleted) {
                res.status(200); // Set HTTP status to OK (200)
                return "{\"message\":\"Table deleted successfully.\"}";
            } else {
                res.status(400); // Set HTTP status to Bad Request (400)
                return "{\"message\":\"Table deletion failed.\"}";
            }
        });
        Spark.delete("/api/deleteModifiedTable", (req, res) -> {
            String tableName = req.queryParams("tableName");
            String loggedInUserID = req.session().attribute("loggedInUserID");

            // Validate user authentication and authorization here

            // Call a method to delete the table
            boolean deleted = dp.deleteModifiedTable(loggedInUserID, tableName);

            if (deleted) {
                res.status(200); // Set HTTP status to OK (200)
                return "{\"message\":\"Table deleted successfully.\"}";
            } else {
                res.status(400); // Set HTTP status to Bad Request (400)
                return "{\"message\":\"Table deletion failed.\"}";
            }
        });


    }



    private static String renderHTML(String fileName) {
        try {
            // Read HTML content
            InputStream inputStream = WebApp.class.getResourceAsStream("/" + fileName);
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