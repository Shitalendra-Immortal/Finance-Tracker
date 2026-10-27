package com.financetracker;

import com.financetracker.db.Database;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/** Entry point. Creates the database on first launch, then opens the main window. */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        try {
            Database.initDatabase();
        } catch (Exception e) {
            System.err.println("Database init failed: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.getDialogPane().getStylesheets().add(Main.class.getResource("/css/style.css").toExternalForm());
            alert.setTitle("Database error");
            alert.setHeaderText("Could not connect to MySQL");
            alert.setContentText("Check that MySQL is running and that db.properties "
                    + "has the right host, user and password.");
            alert.showAndWait();
            Platform.exit();
            return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 1080, 700);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setTitle("Personal Finance Tracker");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
