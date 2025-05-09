package com.furnitureapp.ui;

import com.furnitureapp.model.FurnitureItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel dedicated to drawing the 2D representation of the room and furniture.
 * Handles mouse interactions for selection, movement, resizing, and rotation.
 */
public class DrawingCanvas extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {

    private static final Logger LOGGER = Logger.getLogger(DrawingCanvas.class.getName());
    private static final Color SELECTION_COLOR = Color.BLUE;
    private static final Color HANDLE_COLOR = Color.YELLOW;
    private static final int HANDLE_SIZE = 8;
    private static final Stroke SELECTION_STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f}, 0.0f);
    private static final Color ROTATION_HANDLE_COLOR = Color.ORANGE;
    private static final int ROTATION_HANDLE_OFFSET = 20; // Pixels away from corner
    private static final int ROTATION_HANDLE_INDEX = 4; // Use index 4 for rotation handle
    private static final Color ROOM_BORDER_COLOR = Color.DARK_GRAY;
    private static final Color ROOM_FILL_COLOR = new Color(240, 240, 240); // Light gray fill
    private static final double DEFAULT_PIXELS_PER_METER = 50.0; // Added default scale

    private List<FurnitureItem> furnitureItems = new ArrayList<>();
    private FurnitureItem selectedItem = null;
    private ItemUpdateListener listener;

    // --- View Transformation & Interaction State ---
    private AffineTransform viewTransform = new AffineTransform();
    private Point2D lastMousePressScreen; // Mouse position in screen coordinates
    private Point2D lastMousePressWorld;  // Mouse position transformed to world coordinates
    private Point2D lastMouseDragWorld;   // Last drag position in WORLD coordinates (re-added)
    private Point2D initialPanTranslation; // Store translation state at pan start
    private double lastDragAngle;        // Store angle state at last drag event for rotation
    private AffineTransform originalViewTransformState; // Added for panning state

    private enum InteractionMode { NONE, MOVING, RESIZING, ROTATING, PANNING }
    private InteractionMode currentMode = InteractionMode.NONE;
    private int activeHandle = -1; // Index of the active resize/rotate handle
    private FurnitureItem originalItemState; // Item state at the start of interaction

    // --- Room State ---
    private double roomWidthMeters = 5.0; // Default width
    private double roomDepthMeters = 4.0; // Default depth
    private double pixelsPerMeter = DEFAULT_PIXELS_PER_METER;
    private Rectangle2D roomBoundsWorld = new Rectangle2D.Double(); // Room bounds in world coords (pixels)

    public DrawingCanvas() {
        setBackground(Color.WHITE); // Background outside the room bounds
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this); // Register wheel listener
        updateRoomBoundsWorld(); // Calculate initial bounds
    }

    /**
     * Sets the room dimensions in meters and updates the world bounds.
     */
    public void setRoomDimensions(double widthMeters, double depthMeters) {
        this.roomWidthMeters = Math.max(0.1, widthMeters);
        this.roomDepthMeters = Math.max(0.1, depthMeters);
        updateRoomBoundsWorld();
        // TODO: Consider resetting view or adjusting pan/zoom when room changes?
        repaint();
    }

     /** Recalculates room bounds in world pixel coordinates */
     private void updateRoomBoundsWorld() {
         double roomWidthPixels = roomWidthMeters * pixelsPerMeter;
         double roomDepthPixels = roomDepthMeters * pixelsPerMeter;
         // Position room origin at (0,0) in world space for simplicity
         roomBoundsWorld.setRect(0, 0, roomWidthPixels, roomDepthPixels);
         LOGGER.finer("Room bounds (world px) updated: " + roomBoundsWorld);
     }

    /**
     * Sets the pixels-per-meter scale used for drawing the room and potentially items.
     * @param ppm Pixels per meter
     */
    public void setPixelsPerMeter(double ppm) {
        this.pixelsPerMeter = Math.max(1.0, ppm); // Ensure positive scale
        updateRoomBoundsWorld();
        // TODO: Rescale items or adjust view?
        repaint();
    }

    // --- Getters (optional) ---
    public double getRoomWidthMeters() { return roomWidthMeters; }
    public double getRoomDepthMeters() { return roomDepthMeters; }
    public double getPixelsPerMeter() { return pixelsPerMeter; }
    public AffineTransform getViewTransform() { return viewTransform; }

    /**
     * Sets the list of furniture items to be displayed and interacted with.
     * @param items The list of furniture items.
     */
    public void setFurnitureItems(List<FurnitureItem> items) {
        if (items == null) {
             LOGGER.warning("[Canvas] setFurnitureItems called with null list. Clearing items.");
             this.furnitureItems = new ArrayList<>();
             this.selectedItem = null;
        } else {
            LOGGER.finer("[Canvas] setFurnitureItems called with list size: " + items.size()); // DEBUG
            this.furnitureItems = new ArrayList<>(items); // Use a copy
            // Ensure selectedItem reference is updated if the underlying list changes
            if (selectedItem != null) {
                FurnitureItem currentSelection = this.selectedItem;
                this.selectedItem = items.stream()
                                    .filter(item -> item.equals(currentSelection)) // Find item equal to the previous selection
                                    .findFirst()
                                    .orElse(null); // Deselect if not found in new list
                if (this.selectedItem == null) {
                     LOGGER.finer("[Canvas] Previous selected item not found in new list, deselected."); // DEBUG
                } else {
                    LOGGER.finer("[Canvas] Selected item reference updated."); // DEBUG
                }
            } else {
                LOGGER.finer("[Canvas] No item was selected, skipping selection update."); // DEBUG
            }
        }
        repaint();
    }

    /**
     * Sets the listener that will be notified about item updates (selection, transform changes).
     * @param listener The listener object (typically the MainWorkspacePanel).
     */
    public void setItemUpdateListener(ItemUpdateListener listener) {
        this.listener = listener;
    }

    /** Deselects the currently selected item and hides handles. */
    public void deselectItem() {
        if (this.selectedItem != null) {
             this.selectedItem = null;
             if (listener != null) {
                 listener.itemDeselected(); 
             }
             repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create(); // Work on a copy

        // Enable anti-aliasing for smoother graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- Apply View Transform --- 
        // Store original transform from Graphics object
        AffineTransform originalTx = g2d.getTransform();
        // Apply our view transform (pan/zoom)
        g2d.transform(viewTransform);

        // --- Draw Room Bounds (in World Coordinates) --- 
        g2d.setColor(ROOM_FILL_COLOR);
        g2d.fill(roomBoundsWorld);
        g2d.setColor(ROOM_BORDER_COLOR);
        g2d.setStroke(new BasicStroke(1.0f / (float)viewTransform.getScaleX())); // Scale stroke width
        g2d.draw(roomBoundsWorld);
        g2d.setStroke(new BasicStroke(1)); // Reset stroke

        // --- Draw Furniture Items (within the transformed world) ---
        for (FurnitureItem item : furnitureItems) {
            Shape itemShape = item.footprint(); // Footprint is already in world coordinates
            g2d.setColor(item.color());
            g2d.fill(itemShape);
            g2d.setColor(Color.BLACK); // Outline
            // Scale outline stroke width based on view zoom
            g2d.setStroke(new BasicStroke(1.0f / (float)viewTransform.getScaleX())); 
            g2d.draw(itemShape);
        }
         g2d.setStroke(new BasicStroke(1)); // Reset stroke

        // --- Draw Selection Highlight and Handles ---
        if (selectedItem != null) {
            Shape selectedShape = selectedItem.footprint();
            // Scale selection stroke based on view zoom
            float baseDash[] = {5.0f};
            float scaledDash[] = {baseDash[0] / (float)viewTransform.getScaleX()};
            Stroke scaledSelectionStroke = new BasicStroke(2.0f / (float)viewTransform.getScaleX(), 
                                                         BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                                                         10.0f, scaledDash, 0.0f);
            g2d.setColor(SELECTION_COLOR);
            g2d.setStroke(scaledSelectionStroke);
            g2d.draw(selectedShape); // Draw dashed outline
            g2d.setStroke(new BasicStroke(1)); // Reset stroke

            drawSelectionHandles(g2d, selectedShape.getBounds2D());
        }

        // --- Restore Original Transform --- 
        // Before drawing any screen-fixed overlays (like coordinates, HUD)
        g2d.setTransform(originalTx);

        // Example: Draw mouse world coordinates in a corner (screen fixed)
        // drawWorldCoordinates(g2d); 

        g2d.dispose(); // Dispose the graphics copy
    }

    // --- Draw Handles ---
    private void drawSelectionHandles(Graphics2D g2d, Rectangle2D bounds) {
        g2d.setColor(HANDLE_COLOR);
        double x = bounds.getX();
        double y = bounds.getY();
        double w = bounds.getWidth();
        double h = bounds.getHeight();

        // Corner Resize handles
        Rectangle2D[] handles = getHandleRects(bounds);
        for (int i=0; i<4; i++) { // Only draw corner handles (0-3)
            g2d.fill(handles[i]);
            g2d.setColor(Color.BLACK);
            g2d.draw(handles[i]);
            g2d.setColor(HANDLE_COLOR); // Reset color for next fill
        }

        // Rotation Handle (above Top-Right corner)
        Point2D rotationHandleCenter = getRotationHandleCenter(bounds);
        if (rotationHandleCenter != null) {
            double rhX = rotationHandleCenter.getX() - HANDLE_SIZE / 2.0;
            double rhY = rotationHandleCenter.getY() - HANDLE_SIZE / 2.0;
            Shape rotationHandleShape = new Ellipse2D.Double(rhX, rhY, HANDLE_SIZE, HANDLE_SIZE);
            g2d.setColor(ROTATION_HANDLE_COLOR);
            g2d.fill(rotationHandleShape);
            g2d.setColor(Color.BLACK);
            g2d.draw(rotationHandleShape);
            // Optional: Draw line from corner/center to rotation handle
            g2d.drawLine((int)bounds.getMaxX(), (int)bounds.getMinY(), // TR corner
                         (int)rotationHandleCenter.getX(), (int)rotationHandleCenter.getY());
        }
    }

    // --- Calculate Handle Rectangles (Corners only) ---
    private Rectangle2D[] getHandleRects(Rectangle2D bounds) {
        double x = bounds.getX();
        double y = bounds.getY();
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        double halfHandle = HANDLE_SIZE / 2.0;
        Rectangle2D[] handles = new Rectangle2D[4]; // TL, TR, BR, BL
        handles[0] = new Rectangle2D.Double(x - halfHandle, y - halfHandle, HANDLE_SIZE, HANDLE_SIZE); // TL
        handles[1] = new Rectangle2D.Double(x + w - halfHandle, y - halfHandle, HANDLE_SIZE, HANDLE_SIZE); // TR
        handles[2] = new Rectangle2D.Double(x + w - halfHandle, y + h - halfHandle, HANDLE_SIZE, HANDLE_SIZE); // BR
        handles[3] = new Rectangle2D.Double(x - halfHandle, y + h - halfHandle, HANDLE_SIZE, HANDLE_SIZE); // BL
        return handles;
    }

    // --- Calculate Rotation Handle Center ---
    private Point2D getRotationHandleCenter(Rectangle2D bounds) {
        // Position above Top-Right corner
        double handleX = bounds.getMaxX();
        double handleY = bounds.getMinY() - ROTATION_HANDLE_OFFSET;
        // TODO: Consider rotating this handle position based on item rotation for better UX?
        // For now, it stays directly above the TR corner relative to the bounds.
        return new Point2D.Double(handleX, handleY);
    }

    // --- Hit Testing ---
    private FurnitureItem getItemAtPoint(Point2D worldPoint) {
        // Iterate in reverse draw order (topmost first)
        for (int i = furnitureItems.size() - 1; i >= 0; i--) {
            FurnitureItem item = furnitureItems.get(i);
            if (item.footprint().contains(worldPoint)) {
                return item;
            }
        }
        return null;
    }

    private int getHandleAtPoint(Point2D worldPoint) {
        if (selectedItem == null) return -1;
        Rectangle2D bounds = selectedItem.footprint().getBounds2D();

        // Check Rotation Handle First
        Point2D rotationHandleCenter = getRotationHandleCenter(bounds);
        if (rotationHandleCenter != null) {
            Shape rotationHandleShape = new Ellipse2D.Double(
                rotationHandleCenter.getX() - HANDLE_SIZE / 2.0,
                rotationHandleCenter.getY() - HANDLE_SIZE / 2.0,
                HANDLE_SIZE, HANDLE_SIZE);
            if (rotationHandleShape.contains(worldPoint)) {
                return ROTATION_HANDLE_INDEX; // Return 4 for rotation
            }
        }

        // Check Resize Handles (Corners)
        Rectangle2D[] handles = getHandleRects(bounds);
        for (int i = 0; i < handles.length; i++) {
            if (handles[i].contains(worldPoint)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void mouseClicked(MouseEvent e) { } // Use pressed/released

    @Override
    public void mousePressed(MouseEvent e) {
        // Log event details immediately
        LOGGER.info(() -> String.format(
            "mousePressed: Button=%d, ClickCount=%d, Modifiers=%s, CtrlDown=%b, ShiftDown=%b, AltDown=%b, MetaDown=%b, IsRightButton=%b",
            e.getButton(), e.getClickCount(), InputEvent.getModifiersExText(e.getModifiersEx()),
            e.isControlDown(), e.isShiftDown(), e.isAltDown(), e.isMetaDown(), // Check all modifiers
            SwingUtilities.isRightMouseButton(e)
        ));

        lastMousePressScreen = e.getPoint();
        try {
            lastMousePressWorld = viewTransform.inverseTransform(lastMousePressScreen, null);
        } catch (NoninvertibleTransformException ex) {
            LOGGER.log(Level.SEVERE, "View transform non-invertible on mouse press.", ex);
            return;
        }

        activeHandle = -1;

        if (SwingUtilities.isRightMouseButton(e) || e.isControlDown()) {
            currentMode = InteractionMode.PANNING;
            initialPanTranslation = new Point2D.Double(viewTransform.getTranslateX(), viewTransform.getTranslateY());
            originalViewTransformState = new AffineTransform(viewTransform); // Store current view transform state
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            LOGGER.finer("Mouse pressed: Starting PANNING mode.");
            return;
        }

        if (selectedItem != null) {
            activeHandle = getHandleAtPoint(lastMousePressWorld);
            if (activeHandle != -1) {
                originalItemState = selectedItem;
                if (activeHandle == ROTATION_HANDLE_INDEX) {
                    currentMode = InteractionMode.ROTATING;
                    Point2D itemCenter = getShapeCenter(originalItemState.footprint());
                    lastDragAngle = Math.atan2(lastMousePressWorld.getY() - itemCenter.getY(),
                                              lastMousePressWorld.getX() - itemCenter.getX());
                    LOGGER.finer("Mouse pressed on ROTATION handle. Mode: ROTATING.");
                } else {
                    currentMode = InteractionMode.RESIZING;
                    LOGGER.finer("Mouse pressed on RESIZE handle " + activeHandle + ". Mode: RESIZING.");
                }
                setCursorForHandle(activeHandle);
                repaint();
                return;
            }
        }

        FurnitureItem itemUnderMouse = getItemAtPoint(lastMousePressWorld);
        if (itemUnderMouse != null) {
            if (selectedItem != itemUnderMouse) {
                selectItem(itemUnderMouse);
            }
            currentMode = InteractionMode.MOVING;
            originalItemState = selectedItem;
            this.lastMouseDragWorld = lastMousePressWorld;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            LOGGER.finer("Mouse pressed on item " + selectedItem.type() + ". Mode: MOVING.");
        } else {
            if (selectedItem != null) {
                deselectItem();
            }
            currentMode = InteractionMode.NONE;
            LOGGER.finer("Mouse pressed on empty space. Mode: NONE.");
        }
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (lastMousePressWorld == null && currentMode != InteractionMode.PANNING) { // Allow panning even if lastMousePressWorld is null (e.g. if press was outside)
             if (lastMousePressScreen == null) return; // But screen press is essential for panning delta
        }

        Point2D currentMouseScreen = e.getPoint();
        Point2D currentMouseWorld = null; // Initialize to null
        try {
            if (viewTransform != null && !viewTransform.isIdentity() && Math.abs(viewTransform.getDeterminant()) > 1e-9) {
                 currentMouseWorld = viewTransform.inverseTransform(currentMouseScreen, null);
            } else if (viewTransform != null && viewTransform.isIdentity()) {
                 currentMouseWorld = currentMouseScreen; // If identity, world is screen
            } else {
                // Transform is null or singular, cannot get world coords reliably for item interactions
                // For panning, we primarily use screen coords, so this might be less critical
                LOGGER.finer("View transform is null or singular in mouseDragged, world coords not calculated.");
            }
        } catch (NoninvertibleTransformException ex) {
            LOGGER.log(Level.WARNING, "View transform non-invertible on mouse drag.", ex);
            // currentMouseWorld remains null
        }

        if (currentMode == InteractionMode.PANNING) {
            if (originalViewTransformState != null && lastMousePressScreen != null) { 
                double dxScreen = currentMouseScreen.getX() - lastMousePressScreen.getX();
                double dyScreen = currentMouseScreen.getY() - lastMousePressScreen.getY();
                
                // Start with the transform state from when the pan began
                viewTransform.setTransform(originalViewTransformState);
                
                // Prepend the current screen delta. 
                // viewTransform becomes T_delta * originalViewTransformState
                viewTransform.preConcatenate(AffineTransform.getTranslateInstance(dxScreen, dyScreen));
                
                LOGGER.finer(() -> String.format(
                    "PANNING (New Logic): lastPressSc(%.1f,%.1f), currSc(%.1f,%.1f) -> dSc(%.1f,%.1f) | " +
                    "origVT: %s | final VT: %s",
                    lastMousePressScreen.getX(), lastMousePressScreen.getY(),
                    currentMouseScreen.getX(), currentMouseScreen.getY(),
                    dxScreen, dyScreen,
                    originalViewTransformState.toString(),
                    viewTransform.toString()
                ));
                repaint();
            }
        } else if (currentMode == InteractionMode.MOVING) {
            if (selectedItem != null && lastMouseDragWorld != null) { 
                double dxWorld = currentMouseWorld.getX() - lastMouseDragWorld.getX();
                double dyWorld = currentMouseWorld.getY() - lastMouseDragWorld.getY();

                FurnitureItem movedItem = selectedItem.translateTo(
                    selectedItem.getTx() + dxWorld,
                    selectedItem.getTy() + dyWorld
                );
                if (listener != null) {
                    listener.itemUpdated(movedItem);
                    LOGGER.finest(() -> String.format("Moving item: %s to (%.2f, %.2f)", movedItem.type(), movedItem.getTx(), movedItem.getTy()));
                }
            }
        } else if (currentMode == InteractionMode.RESIZING) {
            if (selectedItem != null && originalItemState != null && activeHandle != -1) {
                Point2D anchor = getResizeAnchor(activeHandle, originalItemState.footprint().getBounds2D());
                if (anchor == null) return;

                double currentItemRotation = originalItemState.getRotationRadians();
                AffineTransform rotation = AffineTransform.getRotateInstance(currentItemRotation, anchor.getX(), anchor.getY());
                AffineTransform inverseRotation;
                try {
                    inverseRotation = rotation.createInverse();
                } catch (NoninvertibleTransformException ex) {
                    LOGGER.log(Level.WARNING, "Cannot invert rotation for resize, skipping.", ex);
                    return;
                }

                Point2D rotatedCurrentMouse = inverseRotation.transform(currentMouseWorld, null);
                Point2D rotatedAnchor = inverseRotation.transform(anchor, null);

                double baseItemWidth = originalItemState.getBaseFootprint().getBounds2D().getWidth();
                double baseItemHeight = originalItemState.getBaseFootprint().getBounds2D().getHeight();
                if (baseItemWidth == 0 || baseItemHeight == 0) return; 

                double newWidthBasedOnMouse = 0;
                double newHeightBasedOnMouse = 0;

                if (activeHandle == 0) { // TL
                    newWidthBasedOnMouse = rotatedAnchor.getX() - rotatedCurrentMouse.getX();
                    newHeightBasedOnMouse = rotatedAnchor.getY() - rotatedCurrentMouse.getY();
                } else if (activeHandle == 1) { // TR
                    newWidthBasedOnMouse = rotatedCurrentMouse.getX() - rotatedAnchor.getX();
                    newHeightBasedOnMouse = rotatedAnchor.getY() - rotatedCurrentMouse.getY();
                } else if (activeHandle == 2) { // BR : Anchor is TL
                    newWidthBasedOnMouse = rotatedCurrentMouse.getX() - rotatedAnchor.getX();
                    newHeightBasedOnMouse = rotatedCurrentMouse.getY() - rotatedAnchor.getY();
                } else if (activeHandle == 3) { // BL : Anchor is TR
                    newWidthBasedOnMouse = rotatedAnchor.getX() - rotatedCurrentMouse.getX();
                    newHeightBasedOnMouse = rotatedCurrentMouse.getY() - rotatedAnchor.getY();
                }
                
                newWidthBasedOnMouse = Math.max(newWidthBasedOnMouse, HANDLE_SIZE / viewTransform.getScaleX()); 
                newHeightBasedOnMouse = Math.max(newHeightBasedOnMouse, HANDLE_SIZE / viewTransform.getScaleY());

                double newScaleX = newWidthBasedOnMouse / baseItemWidth;
                double newScaleY = newHeightBasedOnMouse / baseItemHeight;

                FurnitureItem resizedItem = originalItemState.setScale(newScaleX, newScaleY);

                if (listener != null) {
                    listener.itemUpdated(resizedItem);
                     LOGGER.finest(() -> String.format("Resizing item: %s to scale (%.2f, %.2f)", resizedItem.type(), resizedItem.getScaleX(), resizedItem.getScaleY()));
                }
            }
        } else if (currentMode == InteractionMode.ROTATING) {
            if (selectedItem != null && originalItemState != null) {
                Point2D itemCenter = getShapeCenter(originalItemState.footprint()); 
                double newAbsoluteAngle = Math.atan2(currentMouseWorld.getY() - itemCenter.getY(),
                                                     currentMouseWorld.getX() - itemCenter.getX());
                FurnitureItem trulyRotatedItem = originalItemState.setRotation(newAbsoluteAngle);
                if (listener != null) {
                    listener.itemUpdated(trulyRotatedItem);
                     LOGGER.finest(() -> String.format("Rotating item: %s to angle %.2f rad", trulyRotatedItem.type(), trulyRotatedItem.getRotationRadians()));
                }
            }
        }
        
        if (currentMouseWorld != null) { // Only update lastMouseDragWorld if successfully calculated
            this.lastMouseDragWorld = currentMouseWorld;
        }
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (currentMode == InteractionMode.MOVING || currentMode == InteractionMode.RESIZING || currentMode == InteractionMode.ROTATING) {
             LOGGER.log(Level.FINER, "Interaction ended: " + currentMode);
             // Optional: Final notification if needed, but updates are live during drag
        }
        currentMode = InteractionMode.NONE;
        activeHandle = -1;
        originalItemState = null;
        lastMousePressScreen = null;
        lastMousePressWorld = null;
        lastMouseDragWorld = null; // Reset last drag world pos
        lastDragAngle = 0.0; // Reset last drag angle
    }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }

    @Override
    public void mouseMoved(MouseEvent e) { } // Handle hover effects here if needed

    // --- Mouse Wheel Listener for Zooming ---
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double scroll = e.getPreciseWheelRotation();
        double scaleFactor = Math.pow(1.1, -scroll); // Adjust sensitivity here (1.1)

        // Get the current scale
        double currentScale = viewTransform.getScaleX(); // Assuming uniform scale
        double newScale = currentScale * scaleFactor;
        // Clamp scale factor
        newScale = Math.max(0.1, Math.min(newScale, 10.0)); // Clamp scale (e.g., 0.1x to 10x)
        // Recalculate the actual scale factor applied after clamping
        scaleFactor = newScale / currentScale;
        if (Math.abs(scaleFactor - 1.0) < 1e-6) {
             return; // No significant change
        }

        // Point to zoom around (center of the room in world coordinates)
        Point2D zoomCenterWorld = new Point2D.Double(roomBoundsWorld.getCenterX(), roomBoundsWorld.getCenterY());
        // Screen point corresponding to the zoom center *before* scaling
        Point2D zoomCenterScreenPre = viewTransform.transform(zoomCenterWorld, null);

        // Apply the scaling part of the transform centered on the world point
        AffineTransform scaleTx = AffineTransform.getTranslateInstance(zoomCenterWorld.getX(), zoomCenterWorld.getY());
        scaleTx.scale(scaleFactor, scaleFactor);
        scaleTx.translate(-zoomCenterWorld.getX(), -zoomCenterWorld.getY());
        viewTransform.preConcatenate(scaleTx);

        // Find where the zoom center is *now* on the screen
        Point2D zoomCenterScreenPost = viewTransform.transform(zoomCenterWorld, null);

        // Calculate the screen delta needed to bring the post-zoom center back to the pre-zoom position
        double deltaX = zoomCenterScreenPre.getX() - zoomCenterScreenPost.getX();
        double deltaY = zoomCenterScreenPre.getY() - zoomCenterScreenPost.getY();

        // Apply a corrective translation to the view transform
        AffineTransform translateTx = AffineTransform.getTranslateInstance(deltaX, deltaY);
        viewTransform.preConcatenate(translateTx);

        repaint();
    }

    // Helper to select an item and notify listener
    private void selectItem(FurnitureItem item) {
        if (this.selectedItem != item) {
            this.selectedItem = item;
            if (listener != null) {
                listener.itemSelected(item);
            }
            repaint();
        }
    }

    // Helper to get the world coordinates of the opposite corner handle
    private Point2D getResizeAnchor(int handleIndex, Rectangle2D bounds) {
        double x = bounds.getX();
        double y = bounds.getY();
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        switch (handleIndex) {
            case 0: return new Point2D.Double(x + w, y + h); // TL -> BR
            case 1: return new Point2D.Double(x, y + h);     // TR -> BL
            case 2: return new Point2D.Double(x, y);         // BR -> TL
            case 3: return new Point2D.Double(x + w, y);     // BL -> TR
            default: return null; // Should not happen for corner handles 0-3
        }
    }

    // Helper to get the world coordinates of a specific handle based on index
    private Point2D getHandleWorldPosition(int handleIndex, Rectangle2D bounds) {
        double x = bounds.getX();
        double y = bounds.getY();
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        switch (handleIndex) {
            case 0: return new Point2D.Double(x, y); // TL
            case 1: return new Point2D.Double(x + w, y); // TR
            case 2: return new Point2D.Double(x + w, y + h); // BR
            case 3: return new Point2D.Double(x, y + h); // BL
            // Add cases for side handles or rotation handle if needed
            default: return null;
        }
    }

    // Helper to compare footprints, accounting for potential floating point inaccuracies
    private boolean footprintsEffectivelyEqual(Shape s1, Shape s2) {
        if (s1 == null || s2 == null) return s1 == s2;
        Rectangle2D b1 = s1.getBounds2D();
        Rectangle2D b2 = s2.getBounds2D();
        double tolerance = 1e-4;
        return Math.abs(b1.getX() - b2.getX()) < tolerance &&
               Math.abs(b1.getY() - b2.getY()) < tolerance &&
               Math.abs(b1.getWidth() - b2.getWidth()) < tolerance &&
               Math.abs(b1.getHeight() - b2.getHeight()) < tolerance;
    }

    // Helper to get the center of a shape's bounds
    private Point2D getShapeCenter(Shape shape) {
        if (shape == null) return null;
        Rectangle2D bounds = shape.getBounds2D();
        return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
    }

    /** Centers the view on the current room bounds with some padding. */
    public void centerViewOnRoom() {
        if (getWidth() <= 0 || getHeight() <= 0 || roomBoundsWorld.isEmpty()) {
            LOGGER.warning("Cannot center view: Canvas or room size is zero or room bounds not set.");
            return; // Cannot center if canvas or room size is unknown
        }

        // Target zoom level (fit room to view with some padding)
        double paddingFactor = 0.9; // Show 90% of the room initially
        double scaleX = (getWidth() * paddingFactor) / roomBoundsWorld.getWidth();
        double scaleY = (getHeight() * paddingFactor) / roomBoundsWorld.getHeight();
        double newScale = Math.min(scaleX, scaleY); // Use smaller scale to fit both dimensions
        newScale = Math.max(0.1, Math.min(newScale, 5.0)); // Clamp scale

        // Calculate pan needed to center the room's center point in the view
        double roomCenterXWorld = roomBoundsWorld.getCenterX();
        double roomCenterYWorld = roomBoundsWorld.getCenterY();

        // Target screen center
        double screenCenterX = getWidth() / 2.0;
        double screenCenterY = getHeight() / 2.0;

        // Calculate required translation
        // Formula: screen = (world * scale) + pan => pan = screen - (world * scale)
        // Since our transform applies pan first then scale (conceptually), we need to find the correct pan values
        // T_final = T_pan * T_scale
        // Let C_screen = (screenCenterX, screenCenterY)
        // Let C_world = (roomCenterXWorld, roomCenterYWorld)
        // We want C_screen = viewTransform * C_world
        // C_screen = (C_world.x * scale + panX, C_world.y * scale + panY) for a simple scale/pan
        // For AffineTransform: T_final applies scale around origin then translates.
        // screenX = worldX * scaleX + transX
        // screenY = worldY * scaleY + transY
        double newTranslateX = screenCenterX - (roomCenterXWorld * newScale);
        double newTranslateY = screenCenterY - (roomCenterYWorld * newScale);

        // Reset the transform and apply the calculated scale and translation
        viewTransform.setToIdentity();
        viewTransform.translate(newTranslateX, newTranslateY);
        viewTransform.scale(newScale, newScale);

        LOGGER.info(String.format("Centering View: Scale=%.2f, TranslateX=%.1f, TranslateY=%.1f%n",
                           viewTransform.getScaleX(), viewTransform.getTranslateX(), viewTransform.getTranslateY()));

        repaint();
    }

    // Modified method to always indicate aspect ratio is not locked
    private boolean isAspectRatioLocked() {
        return false; // Aspect ratio is never locked now
    }

    // Added placeholder method for setting cursor based on handle
    private void setCursorForHandle(int handleIndex) {
        // TODO: Implement cursor changes based on handle index (e.g., NW_RESIZE_CURSOR)
        // For now, just a default resize cursor or based on rotation handle
        if (handleIndex == ROTATION_HANDLE_INDEX) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Or a custom rotation cursor
        } else if (handleIndex >= 0 && handleIndex < 4) {
            // Basic resize cursor for now, could be more specific (NW, NE, SW, SE)
            setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR)); 
        }
    }
}