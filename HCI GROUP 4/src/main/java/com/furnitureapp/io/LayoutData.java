package com.furnitureapp.io;

import java.util.List;

/**
 * Top-level DTO for saving/loading the entire layout state.
 */
public class LayoutData {
    public double roomWidthMeters;
    public double roomDepthMeters;
    public double wallHeightMeters;
    public List<ColorData> wallColors; // List of ColorData DTOs
    public List<ItemData> items; // List of ItemData DTOs

    // No-arg constructor for Jackson
    public LayoutData() {}
} 