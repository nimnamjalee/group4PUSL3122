package com.furnitureapp.io;

/**
 * Simple DTO for serializing/deserializing the state of a FurnitureItem.
 * Does not store the Shape, as that will be reloaded from the model file.
 */
public class ItemData {
    public String modelFilename;
    public String type;
    public ColorData color; // Use ColorData DTO
    public double tx;
    public double ty;
    public double scaleX;
    public double scaleY;
    public double rotationRadians;

    // No-arg constructor for Jackson
    public ItemData() {}
} 