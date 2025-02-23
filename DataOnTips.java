package programs;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;

public class DataOnTips extends Application {

    private static final String DB_URL = "jdbc:mysql://localhost:3307/data";
    private static final String USER = "root";
    private static final String PASS = "Rithu@1826";
    private static final String LIVE_API_URL = "https://eonet.gsfc.nasa.gov/api/v2.1/events";

    @Override
    public void start(Stage primaryStage) {
        VBox rootLayout = new VBox(20);
        rootLayout.setStyle("-fx-background-color: #33b2ff; -fx-padding: 20;");
        rootLayout.setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label("DataOnTips");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: black;");
        rootLayout.getChildren().add(titleLabel);

        ListView<String> disasterListView = new ListView<>();
        disasterListView.setPrefHeight(200);
        loadDisasterTypes(disasterListView);

        disasterListView.setOnMouseClicked(e -> {
            String selectedType = disasterListView.getSelectionModel().getSelectedItem();
            if (selectedType != null) {
                openWindow("Details for " + selectedType, layout -> setupDisasterDetailsWindow(layout, selectedType));
            }
        });

        // Export Button for All Data
        Button exportButton = new Button("Download All Data");
        exportButton.setOnAction(e -> exportToCSV());

        // Live Data Button
        Button liveDataButton = new Button("Fetch Live Disasters");
        liveDataButton.setOnAction(e -> openWindow("Live Disaster Data", this::setupLiveDisasterWindow));

        rootLayout.getChildren().addAll(disasterListView, exportButton, liveDataButton);

        Scene scene = new Scene(rootLayout, 500, 500);
        primaryStage.setTitle("DataOnTips");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadDisasterTypes(ListView<String> listView) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT Disaster_Type FROM disasters")) {

            while (rs.next()) {
                listView.getItems().add(rs.getString("Disaster_Type"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            listView.getItems().add("⚠️ Error retrieving data.");
        }
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

            // Clicking a place opens more details
            placesList.setOnMouseClicked(e -> {
                String selectedItem = placesList.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    String location = selectedItem.split(" - ")[0];
                    openWindow("Details for " + location, l -> setupPlaceDetailsWindow(l, location));
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            placesList.getItems().add("⚠️ No information available.");
        }

        // Export Button for Specific Disaster Type
        Button exportButton = new Button("Download Data");
        exportButton.setOnAction(e -> exportToCSV(disasterType));

        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> ((Stage) layout.getScene().getWindow()).close());

        layout.getChildren().addAll(placesList, exportButton, exitButton);
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
                String details = "📍 Location: " + rs.getString("Location") + "\n" +
                                 "🌊 Disaster Type: " + rs.getString("Disaster_Type") + "\n" +
                                 "📌 Subtype: " + rs.getString("Disaster_Subtype") + "\n" +
                                 "📅 Date: " + rs.getDate("Disaster_Date").toString() + "\n" +
                                 "☠️ Total Deaths: " + rs.getInt("Total_Deaths");
                detailsLabel.setText(details);
            } else {
                detailsLabel.setText("⚠️ No detailed information available.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            detailsLabel.setText("⚠️ Error retrieving details.");
        }

        layout.getChildren().add(detailsLabel);

        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> ((Stage) layout.getScene().getWindow()).close());
        layout.getChildren().add(exitButton);
    }


    private void setupLiveDisasterWindow(VBox layout) {
        layout.setStyle("-fx-background-color: #33b2ff; -fx-padding: 20;");
        layout.setAlignment(Pos.TOP_CENTER);

        Label label = new Label("Live Disaster Data");
        label.setStyle("-fx-font-size: 20px; -fx-text-fill: black;");
        layout.getChildren().add(label);

        ListView<String> liveDataList = new ListView<>();
        liveDataList.setPrefHeight(200);
        layout.getChildren().add(liveDataList);

        // Show loading message
        liveDataList.getItems().add("Fetching live data...");

        // Run API call in a background thread
        new Thread(() -> {
            try {
                URL url = new URL(LIVE_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder content = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                conn.disconnect();

                JSONObject jsonResponse = new JSONObject(content.toString());
                JSONArray events = jsonResponse.getJSONArray("events");

                Platform.runLater(() -> {
                    liveDataList.getItems().clear(); // Clear loading message
                    if (events.length() == 0) {
                        liveDataList.getItems().add("⚠️ No live disasters available.");
                    } else {
                        for (int i = 0; i < events.length(); i++) {
                            JSONObject event = events.getJSONObject(i);
                            liveDataList.getItems().add(event.getString("title"));
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    liveDataList.getItems().clear();
                    liveDataList.getItems().add("⚠️ Error fetching live data.");
                });
            }
        }).start();
    }


 // Export ALL disasters to a CSV file
    private void exportToCSV() {
        String fileName = "all_disasters_data.csv";

        try (FileWriter fileWriter = new FileWriter(fileName)) {
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

            showAlert("Export Successful", "All disaster data saved to " + fileName);

        } catch (IOException | SQLException e) {
            e.printStackTrace();
            showAlert("Export Failed", "An error occurred while exporting data.");
        }
    }

    // Export data for a SPECIFIC disaster type
    private void exportToCSV(String disasterType) {
        String fileName = disasterType.replaceAll("\\s+", "_") + "_data.csv";

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

            showAlert("Export Successful", "Data for " + disasterType + " saved to " + fileName);

        } catch (IOException | SQLException e) {
            e.printStackTrace();
            showAlert("Export Failed", "An error occurred while exporting data.");
        }
    }


    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
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
