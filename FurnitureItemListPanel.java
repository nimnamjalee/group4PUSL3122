package com.furnitureapp.ui;

import com.furnitureapp.model.FurnitureItem;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panel to display the list of added furniture items.
 * Placeholder implementation.
 */
public class FurnitureItemListPanel extends JPanel {

    private JList<String> itemList;
    private DefaultListModel<String> listModel;

    public FurnitureItemListPanel() {
        setLayout(new BorderLayout());
        listModel = new DefaultListModel<>();
        itemList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(itemList);
        add(scrollPane, BorderLayout.CENTER);
        setBorder(BorderFactory.createTitledBorder("Items"));
        setPreferredSize(new Dimension(150, 0)); // Set preferred width

        listModel.addElement("(No items yet)");
    }

    /**
     * Updates the list display based on the provided furniture items.
     * @param items The current list of furniture items.
     */
    public void updateList(List<FurnitureItem> items) {
        listModel.clear();
        if (items == null || items.isEmpty()) {
            listModel.addElement("(No items yet)");
        } else {
            for (FurnitureItem item : items) {
                String itemLabel = String.format("%s (%.1f, %.1f)", item.type(), item.getTx(), item.getTy());
                // Consider adding filename or other identifier if types aren't unique
                // String itemLabel = String.format("%s [%s]", item.type(), item.getModelFilename());
                listModel.addElement(itemLabel);
            }
        }
        // TODO: Add listener to handle selection in this list syncing with DrawingCanvas
    }
} 