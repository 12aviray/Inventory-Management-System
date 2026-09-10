package com.inventory;

import com.inventory.util.DatabaseSeeder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Main entry point for the Inventory Management desktop application.
 * Structured to launch seamlessly from any IDE (IntelliJ, VS Code, Eclipse)
 * without requiring custom JVM module flags.
 */
public class MainApp {

    public static void main(String[] args) {
        silenceInternalJvmWarnings();
        Application.launch(InventoryApplication.class, args);
    }

    public static void silenceInternalJvmWarnings() {
        System.setProperty("prism.verbose", "false");
        System.setProperty("javafx.verbose", "false");

        // Silence java.util.logging (e.g. PlatformImpl startup message)
        try {
            java.util.logging.LogManager.getLogManager().reset();
            java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
            rootLogger.setLevel(java.util.logging.Level.OFF);
            for (java.util.logging.Handler h : rootLogger.getHandlers()) {
                rootLogger.removeHandler(h);
            }
        } catch (Exception ignored) {
        }

        // Intercept byte-level writes to System.err to filter JVM diagnostic notices
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(new OutputStream() {
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            @Override
            public synchronized void write(int b) {
                if (b == '\n' || b == '\r') {
                    flushLine();
                } else {
                    buffer.write(b);
                }
            }

            @Override
            public synchronized void write(byte[] b, int off, int len) {
                for (int i = off; i < off + len; i++) {
                    write(b[i]);
                }
            }

            private void flushLine() {
                if (buffer.size() == 0) return;
                String line = buffer.toString(StandardCharsets.UTF_8);
                buffer.reset();
                if (!isIgnorableWarning(line)) {
                    originalErr.println(line);
                }
            }

            private boolean isIgnorableWarning(String text) {
                if (text == null) return false;
                String trimmed = text.trim();
                return trimmed.startsWith("WARNING:")
                        || trimmed.startsWith("SLF4J:")
                        || trimmed.contains("PlatformImpl startup")
                        || trimmed.contains("sun.misc.Unsafe")
                        || trimmed.contains("allocateMemory")
                        || trimmed.contains("OffHeapArray")
                        || trimmed.contains("NativeLibLoader")
                        || trimmed.contains("Unsupported JavaFX configuration")
                        || trimmed.contains("com.sun.javafx");
            }
        }, true, StandardCharsets.UTF_8));
    }

    public static class InventoryApplication extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            // Ensure schema exists and sample data is loaded on first run.
            DatabaseSeeder.run();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
            Parent root = loader.load();

            primaryStage.setTitle("Multi-Warehouse Inventory Management System");
            Scene scene = new Scene(root, 1150, 700);
            if (getClass().getResource("/css/style.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            }
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(650);
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    }
}
