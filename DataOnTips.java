package programs;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

public class DataOnTips extends Application {

    private static final String DB_URL = "jdbc:mysql://localhost:3307/data";
    private static final String USER = "root";
    private static final String PASS = "Rithu@1826";

    @Override
    public void start(Stage primaryStage) {
        VBox rootLayout = new VBox(20);
        rootLayout.setStyle("-fx-background-color: #33b2ff; -fx-padding: 20;");
        rootLayout.setAlignment(Pos.TOP_CENTER);

        // Title
        Label titleLabel = new Label("DataOnTips");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: black;");
        rootLayout.getChildren().add(titleLabel);

        // Disaster List
        ListView<String> disasterListView = new ListView<>();
        disasterListView.setPrefHeight(200);

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT Disaster_Type FROM disasters")) {

            while (rs.next()) {
                String disasterType = rs.getString("Disaster_Type");
                disasterListView.getItems().add(disasterType);
            }

            disasterListView.setOnMouseClicked(e -> {
                String selectedType = disasterListView.getSelectionModel().getSelectedItem();
                if (selectedType != null) {
                    openWindow("Details for " + selectedType, layout -> setupDisasterDetailsWindow(layout, selectedType));
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            disasterListView.getItems().add("Error retrieving data.");
        }

        rootLayout.getChildren().add(disasterListView);

        // Export Button on main page (if needed, otherwise remove if only per disaster type is required)
        Button exportButton = new Button("Download Data");
        exportButton.setOnAction(e -> exportToCSV());
        rootLayout.getChildren().add(exportButton);

        Scene scene = new Scene(rootLayout, 500, 500);
        primaryStage.setTitle("DataOnTips");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupDisasterDetailsWindow(VBox layout, String disasterType) {
        layout.setStyle("-fx-background-color: #33b2ff; -fx-padding: 20;");
        layout.setAlignment(Pos.TOP_CENTER);

        Label label = new Label("Details for " + disasterType);
        label.setStyle("-fx-font-size: 20px; -fx-text-fill: black;");
        layout.getChildren().add(label);

        ListView<String> placesList = new ListView<>();
        placesList.setPrefHeight(200);

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement("SELECT Location, Disaster_Date FROM disasters WHERE Disaster_Type = ?")) {

            stmt.setString(1, disasterType);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String location = rs.getString("Location");
                String date = rs.getDate("Disaster_Date").toString();
                placesList.getItems().add(location + " - " + date);
            }

            // When a location is clicked, extract only the location part (before " - ")
            placesList.setOnMouseClicked(e -> {
                String selectedItem = placesList.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    // Split the string to get only the location
                    String location = selectedItem.split(" - ")[0];
                    openWindow("Details for " + location, l -> setupPlaceDetailsWindow(l, location));
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            placesList.getItems().add("No information available.");
        }

        // Add the places list and Export button to this disaster window
        Button exportButton = new Button("Download Data");
        exportButton.setOnAction(e -> exportToCSV(disasterType));
        layout.getChildren().addAll(placesList, exportButton);

        // Exit Button
        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> ((Stage) layout.getScene().getWindow()).close());
        layout.getChildren().add(exitButton);
    }

    private void setupPlaceDetailsWindow(VBox layout, String place) {
        layout.setStyle("-fx-background-color: #33b2ff; -fx-padding: 20;");
        layout.setAlignment(Pos.TOP_CENTER);

        Label label = new Label("Details for " + place);
        label.setStyle("-fx-font-size: 20px; -fx-text-fill: black;");
        layout.getChildren().add(label);

        Label detailsLabel = new Label();
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM disasters WHERE Location = ?")) {

            stmt.setString(1, place);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String details = "Location: " + rs.getString("Location") + "\n" +
                        "Disaster Type: " + rs.getString("Disaster_Type") + "\n" +
                        "Disaster Subtype: " + rs.getString("Disaster_Subtype") + "\n" +
                        "Disaster Date: " + rs.getDate("Disaster_Date").toString() + "\n" +
                        "Total Deaths: " + rs.getInt("Total_Deaths");
                detailsLabel.setText(details);
            } else {
                detailsLabel.setText("No detailed information available.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            detailsLabel.setText("Error retrieving details.");
        }

        layout.getChildren().add(detailsLabel);

        // Exit Button
        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> ((Stage) layout.getScene().getWindow()).close());
        layout.getChildren().add(exitButton);
    }

    private void exportToCSV(String disasterType) {
        String fileName = disasterType + "_data.csv";
        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write("Disaster ID,Disaster Date,Disaster Type,Disaster Subtype,Location,Total Deaths\n");

            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM disasters WHERE Disaster_Type = ?")) {
                stmt.setString(1, disasterType);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    String line = rs.getInt("Disaster_ID") + "," +
                            rs.getDate("Disaster_Date") + "," +
                            rs.getString("Disaster_Type") + "," +
                            rs.getString("Disaster_Subtype") + "," +
                            rs.getString("Location") + "," +
                            rs.getInt("Total_Deaths") + "\n";
                    fileWriter.write(line);
                }
            }
            System.out.println("Data exported to " + fileName);
            // Print absolute path for debugging:
            java.io.File file = new java.io.File(fileName);
            System.out.println("File saved at: " + file.getAbsolutePath());
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }


    // This method is used to export all data from the main page (if needed)
    private void exportToCSV() {
        try (FileWriter fileWriter = new FileWriter("disaster_data.csv")) {
            fileWriter.write("Disaster ID,Disaster Date,Disaster Type,Disaster Subtype,Location,Total Deaths\n");

            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM disasters")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String line = rs.getInt("Disaster_ID") + "," +
                            rs.getDate("Disaster_Date") + "," +
                            rs.getString("Disaster_Type") + "," +
                            rs.getString("Disaster_Subtype") + "," +
                            rs.getString("Location") + "," +
                            rs.getInt("Total_Deaths") + "\n";
                    fileWriter.write(line);
                }
            }
            System.out.println("Data exported to disaster_data.csv");
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void openWindow(String title, WindowSetup setup) {
        Stage stage = new Stage();
        VBox layout = new VBox(20);
        setup.setup(layout);
        Scene scene = new Scene(layout, 500, 500);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }

    @FunctionalInterface
    interface WindowSetup {
        void setup(VBox layout);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
