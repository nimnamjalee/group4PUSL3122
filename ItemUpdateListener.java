package com.furnitureapp.ui;

import com.furnitureapp.model.FurnitureItem;

/**
 * Listener interface for components that need to be notified about
 * changes to FurnitureItems originating from the DrawingCanvas.
 */
public interface ItemUpdateListener {
    /** Called when an item's transform (position, rotation, scale) has been changed in the canvas. */
    void itemUpdated(FurnitureItem updatedItem);
    /** Called when an item is selected in the canvas. */
    void itemSelected(FurnitureItem selectedItem);
    /** Called when no item is selected in the canvas. */
    void itemDeselected();
} 