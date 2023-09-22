
# Erasmus Mobility Data Centre

The Erasmus Mobility Data Centre is a web application designed to manage and analyze data related to Erasmus student mobility programs. This README provides an overview of the app's features, how to set it up, and how to get started.

## Features

### 1. User Registration and Login

- Users can create accounts by registering with a username and password.
- Registered users can log in to access the application's features.

### 2. File Upload and Database Integration

- Users can upload data files in CSV or Excel (XLSX) format.
- The files to be uploaded should have the following column names: "Project Reference" , "Mobility Duration", "Participant Age", "Sending Country Code", "Receiving Country Code".
- The uploaded data is saved in a MySQL database.
- The table name in the database matches the name of the upladed file.

### 3. Data Analysis

- Users can perform data analysis and visualization on the uploaded data.
- Various data analysis tools are available, including filtering, sorting, and aggregation.

### 4. Data Export

- Analyzed data can be exported to CSV format for further use or sharing.

### 5. Data Deletion

- Users with appropriate permissions can delete tables and files from the database.

## Getting Started

### Prerequisites

- Java 8 or higher
- Apache Maven
- MySQL database server
- Spark (for data analysis)
- Web server (e.g., Apache Tomcat)
- Web browser

### Installation and Setup

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/erasmus-mobility-data-centre.git

