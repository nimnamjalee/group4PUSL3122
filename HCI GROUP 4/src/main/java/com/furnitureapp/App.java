package com.furnitureapp;

import com.furnitureapp.database.DatabaseManager;
import com.furnitureapp.ui.LoginPanel;
import com.furnitureapp.ui.MainWorkspacePanel;

import javax.swing.*;
import java.awt.*;

/**
 * Main application class for the Furniture Designer.
 */
public class App {

    private static JFrame frame;
    private static DatabaseManager dbManager;
    private static CardLayout cardLayout;
    private static JPanel mainPanel;

    private static final String LOGIN_PANEL = "LoginPanel";
    private static final String MAIN_APP_PANEL = "MainWorkspacePanel";

    public static void main(String[] args) {
        // Initialize Database Manager first
        dbManager = new DatabaseManager();

        // Ensure UI updates are done on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }

    private static void createAndShowGUI() {
        // Create the main frame
        frame = new JFrame("Furniture Designer - PULS3122");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Use CardLayout to switch between Login and Main App
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Create Login Panel
        LoginPanel loginPanel = new LoginPanel(dbManager, App::showMainApplication);

        // Create the Main Workspace Panel
        MainWorkspacePanel mainWorkspacePanel = new MainWorkspacePanel();

        // Add panels to the CardLayout container
        mainPanel.add(loginPanel, LOGIN_PANEL);
        mainPanel.add(mainWorkspacePanel, MAIN_APP_PANEL);

        // Add the CardLayout container panel to the frame
        frame.getContentPane().add(mainPanel);

        // Show Login Panel initially
        cardLayout.show(mainPanel, LOGIN_PANEL);

        // Center the frame on the screen
        frame.setLocationRelativeTo(null);

        // Make the frame visible
        frame.setVisible(true);

        System.out.println("GUI Initialized. Showing Login Panel.");
    }

    /**
     * Switches the view to the main application panel.
     * This method is intended to be called after successful login.
     */
    private static void showMainApplication() {
        System.out.println("Login successful. Switching to Main Application view.");
        cardLayout.show(mainPanel, MAIN_APP_PANEL);
        // Optionally resize or re-center the frame if needed for the main view
        // frame.pack();
        // frame.setLocationRelativeTo(null);
    }

} 