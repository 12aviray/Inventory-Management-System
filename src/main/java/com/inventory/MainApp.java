package com.inventory;

import com.inventory.util.DatabaseSeeder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Ensure schema exists and sample data is loaded on first run.
        DatabaseSeeder.run();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Inventory Management System");
        primaryStage.setScene(new Scene(root, 1100, 650));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
