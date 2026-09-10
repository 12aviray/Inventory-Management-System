package com.inventory;

/**
 * Main application entry point / bootstrap launcher.
 * <p>
 * This wrapper class does not directly extend javafx.application.Application,
 * allowing the application to be launched directly with the IDE's green "Run" button
 * (in IntelliJ IDEA, Eclipse, or VS Code) without requiring custom VM flags or module paths.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.silenceInternalJvmWarnings();
        MainApp.main(args);
    }
}
