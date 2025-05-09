package com.furnitureapp.ui;

import com.furnitureapp.model.FurnitureItem;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

/**
 * Panel to display information about the selected furniture item.
 * Placeholder implementation.
 */
public class InfoPanel extends JPanel {

    private JLabel infoLabel;
    private DecimalFormat df = new DecimalFormat("#.##"); // Formatter for dimensions/positions
    private JButton changeColorButton;
    private JLabel scaleLabel;
    private JSpinner scaleSpinner;
    private boolean internalSpinnerUpdate = false; // Flag to prevent listener loops

    public InfoPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        infoLabel = new JLabel("Select an item to see its details.");
        add(infoLabel);
        
        changeColorButton = new JButton("Change Color");
        add(changeColorButton);
        // changeColorButton.setEnabled(false); // Initial state set by updateInfo

        // --- Add Scale Controls ---
        scaleLabel = new JLabel("Scale:");
        add(scaleLabel);
        // Spinner model: initial 1.0, min 0.0001, max 100.0, step 0.01
        scaleSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0001, 100.0, 0.01)); 
        // Set preferred size to prevent excessive width
        scaleSpinner.setPreferredSize(new Dimension(70, scaleSpinner.getPreferredSize().height)); 
        add(scaleSpinner);
        
        setBorder(BorderFactory.createTitledBorder("Selected Item Info"));
        // setPreferredSize(new Dimension(0, 60)); // REMOVE or COMMENT OUT fixed preferred height

        // Initially disable controls until an item is selected
        updateInfo(null);
    }

    /**
     * Updates the displayed information based on the selected item.
     * @param item The selected FurnitureItem, or null if none is selected.
     */
    public void updateInfo(FurnitureItem item) {
        internalSpinnerUpdate = true; // Prevent listener firing during update
        try {
            if (item == null) {
                infoLabel.setText("Select an item to see its details.");
                changeColorButton.setEnabled(false);
                scaleLabel.setEnabled(false);
                scaleSpinner.setEnabled(false);
                scaleSpinner.setValue(1.0); // Reset spinner when no item is selected
            } else {
                String infoText = "<html><b>Selected:</b> " + item.type() // Shorter title
                        // + " File: " + item.getModelFilename() // Maybe too long
                        + String.format(" | Pos: (%.1f, %.1f)", item.getTx(), item.getTy())
                        + String.format(" | Scale: %.2f", item.getScaleX()) // Assuming uniform scale for display
                        + String.format(" | Rot: %.1f°", Math.toDegrees(item.getRotationRadians()))
                        + "</html>";
                infoLabel.setText(infoText);
                
                changeColorButton.setEnabled(true);
                scaleLabel.setEnabled(true);
                scaleSpinner.setEnabled(true);
                // Set spinner value - use scaleX, assuming uniform scaling for control
                scaleSpinner.setValue(item.getScaleX()); 
            }
        } finally {
            internalSpinnerUpdate = false;
        }
    }

    // Listener for external changes (e.g., spinner interaction)
    public void addScaleChangeListener(ChangeListener listener) {
        scaleSpinner.addChangeListener(listener);
    }
    
    // Getter for spinner value (used by listener in MainWorkspacePanel)
    public double getScaleValue() {
        return (Double) scaleSpinner.getValue();
    }

    // Check if the spinner update was internal (to prevent loops)
    public boolean isInternalSpinnerUpdate() {
         return internalSpinnerUpdate;
    }

    public void setChangeColorActionListener(ActionListener listener) {
        changeColorButton.addActionListener(listener);
    }
} 