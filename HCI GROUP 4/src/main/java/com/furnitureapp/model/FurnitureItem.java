package com.furnitureapp.model;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

/**
 * Represents a single piece of furniture on the 2D canvas.
 * Stores its original 2D footprint (Shape) derived from a model file (centered at origin),
 * its base largest dimension for normalization, and its current transformation state.
 */
public final class FurnitureItem {
    // Base shape (untransformed) centered around (0,0)
    private final Shape baseFootprint;
    private final Color color;
    private final String modelFilename; // Source model file (e.g., GLB)
    private final String type; // Derived from filename or default
    private final double baseLargestDimension; // For 3D normalization

    // Transformation components
    private final double tx; // Translation X
    private final double ty; // Translation Y
    private final double scaleX;
    private final double scaleY;
    private final double rotationRadians;

    /**
     * Primary constructor. Assumes baseFootprint is centered around (0,0).
     * Initializes transform components to default (no translation, scale 1, no rotation).
     */
    public FurnitureItem(Shape baseFootprint, Color color, String modelFilename, String type, double baseLargestDimension) {
        this(baseFootprint, color, modelFilename, type, baseLargestDimension, 0.0, 0.0, 1.0, 1.0, 0.0);
    }

    /**
     * Private constructor for creating instances with updated transformation components.
     */
    private FurnitureItem(Shape baseFootprint, Color color, String modelFilename, String type, double baseLargestDimension,
                          double tx, double ty, double scaleX, double scaleY, double rotationRadians) {
        this.baseFootprint = Objects.requireNonNull(baseFootprint, "baseFootprint cannot be null");
        this.color = Objects.requireNonNull(color, "color cannot be null");
        this.modelFilename = Objects.requireNonNull(modelFilename, "modelFilename cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.baseLargestDimension = baseLargestDimension; // Store base dimension
        this.tx = tx;
        this.ty = ty;
        this.scaleX = Math.max(0.01, scaleX); // Ensure minimum scale
        this.scaleY = Math.max(0.01, scaleY);
        this.rotationRadians = rotationRadians;
    }

    /**
     * Returns the footprint shape *after* applying the current transform components.
     * The transform order is Scale -> Rotate -> Translate.
     */
    public Shape footprint() {
        AffineTransform at = new AffineTransform();
        at.translate(tx, ty);           // 3. Translate to final position
        at.rotate(rotationRadians);     // 2. Rotate around origin (since base is centered)
        at.scale(scaleX, scaleY);       // 1. Scale around origin
        return at.createTransformedShape(baseFootprint);
    }

    /**
     * Returns the original, untransformed base footprint shape (centered at origin).
     */
    public Shape getBaseFootprint() {
        return baseFootprint;
    }

    // Getters for transform components
    public double getTx() { return tx; }
    public double getTy() { return ty; }
    public double getScaleX() { return scaleX; }
    public double getScaleY() { return scaleY; }
    public double getRotationRadians() { return rotationRadians; }

    // Getters for identity/base properties
    public Color color() { return color; }
    public String getModelFilename() { return modelFilename; } // Renamed getter
    public String type() { return type; }
    public double getBaseLargestDimension() { return baseLargestDimension; } // New getter

    /**
     * Creates a new FurnitureItem instance translated to a specific location (tx, ty).
     */
    public FurnitureItem translateTo(double newTx, double newTy) {
        // Pass all other fields unchanged
        return new FurnitureItem(this.baseFootprint, this.color, this.modelFilename, this.type, this.baseLargestDimension,
                                 newTx, newTy, this.scaleX, this.scaleY, this.rotationRadians);
    }

    /**
     * Creates a new FurnitureItem instance moved to have its BOUNDING BOX top-left at (x, y).
     */
    public FurnitureItem moveTo(double x, double y) {
        Rectangle2D currentBounds = this.footprint().getBounds2D();
        double currentX = currentBounds.getX();
        double currentY = currentBounds.getY();
        // Calculate required delta translation based on current state
        double dx = x - currentX;
        double dy = y - currentY;
        // Apply delta translation to current translation
        return translateTo(this.tx + dx, this.ty + dy);
    }

    /**
     * Creates a new FurnitureItem instance with a specific total scale factor.
     */
    public FurnitureItem setScale(double newScaleX, double newScaleY) {
         // Pass all other fields unchanged
         return new FurnitureItem(this.baseFootprint, this.color, this.modelFilename, this.type, this.baseLargestDimension,
                                 this.tx, this.ty, newScaleX, newScaleY, this.rotationRadians);
    }

     /**
     * Creates a new FurnitureItem instance with a specific total rotation angle.
     */
    public FurnitureItem setRotation(double newRotationRadians) {
         // Pass all other fields unchanged
         return new FurnitureItem(this.baseFootprint, this.color, this.modelFilename, this.type, this.baseLargestDimension,
                                 this.tx, this.ty, this.scaleX, this.scaleY, newRotationRadians);
    }

    public FurnitureItem withColor(java.awt.Color newColor) {
        return new FurnitureItem(this.baseFootprint, newColor, this.modelFilename, this.type, this.baseLargestDimension,
                                 this.tx, this.ty, this.scaleX, this.scaleY, this.rotationRadians);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FurnitureItem) obj;
        // Equality based *only* on identifying source information (filename, maybe base shape/type)
        // DO NOT include tx, ty, scaleX, scaleY, rotationRadians
        return Objects.equals(this.modelFilename, that.modelFilename) &&
               Objects.equals(this.type, that.type) && // Added type for better distinction if needed
               Objects.equals(this.baseFootprint, that.baseFootprint); // Optional: Compare base shapes if filename isn't unique enough
               // Objects.equals(this.color, that.color); // Color probably shouldn't determine equality
    }

    @Override
    public int hashCode() {
        // Hashing based *only* on identifying source information
        // DO NOT include tx, ty, scaleX, scaleY, rotationRadians
        return Objects.hash(modelFilename, type, baseFootprint); // Match fields used in equals()
    }

    @Override
    public String toString() {
        // Include new fields
        return "FurnitureItem[" +
               "modelFilename=" + modelFilename + ", " +
               "type=" + type + ", " +
               "baseLargestDimension=" + String.format("%.3f", baseLargestDimension) + ", " +
               "color=" + color + ", " +
               "tx=" + tx + ", ty=" + ty + ", scaleX=" + scaleX + ", scaleY=" + scaleY + ", rot=" + Math.toDegrees(rotationRadians) + ", " +
               "bounds=" + footprint().getBounds2D() + // Show current bounds
               ']';
    }

} 