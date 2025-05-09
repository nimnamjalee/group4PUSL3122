package com.furnitureapp.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A D-Pad style control panel for navigating the 2D canvas.
 */
public class DPadControlPanel extends JPanel {

    public static final String ACTION_PAN_UP = "PAN_UP";
    public static final String ACTION_PAN_DOWN = "PAN_DOWN";
    public static final String ACTION_PAN_LEFT = "PAN_LEFT";
    public static final String ACTION_PAN_RIGHT = "PAN_RIGHT";
    public static final String ACTION_ZOOM_IN = "ZOOM_IN";
    public static final String ACTION_ZOOM_OUT = "ZOOM_OUT";
    public static final String ACTION_RESET_VIEW = "RESET_VIEW";

    private final List<ActionListener> actionListeners = new ArrayList<>();

    public DPadControlPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("2D View Control"));
        GridBagConstraints gbc = new GridBagConstraints();
        Insets buttonInsets = new Insets(1, 1, 1, 1);

        // Create buttons
        JButton btnUp = createButton("^", ACTION_PAN_UP, buttonInsets);
        JButton btnDown = createButton("v", ACTION_PAN_DOWN, buttonInsets);
        JButton btnLeft = createButton("<", ACTION_PAN_LEFT, buttonInsets);
        JButton btnRight = createButton(">", ACTION_PAN_RIGHT, buttonInsets);
        JButton btnZoomIn = createButton("+", ACTION_ZOOM_IN, buttonInsets);
        JButton btnZoomOut = createButton("-", ACTION_ZOOM_OUT, buttonInsets);
        JButton btnReset = createButton("R", ACTION_RESET_VIEW, buttonInsets); // Reset button

        // Layout using GridBagLayout
        gbc.fill = GridBagConstraints.BOTH; // Make buttons fill cell
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // Row 0
        gbc.gridx = 1; gbc.gridy = 0;
        add(btnUp, gbc);
        gbc.gridx = 3; gbc.gridy = 0;
        add(btnZoomIn, gbc);

        // Row 1
        gbc.gridx = 0; gbc.gridy = 1;
        add(btnLeft, gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        add(btnReset, gbc); // Reset in the middle
        gbc.gridx = 2; gbc.gridy = 1;
        add(btnRight, gbc);
        gbc.gridx = 3; gbc.gridy = 1;
        add(btnZoomOut, gbc);

        // Row 2
        gbc.gridx = 1; gbc.gridy = 2;
        add(btnDown, gbc);

        setPreferredSize(new Dimension(150, 100)); // Suggest a size
    }

    private JButton createButton(String text, String actionCommand, Insets margins) {
        JButton button = new JButton(text);
        button.setActionCommand(actionCommand);
        button.setMargin(margins);
        button.addActionListener(e -> fireActionPerformed(e.getActionCommand()));
        return button;
    }

    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        actionListeners.remove(listener);
    }

    private void fireActionPerformed(String command) {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);
        for (ActionListener listener : actionListeners) {
            listener.actionPerformed(event);
        }
    }
} 