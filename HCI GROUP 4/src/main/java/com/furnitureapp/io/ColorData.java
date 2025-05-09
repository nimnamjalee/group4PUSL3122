package com.furnitureapp.io;

import java.awt.Color;

/**
 * Simple DTO for serializing/deserializing java.awt.Color.
 */
public class ColorData {
    public int r;
    public int g;
    public int b;
    public int a;

    // No-arg constructor for Jackson
    public ColorData() {}

    public ColorData(Color color) {
        if (color != null) {
            this.r = color.getRed();
            this.g = color.getGreen();
            this.b = color.getBlue();
            this.a = color.getAlpha();
        } else {
            // Default to gray if null? Or handle upstream?
            this.r = 128;
            this.g = 128;
            this.b = 128;
            this.a = 255;
        }
    }

    public Color toAwtColor() {
        // Ensure values are within valid range (0-255)
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        a = Math.max(0, Math.min(255, a));
        return new Color(r, g, b, a);
    }
} 