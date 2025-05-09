package com.furnitureapp.ui;

import com.furnitureapp.model.FurnitureItem;
import com.furnitureapp.model.GeometryData;
import com.furnitureapp.util.ModelHelper;
import com.furnitureapp.util.ModelHelper.ModelLoadResult;
// JavaFX imports needed
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.LightBase;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.logging.Logger; // Added Logger import
import java.util.logging.Level; // Added Level import
import java.awt.geom.Point2D; // Added missing import
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import javax.swing.BoxLayout; // Added import

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.util.Map; // Added import
import java.util.HashMap; // Added import
import javax.swing.JToggleButton; // Added import
import com.furnitureapp.io.ColorData;
import com.furnitureapp.io.ItemData;
import com.furnitureapp.io.LayoutData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature; // For pretty printing
import java.io.IOException; // Added import
import java.util.stream.IntStream; // Added import

/**
 * The main workspace panel holding the drawing canvas, toolbars, object list, and other controls.
 */
public class MainWorkspacePanel extends JPanel implements ItemUpdateListener {

    private static final Logger LOGGER = Logger.getLogger(MainWorkspacePanel.class.getName()); // Added Logger instance
    private static final String MODELS_DIR = "assets/objects"; // Assuming OBJs are here too
    // private static final double DPAD_PAN_AMOUNT = 20.0; // DPad controls removed/disabled

    private DrawingCanvas drawingCanvas;
    private JToolBar topToolBar;
    private JList<String> objFileList;
    private DefaultListModel<String> objListModel;
    private JScrollPane listScrollPane;
    private JPanel controlPanel; // Panel for room controls etc.
    private JSpinner roomWidthSpinner;
    private JSpinner roomDepthSpinner;
    private JSpinner wallHeightSpinner;
    private JTabbedPane centerTabbedPane; // Add this field
    // private JPanel viewControlPanel; // Removed: Panel for zoom/pan buttons
    private DPadControlPanel dPadControlPanel; // Added DPad Panel
    private FurnitureItemListPanel furnitureItemListPanel;
    private InfoPanel infoPanel;
    private JFileChooser layoutFileChooser; // Renamed/repurposed file chooser
    private JFXPanel jfxPanel; // Added JavaFX panel
    private Furniture3DView furniture3DView; // Added JavaFX 3D view component

    private double roomWidthMeters = 5.0;
    private double roomDepthMeters = 4.0; // Represents depth in 3D
    private double wallHeightMeters = 2.5; // Added wall height

    // --- State Management ---
    private List<FurnitureItem> furnitureItems;
    private FurnitureItem selectedItem; // Keep track of the selected item centrally
    private Map<String, ModelLoadResult> itemModelCache; // Added itemModelCache

    private JPanel wallColorChooserPanel; // *** RENAMED *** Added panel for wall colors
    private JButton[] wallColorButtons = new JButton[4]; // N, E, S, W
    private java.awt.Color[] wallColors = { // Default wall colors - USE AWT COLOR
        java.awt.Color.LIGHT_GRAY, // North
        java.awt.Color.LIGHT_GRAY, // East
        java.awt.Color.LIGHT_GRAY, // South
        java.awt.Color.LIGHT_GRAY  // West
    };

    private JButton zoomInButton; // Added
    private JButton zoomOutButton; // Added
    private JToggleButton toggleLightingButton; // Added
    private ObjectMapper objectMapper; // Jackson object mapper

    public MainWorkspacePanel() {
        // IMPORTANT: Initialize the JavaFX toolkit
        // This is often done implicitly by JFXPanel, but explicit init is safer
        // Consider moving this to App.main if multiple JFXPanels are used.
        Platform.startup(() -> {});

        itemModelCache = new HashMap<>(); // Initialize cache
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Make JSON readable
        initComponents();
        layoutComponents();
        attachListeners();
        loadObjFiles();
    }

    private void initComponents() {
        drawingCanvas = new DrawingCanvas();
        centerTabbedPane = new JTabbedPane(); // Initialize the TabbedPane
        furnitureItemListPanel = new FurnitureItemListPanel();
        infoPanel = new InfoPanel();
        layoutFileChooser = new JFileChooser(); // Use one chooser for layout files
        layoutFileChooser.setFileFilter(new FileNameExtensionFilter("Furniture Layout Files (*.furn)", "furn"));
        layoutFileChooser.setAcceptAllFileFilterUsed(false);
        jfxPanel = new JFXPanel(); // Initialize the JFXPanel

        // Initialize furniture list before setting up views
        furnitureItems = new ArrayList<>();
        selectedItem = null;

        // Setup DrawingCanvas
        drawingCanvas.setRoomDimensions(roomWidthMeters, roomDepthMeters);
        drawingCanvas.setFurnitureItems(furnitureItems);
        drawingCanvas.setItemUpdateListener(this);

        // Initialize JavaFX content on the FX thread
        Platform.runLater(() -> initFX(jfxPanel));

        // --- Toolbar Setup --- 
        topToolBar = new JToolBar();
        topToolBar.setFloatable(false);

        JButton addSelectedButton = new JButton("Add Selected");
        JButton clearButton = new JButton("Clear");

        topToolBar.add(addSelectedButton);
        topToolBar.addSeparator();
        topToolBar.add(clearButton);
        topToolBar.addSeparator(); 
        JButton refresh3DButton = new JButton("Refresh 3D"); 
        topToolBar.add(refresh3DButton);

        // --- Add Zoom Buttons --- 
        topToolBar.addSeparator();
        zoomInButton = new JButton("Zoom In");
        topToolBar.add(zoomInButton);
        zoomOutButton = new JButton("Zoom Out");
        topToolBar.add(zoomOutButton);
        // --- End Zoom Buttons --- 

        // --- Add Lighting Toggle Button --- 
        topToolBar.addSeparator();
        toggleLightingButton = new JToggleButton("Enable Depth Light");
        toggleLightingButton.setSelected(false); // Initial state matches Furniture3DView
        topToolBar.add(toggleLightingButton);
        // --- End Lighting Toggle Button --- 

        // --- Add Save/Load Buttons to Toolbar --- 
        topToolBar.addSeparator();
        JButton saveButton = new JButton("Save Layout");
        topToolBar.add(saveButton);
        JButton loadButton = new JButton("Load Layout");
        topToolBar.add(loadButton);

        // --- OBJ File List Setup ---
        objListModel = new DefaultListModel<>();
        objFileList = new JList<>(objListModel);
        objFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        objFileList.setVisibleRowCount(10); // Show reasonable number of items
        listScrollPane = new JScrollPane(objFileList);
        listScrollPane.setPreferredSize(new Dimension(150, 0)); // Set preferred width

        // --- Control Panel Setup (Room Dimensions) ---
        controlPanel = new JPanel();
        controlPanel.setLayout(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Room"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Room Width
        gbc.gridx = 0; gbc.gridy = 0;
        controlPanel.add(new JLabel("Width (m):"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        // Spinner model: initial value 5.0, min 0.1, max 100.0, step 0.1
        roomWidthSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.1, 100.0, 0.1));
        controlPanel.add(roomWidthSpinner, gbc);

        // Room Depth
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        controlPanel.add(new JLabel("Depth (m):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        roomDepthSpinner = new JSpinner(new SpinnerNumberModel(4.0, 0.1, 100.0, 0.1));
        controlPanel.add(roomDepthSpinner, gbc);

        // Wall Height
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        controlPanel.add(new JLabel("Height (m):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        wallHeightSpinner = new JSpinner(new SpinnerNumberModel(wallHeightMeters, 0.1, 20.0, 0.1)); // Added spinner
        controlPanel.add(wallHeightSpinner, gbc);

        // --- Color Selection Panel --- 
        wallColorChooserPanel = new JPanel(); 
        wallColorChooserPanel.setLayout(new GridLayout(4, 1, 5, 5)); 
        wallColorChooserPanel.setBorder(BorderFactory.createTitledBorder("Wall Colors"));
        String[] wallNames = {"North Wall", "East Wall", "South Wall", "West Wall"};
        for (int i = 0; i < 4; i++) {
            wallColorButtons[i] = new JButton(wallNames[i]);
            wallColorButtons[i].setBackground(wallColors[i]); // Uses AWT Color
            wallColorButtons[i].setForeground(getContrastColor(wallColors[i])); // Pass AWT Color
            int wallIndex = i; 
            wallColorButtons[i].addActionListener(e -> chooseWallColor(wallIndex));
            wallColorChooserPanel.add(wallColorButtons[i]); 
        }

        // Add empty component to push controls up
        gbc.gridx = 0; gbc.gridy = 3; gbc.weighty = 1.0;
        controlPanel.add(new JPanel(), gbc);

        // --- DPad Control Panel Setup ---
        dPadControlPanel = new DPadControlPanel();
    }

    /**
     * Initializes the JavaFX components. Must be called on the JavaFX Application Thread.
     */
    private void initFX(JFXPanel fxPanel) {
        // Still create the view instance to check its constructor
        furniture3DView = new Furniture3DView(); 
        
        // --- Minimal Scene Setup (COMMENTED OUT) --- 
        /*
        StackPane simpleRoot = new StackPane();
        simpleRoot.setStyle("-fx-background-color: lightgrey;"); // Add background for visibility
        Scene simpleScene = new Scene(simpleRoot, 600, 400); 
        fxPanel.setScene(simpleScene);
        LOGGER.info("TRACE: initFX completed with minimal scene.");
        */

        // --- Original complex setup (NOW ENABLED) ---
        PerspectiveCamera mainCamera = furniture3DView.getMainCamera();

        // Create SubScene for the main 3D content
        SubScene mainSubScene = new SubScene(
                furniture3DView.getMainSceneRoot(), 
                600, // Initial width 
                400, // Initial height
                true, 
                SceneAntialiasing.BALANCED
        );
        mainSubScene.setFill(Color.LIGHTSKYBLUE); // Set background color
        mainSubScene.setCamera(mainCamera);

        // Create compass node and a simple camera for it
        Parent compassNode = furniture3DView.createCompass();
        PerspectiveCamera compassCamera = new PerspectiveCamera(); // Simple fixed camera

        // Create SubScene for the compass overlay
        SubScene compassSubScene = new SubScene(compassNode, 80, 80, true, SceneAntialiasing.BALANCED);
        compassSubScene.setFill(Color.TRANSPARENT); // Transparent background
        compassSubScene.setCamera(compassCamera);

        // Use StackPane to layer the compass on top of the main scene
        StackPane layoutPane = new StackPane();
        layoutPane.getChildren().addAll(mainSubScene, compassSubScene);
        StackPane.setAlignment(compassSubScene, Pos.BOTTOM_LEFT); // Position compass
        StackPane.setMargin(compassSubScene, new javafx.geometry.Insets(10));

        // Bind main SubScene size to StackPane size (optional, good for resizing)
        mainSubScene.widthProperty().bind(layoutPane.widthProperty());
        mainSubScene.heightProperty().bind(layoutPane.heightProperty());

        // Add listeners to update camera positioning when SubScene resizes
        mainSubScene.widthProperty().addListener((obs, oldVal, newVal) -> furniture3DView.updateCameraPositioning());
        mainSubScene.heightProperty().addListener((obs, oldVal, newVal) -> furniture3DView.updateCameraPositioning());

        // Create the final Scene with the layout pane
        Scene finalScene = new Scene(layoutPane);
        fxPanel.setScene(finalScene);

        // Initial setup for items/dimensions
        // Note: Furniture3DView doesn't have these setters anymore, internal state relies on setItems
        // furniture3DView.setRoomDimensions(roomWidthMeters, roomDepthMeters); 
        // furniture3DView.setPixelsPerMeter(drawingCanvas.getPixelsPerMeter()); 
        // Initial items load will happen via update3DView when tab is selected or refreshed
        // furniture3DView.setItems(new ArrayList<>(furnitureItems)); 
        LOGGER.info("initFX completed with full 3D scene setup.");
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(5, 5));

        // West Panel: OBJ List, Room Controls, Color Controls
        JPanel westPanel = new JPanel(new BorderLayout(0, 5)); // Use BorderLayout
        westPanel.add(listScrollPane, BorderLayout.CENTER); // List expands vertically

        // Panel to group controls at the bottom of westPanel
        JPanel bottomControls = new JPanel();
        bottomControls.setLayout(new BoxLayout(bottomControls, BoxLayout.Y_AXIS)); // Stack vertically
        
        // Ensure controlPanel is added
        if (controlPanel != null) {
            controlPanel.setAlignmentX(Component.LEFT_ALIGNMENT); 
            bottomControls.add(controlPanel);
        } else {
             LOGGER.warning("controlPanel is null during layout!");
        }
        
        // Ensure wallColorChooserPanel is added
        if (wallColorChooserPanel != null) {
             wallColorChooserPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
             bottomControls.add(wallColorChooserPanel); 
        } else {
             LOGGER.warning("wallColorChooserPanel is null during layout!");
        }
        
        westPanel.add(bottomControls, BorderLayout.SOUTH); // Add grouped controls to the bottom

        // East Panel: Item List (DPad removed/commented)
        JPanel eastPanel = new JPanel(new BorderLayout(0, 5));
        eastPanel.add(furnitureItemListPanel, BorderLayout.CENTER);
        // eastPanel.add(dPadControlPanel, BorderLayout.SOUTH); 

        // Center: Tabbed Pane with 2D and 3D views
        centerTabbedPane.addTab("2D Design", drawingCanvas);
        centerTabbedPane.addTab("3D Preview", jfxPanel);

        add(topToolBar, BorderLayout.NORTH);
        add(westPanel, BorderLayout.WEST);
        add(centerTabbedPane, BorderLayout.CENTER);
        add(eastPanel, BorderLayout.EAST);
        add(infoPanel, BorderLayout.SOUTH);
    }

    private void attachListeners() {
        // Set the listener for drawing canvas events
        drawingCanvas.setItemUpdateListener(this);

        // Add listener to center view when canvas is first shown/resized
        drawingCanvas.addComponentListener(new ComponentAdapter() {
            private boolean initialCenterDone = false;
            @Override
            public void componentResized(ComponentEvent e) {
                if (!initialCenterDone && drawingCanvas.getWidth() > 0 && drawingCanvas.getHeight() > 0) {
                    // drawingCanvas.resetView(); // REMOVED - Method doesn't exist
                    initialCenterDone = true;
                    LOGGER.info("DrawingCanvas resized, initial centering performed (ResetView call removed)."); // Use LOGGER
                    // Optionally remove listener if only needed once:
                    // drawingCanvas.removeComponentListener(this);
                }
            }
            // Might also need componentShown if resize isn't triggered reliably on first display
        });

        // Clear button listener
        findButton(topToolBar, "Clear").addActionListener(e -> clearCanvas());

        // Add Selected button listener
        findButton(topToolBar, "Add Selected").addActionListener(e -> addSelectedItemToCanvas());

        // Room dimension spinner listeners
        roomWidthSpinner.addChangeListener(e -> updateRoomDimensionsState());
        roomDepthSpinner.addChangeListener(e -> updateRoomDimensionsState());
        wallHeightSpinner.addChangeListener(e -> updateRoomDimensionsState());
        
        // Refresh 3D button listener - Re-enabled
        JButton refreshButton = findButton(topToolBar, "Refresh 3D");
        if (refreshButton != null) { 
            refreshButton.setEnabled(true); // Re-enable it
            refreshButton.addActionListener(e -> refresh3DViewCompletely()); // Add new listener
        }

        // Listener for InfoPanel's Change Color button
        infoPanel.setChangeColorActionListener(e -> handleChangeItemColor());

        // Listener for InfoPanel's Scale spinner
        infoPanel.addScaleChangeListener(e -> handleScaleChange());

        // Listener for Tab Changes - Start/Stop Animation & Refresh Dimensions/Colors
        centerTabbedPane.addChangeListener(e -> {
            Component selectedComponent = centerTabbedPane.getSelectedComponent();
            if (selectedComponent == jfxPanel) {
                LOGGER.info("Switched to 3D View Tab - Starting animation.");
                // Update dimensions/colors when switching TO 3D tab
                sendDimensionsAndColorsTo3DView(); // Use new separate method
                // Items are now added/updated individually, no full refresh needed here
                if (furniture3DView != null) {
                    Platform.runLater(() -> furniture3DView.startRotationAnimation());
                }
            } else {
                 LOGGER.info("Switched away from 3D View Tab - Stopping animation.");
                 if (furniture3DView != null) {
                     Platform.runLater(() -> furniture3DView.stopRotationAnimation());
                 }
            }
        });

        // --- Add Listeners for Zoom Buttons --- 
        zoomInButton.addActionListener(e -> handleZoomIn3D());
        zoomOutButton.addActionListener(e -> handleZoomOut3D());

        // --- Add Listener for Lighting Toggle Button --- 
        toggleLightingButton.addActionListener(e -> {
            boolean enableDepth = toggleLightingButton.isSelected();
            toggleLightingButton.setText(enableDepth ? "Disable Depth Light" : "Enable Depth Light");
            if (furniture3DView != null) {
                Platform.runLater(() -> furniture3DView.toggleDepthLighting(enableDepth));
                LOGGER.fine("Toggle Depth Lighting requested: " + enableDepth);
            } else {
                 LOGGER.warning("Cannot toggle depth lighting: furniture3DView is null.");
            }
        });

        // --- Add Listeners for Save/Load Buttons --- 
        findButton(topToolBar, "Save Layout").addActionListener(e -> saveLayout());
        findButton(topToolBar, "Load Layout").addActionListener(e -> loadLayout());
    }

    /**
     * Helper to find button by text within a container (works for JPanel now).
     */
    private JButton findButton(Container container, String text) {
         for (Component comp : container.getComponents()) {
            if (comp instanceof JButton && ((JButton) comp).getText().equals(text)) {
                return (JButton) comp;
            }
        }
        return null; // Should not happen in this setup
    }

    /**
     * Scans the MODELS_DIR for .obj files and populates the JList.
     */
    private void loadObjFiles() {
        objListModel.clear();
        File dir = new File(MODELS_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            LOGGER.severe("Error: Models directory not found or not a directory: " + dir.getAbsolutePath());
            objListModel.addElement("Error: Dir not found");
            return;
        }

        File[] modelFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".obj")); // Look for .obj

        if (modelFiles == null || modelFiles.length == 0) {
            objListModel.addElement("No .obj files found"); // Updated message
            return;
        }

        Arrays.stream(modelFiles)
              .map(File::getName)
              .sorted()
              .forEach(objListModel::addElement);
        
        if(!objListModel.isEmpty()){
            objFileList.setSelectedIndex(0); 
        }
    }

    /**
     * Loads the selected .obj file, creates FurnitureItem & GeometryData,
     * adds item to list, and tells 3D view to create node.
     */
    private void addSelectedItemToCanvas() {
        String selectedFilename = objFileList.getSelectedValue();
        if (selectedFilename == null || selectedFilename.startsWith("Error:") || selectedFilename.equals("No .obj files found")) {
            JOptionPane.showMessageDialog(this, "Please select a valid .obj file from the list.", "No File Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fullPath = MODELS_DIR + File.separator + selectedFilename;
        
        ModelLoadResult result = itemModelCache.computeIfAbsent(fullPath, k -> 
            ModelHelper.loadModelDataFromObj(k, java.awt.Color.CYAN)
        );

        if (result != null && result.item != null && result.geometry != null) {
            FurnitureItem newItemTemplate = result.item;
            GeometryData newGeometry = result.geometry;

            Point2D viewCenterWorld = screenToWorld(new Point(drawingCanvas.getWidth() / 2, drawingCanvas.getHeight() / 2));
            FurnitureItem positionedItem = newItemTemplate.translateTo(viewCenterWorld.getX(), viewCenterWorld.getY());
            
            addFurnitureItem(positionedItem); 

            if (furniture3DView != null) {
                final FurnitureItem itemToAdd = positionedItem;
                final GeometryData geomToAdd = newGeometry;
                Platform.runLater(() -> furniture3DView.addOrUpdateItemNode(itemToAdd, geomToAdd));
            }
             updateSaveState(true);
             updateUndoRedoState();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to load model data for: " + selectedFilename, "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Helper to convert screen to world coords (needs canvas transform)
    private Point2D screenToWorld(Point screenPoint) {
        if (drawingCanvas == null) {
            LOGGER.warning("Attempted screenToWorld conversion before DrawingCanvas is initialized.");
            return screenPoint; // Cannot convert yet
        }
        AffineTransform vt = drawingCanvas.getViewTransform();
        if (vt == null) {
            LOGGER.warning("DrawingCanvas viewTransform is null during screenToWorld conversion.");
            return screenPoint; // Cannot convert
        }
        try {
            return vt.inverseTransform(screenPoint, null);
        } catch (NoninvertibleTransformException e) {
            LOGGER.log(Level.SEVERE, "View transform non-invertible during screenToWorld conversion.", e);
            return screenPoint; // Fallback on error
        }
        // LOGGER.warning("screenToWorld conversion in MainWorkspacePanel is approximate (needs DrawingCanvas.getViewTransform())");
        // return screenPoint; // Placeholder
    }

    // addFurnitureItem: No 3D view calls needed here anymore
    public void addFurnitureItem(FurnitureItem item) {
        if (item != null) {
            this.furnitureItems.add(item);
            drawingCanvas.setFurnitureItems(this.furnitureItems);
            furnitureItemListPanel.updateList(this.furnitureItems); 
        }
    }

    // clearCanvas calls clearItems
    private void clearCanvas() {
        LOGGER.info("Clearing canvas...");
        clearItems(); 
    }

    // clearItems: Needs to tell 3D view to clear
    public void clearItems() {
        furnitureItems.clear();
        selectedItem = null;
        drawingCanvas.setFurnitureItems(furnitureItems);
        drawingCanvas.deselectItem(); 
        furnitureItemListPanel.updateList(this.furnitureItems);
        infoPanel.updateInfo(null); 
        // Tell 3D view to clear its items
        if (furniture3DView != null) {
            Platform.runLater(() -> furniture3DView.clearAllItemNodes());
        }
    }
    
    // itemUpdated: Needs to tell 3D view to update transforms
    @Override
    public void itemUpdated(FurnitureItem updatedItem) {
        if (this.selectedItem != null) {
            updateSelectedItem(this.selectedItem, updatedItem); 
        } else {
            if (!furnitureItems.isEmpty()) {
                FurnitureItem lastItem = furnitureItems.get(furnitureItems.size() -1);
                if (lastItem.type().equals(updatedItem.type())) { 
                     updateSelectedItem(lastItem, updatedItem);
                } else {
                     LOGGER.warning("itemUpdated called with no selectedItem, and updatedItem doesn't match last added. Update skipped for: " + updatedItem.type());
                }
            } else {
                 LOGGER.warning("itemUpdated called with no selectedItem and no items in list. Update skipped for: " + updatedItem.type());
            }
        }
    }
    
    // Called when scale spinner value changes
    private void handleScaleChange() {
        if (infoPanel.isInternalSpinnerUpdate() || selectedItem == null) {
            return; 
        }
        double newScale = infoPanel.getScaleValue();
        LOGGER.finer("Scale spinner changed by user to: " + newScale);
        FurnitureItem newItem = selectedItem.setScale(newScale, newScale); 
        updateSelectedItem(this.selectedItem, newItem); 
    }

    // Called when color button is clicked
    private void handleChangeItemColor() {
        if (selectedItem == null) {
            JOptionPane.showMessageDialog(this, "Please select an item first.", "No Item Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        java.awt.Color newColor = JColorChooser.showDialog(this, "Choose Color for " + selectedItem.type(), selectedItem.color());
        if (newColor != null) {
            FurnitureItem newItem = selectedItem.withColor(newColor);
            updateSelectedItem(this.selectedItem, newItem); 
        }
    }
    
    // Modified to take oldItem explicitly
    private void updateSelectedItem(FurnitureItem oldItem, FurnitureItem newItem) {
        if (oldItem == null || newItem == null) {
            LOGGER.warning("updateSelectedItem called with null oldItem or newItem.");
            return;
        }

        int index = furnitureItems.indexOf(oldItem);

        if (index != -1) {
            furnitureItems.set(index, newItem);
            this.selectedItem = newItem;

            // IMPORTANT: Give DrawingCanvas the updated list so it can update its internal copy
            // and also its own selectedItem reference. Pass a new copy.
            if (drawingCanvas != null) {
                drawingCanvas.setFurnitureItems(new ArrayList<>(this.furnitureItems));
            }

            infoPanel.updateInfo(newItem);
            updateSaveState(true);

            ModelLoadResult modelLoadResult = itemModelCache.get(newItem.getModelFilename());
            if (modelLoadResult == null && newItem.getModelFilename() != null && !newItem.getModelFilename().isEmpty()) {
                 LOGGER.info("Model data for " + newItem.getModelFilename() + " not in cache. Attempting to load...");
                 modelLoadResult = itemModelCache.computeIfAbsent(newItem.getModelFilename(), k ->
                    ModelHelper.loadModelDataFromObj(k, newItem.color())
                 );
            }

            if (modelLoadResult != null && modelLoadResult.geometry != null) {
                boolean colorChanged = !oldItem.color().equals(newItem.color());

                if (furniture3DView != null) { // Null check for 3D view
                    if (colorChanged) {
                        Platform.runLater(() -> furniture3DView.updateItemColor(oldItem, newItem));
                    }
                    Platform.runLater(() -> furniture3DView.updateItemTransforms(oldItem, newItem));
                } else {
                    LOGGER.warning("furniture3DView is null, skipping 3D updates for item: " + newItem.type());
                }

            } else {
                LOGGER.warning("Model data not found in cache for " + newItem.getModelFilename() +
                               " during update. 3D view may not reflect all changes or show the item.");
                if (furniture3DView != null) { // Null check for 3D view
                    Platform.runLater(() -> furniture3DView.removeItemNode(oldItem));
                } else {
                    LOGGER.warning("furniture3DView is null, skipping 3D removal for oldItem: " + oldItem.type());
                }
            }
            updateUndoRedoState();
            // drawingCanvas.repaint(); // setFurnitureItems in DrawingCanvas should handle repaint
        } else {
            LOGGER.warning("updateSelectedItem: oldItem not found in furnitureItems list. Old: " + oldItem.type() + " New: " + newItem.type());
        }
    }

    // Reads spinner values into member variables and updates 2D canvas
    private void updateRoomDimensionsState() {
        double newWidth = (Double) roomWidthSpinner.getValue();
        double newDepth = (Double) roomDepthSpinner.getValue(); // Correct spinner variable used
        double newHeight = (Double) wallHeightSpinner.getValue(); // Read height spinner

        boolean changed = false;
         if (Math.abs(newWidth - roomWidthMeters) > 1e-6) {
             roomWidthMeters = newWidth;
             changed = true;
         }
         if (Math.abs(newDepth - roomDepthMeters) > 1e-6) {
             roomDepthMeters = newDepth;
             changed = true;
         }
         if (Math.abs(newHeight - wallHeightMeters) > 1e-6) {
              wallHeightMeters = newHeight;
              changed = true;
         }

         if (changed) {
             drawingCanvas.setRoomDimensions(roomWidthMeters, roomDepthMeters); 
             LOGGER.info(String.format("Room dimensions updated: W=%.1f, D=%.1f, H=%.1f%n", 
                 roomWidthMeters, roomDepthMeters, wallHeightMeters));
             sendDimensionsAndColorsTo3DView(); // Send updates immediately
         }
    }

    // NEW: Method to send current dims/colors to 3D view
    private void sendDimensionsAndColorsTo3DView() {
         if (furniture3DView != null) {
            javafx.scene.paint.Color[] fxWallColors = new javafx.scene.paint.Color[4];
            for(int i=0; i<4; i++) {
                fxWallColors[i] = convertAwtToFxColor(wallColors[i]);
            }
            // Capture local vars for lambda
            final double width = roomWidthMeters;
            final double depth = roomDepthMeters;
            final double height = wallHeightMeters;
            Platform.runLater(() -> {
                furniture3DView.setRoomDimensions(width, depth, height);
                furniture3DView.setWallColors(fxWallColors);
            });
             LOGGER.info("Sent updated dimensions and colors to 3D view.");
        } else {
             LOGGER.warning("Attempted to send dimensions/colors but furniture3DView is null.");
        }
    }

    // Helper to convert AWT Color to JavaFX Color
    private javafx.scene.paint.Color convertAwtToFxColor(java.awt.Color awtColor) {
        if (awtColor == null) return javafx.scene.paint.Color.GRAY; // Default if null
        return javafx.scene.paint.Color.rgb(
            awtColor.getRed(), 
            awtColor.getGreen(), 
            awtColor.getBlue(), 
            (double) awtColor.getAlpha() / 255.0
        );
    }

    // Method to access the canvas if needed from outside
    public DrawingCanvas getDrawingCanvas() {
        return drawingCanvas;
    }

    // --- Item Listener Implementation --- 
    // (Methods itemSelected, itemDeselected, itemUpdated remain largely the same,
    // using LOGGER and removing extra repaint calls if setFurnitureItems handles it)
    @Override
    public void itemSelected(FurnitureItem item) {
        this.selectedItem = item; // Set the centrally tracked selected item
        LOGGER.fine("Item selected: " + item.type());
        infoPanel.updateInfo(this.selectedItem);
         // Let DrawingCanvas handle its own selection highlighting
    }

    @Override
    public void itemDeselected() {
        selectedItem = null;
        LOGGER.fine("Item deselected");
        infoPanel.updateInfo(null);
        // drawingCanvas.repaint(); // Likely not needed
    }

    /**
     * Sets up the action buttons (Save, Load, Clear, etc.)
     */
    private void setupActionButtons(JPanel buttonPanel) {
        JButton saveButton = new JButton("Save Layout");
        saveButton.addActionListener(e -> saveLayout()); // Calls placeholder
        buttonPanel.add(saveButton);

        JButton loadButton = new JButton("Load Layout");
        loadButton.addActionListener(e -> loadLayout()); // Calls placeholder
        buttonPanel.add(loadButton);

        JButton clearButton = new JButton("Clear Canvas");
        clearButton.addActionListener(e -> clearCanvas()); // Calls placeholder
        buttonPanel.add(clearButton);

        // REMOVED: Pan/Zoom buttons are replaced by direct canvas interaction
    }

    /** Sets up keyboard shortcuts for canvas interactions. */
    private void setupKeyboardActions() {
         // REMOVED: Keyboard pan/zoom actions are replaced by direct canvas interaction
    }

    // Helper to get contrasting text color (Now always returns BLACK)
    private java.awt.Color getContrastColor(java.awt.Color bg) {
        // if (bg == null) return java.awt.Color.BLACK; // No longer needed
        // // Simple brightness check
        // double brightness = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255.0;
        // // Lowered threshold: background needs to be darker to get white text
        // return (brightness > 0.45) ? java.awt.Color.BLACK : java.awt.Color.WHITE; 
        return java.awt.Color.BLACK; // Always return black
    }

    // Method to handle wall color selection (Uses and returns AWT Color)
    private void chooseWallColor(int wallIndex) {
        if (wallIndex < 0 || wallIndex >= wallColors.length) {
            LOGGER.warning("Invalid wall index for color chooser: " + wallIndex);
            return;
        }
        java.awt.Color currentColor = wallColors[wallIndex];
        java.awt.Color newColor = JColorChooser.showDialog(
            this, 
            "Choose Color for " + wallColorButtons[wallIndex].getText(), 
            currentColor
        );
        if (newColor != null && !newColor.equals(currentColor)) { // Check if color actually changed
            wallColors[wallIndex] = newColor; // Store AWT Color
            wallColorButtons[wallIndex].setBackground(newColor);
            wallColorButtons[wallIndex].setForeground(getContrastColor(newColor));
            // updateRoomDimensionsState(); // This might not call sendDimensions... if only color changed
            sendDimensionsAndColorsTo3DView(); // Directly send updates for color changes
            updateSaveState(true); // Mark unsaved changes for color change
            LOGGER.info("Wall " + wallIndex + " color changed. Sent update to 3D view.");
        }
    }

    // Called when the database is loaded or reloaded (Placeholder)
    private void onDatabaseLoad() {
        LOGGER.info("Database loaded, updating UI components."); // Use LOGGER
        updateAvailableItemsList(); // Call placeholder
    }

    // --- Placeholder Methods ---
    private void updateAvailableItemsList() {
        LOGGER.info("Placeholder: updateAvailableItemsList() called.");
        // TODO: Implement logic to refresh objListModel, perhaps from a DB or directory scan
        // For now, just ensure loadObjFiles() is called initially.
    }

    // --- Save/Load Implementation --- 
    private void saveLayout() {
        int result = layoutFileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = layoutFileChooser.getSelectedFile();
            // Ensure .furn extension
            if (!file.getName().toLowerCase().endsWith(".furn")) {
                file = new File(file.getParentFile(), file.getName() + ".furn");
            }

            try {
                LayoutData layoutData = new LayoutData();
                layoutData.roomWidthMeters = this.roomWidthMeters;
                layoutData.roomDepthMeters = this.roomDepthMeters;
                layoutData.wallHeightMeters = this.wallHeightMeters;
                
                // Convert wall colors
                layoutData.wallColors = new ArrayList<>();
                for (java.awt.Color awtColor : this.wallColors) {
                    layoutData.wallColors.add(new ColorData(awtColor));
                }

                // Convert furniture items
                layoutData.items = new ArrayList<>();
                for (FurnitureItem item : this.furnitureItems) {
                    ItemData itemData = new ItemData();
                    itemData.modelFilename = item.getModelFilename();
                    itemData.type = item.type();
                    itemData.color = new ColorData(item.color());
                    itemData.tx = item.getTx();
                    itemData.ty = item.getTy();
                    itemData.scaleX = item.getScaleX();
                    itemData.scaleY = item.getScaleY();
                    itemData.rotationRadians = item.getRotationRadians();
                    layoutData.items.add(itemData);
                }

                // Write JSON to file
                objectMapper.writeValue(file, layoutData);
                updateSaveState(false); // Mark as saved
                LOGGER.info("Layout saved successfully to: " + file.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Layout saved successfully!", "Save Successful", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error saving layout to file: " + file.getAbsolutePath(), e);
                JOptionPane.showMessageDialog(this, "Error saving layout: \n" + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadLayout() {
        int result = layoutFileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = layoutFileChooser.getSelectedFile();
            try {
                // Read LayoutData from JSON file
                LayoutData layoutData = objectMapper.readValue(file, LayoutData.class);

                // --- Apply Loaded Data --- 
                
                // 1. Clear current state
                clearItems(); // Clears list, selection, 2D canvas items, 3D view items

                // 2. Update Room Dimensions
                this.roomWidthMeters = layoutData.roomWidthMeters;
                this.roomDepthMeters = layoutData.roomDepthMeters;
                this.wallHeightMeters = layoutData.wallHeightMeters;
                roomWidthSpinner.setValue(this.roomWidthMeters);
                roomDepthSpinner.setValue(this.roomDepthMeters);
                wallHeightSpinner.setValue(this.wallHeightMeters);
                // Update 2D canvas dimensions (triggers repaint)
                drawingCanvas.setRoomDimensions(this.roomWidthMeters, this.roomDepthMeters); 

                // 3. Update Wall Colors
                if (layoutData.wallColors != null && layoutData.wallColors.size() == 4) {
                    IntStream.range(0, 4).forEach(i -> {
                        this.wallColors[i] = layoutData.wallColors.get(i).toAwtColor();
                        wallColorButtons[i].setBackground(this.wallColors[i]);
                        wallColorButtons[i].setForeground(getContrastColor(this.wallColors[i]));
                    });
                }
                // Send updated dimensions & colors to 3D view (handled below by refresh)

                // 4. Rebuild Furniture Items List
                List<FurnitureItem> loadedItems = new ArrayList<>();
                if (layoutData.items != null) {
                    for (ItemData itemData : layoutData.items) {
                        // Reload model template to get base footprint etc.
                        // Use a default color temporarily for loading, we'll apply the loaded color later
                        ModelHelper.ModelLoadResult templateResult = itemModelCache.computeIfAbsent(
                            itemData.modelFilename, 
                            k -> ModelHelper.loadModelDataFromObj(k, java.awt.Color.GRAY) // Default color for template
                        );

                        if (templateResult != null && templateResult.item != null) {
                            FurnitureItem template = templateResult.item;
                            // Start with the template (has base shape, type, base dimension, model filename)
                            FurnitureItem loadedItem = template 
                                .withColor(itemData.color.toAwtColor()) // Apply loaded color
                                .setScale(itemData.scaleX, itemData.scaleY) // Apply loaded scale
                                .setRotation(itemData.rotationRadians) // Apply loaded rotation
                                .translateTo(itemData.tx, itemData.ty); // Apply loaded translation

                            loadedItems.add(loadedItem);
                        } else {
                             LOGGER.warning("Could not reload model template for " + itemData.modelFilename + " during layout load. Item skipped.");
                        }
                    }
                }
                this.furnitureItems = loadedItems; // Replace the main list

                // 5. Update UI Components
                drawingCanvas.setFurnitureItems(this.furnitureItems); // Update canvas with new list
                furnitureItemListPanel.updateList(this.furnitureItems); // Update list panel
                infoPanel.updateInfo(null); // Clear info panel
                refresh3DViewCompletely(); // Reload all items in 3D view
                sendDimensionsAndColorsTo3DView(); // Ensure 3D room state is correct
                updateSaveState(false); // Mark as saved initially after load
                updateUndoRedoState(); // Reset undo/redo 

                LOGGER.info("Layout loaded successfully from: " + file.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Layout loaded successfully!", "Load Successful", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException | IllegalArgumentException e) { // Catch parsing errors too
                LOGGER.log(Level.SEVERE, "Error loading layout from file: " + file.getAbsolutePath(), e);
                JOptionPane.showMessageDialog(this, "Error loading layout: \n" + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // --- Added Placeholder Methods ---
    private void updateSaveState(boolean hasUnsavedChanges) {
        // TODO: Implement logic to indicate unsaved changes (e.g., enable save button, show asterisk)
        LOGGER.finer("updateSaveState called with: " + hasUnsavedChanges);
    }

    private void updateUndoRedoState() {
        // TODO: Implement logic to update undo/redo button states
        LOGGER.finer("updateUndoRedoState called.");
    }

    private void loadAndDisplayItem(FurnitureItem item, boolean isNewItem) {
        // This method was problematic and its functionality is largely covered by
        // how addSelectedItemToCanvas and updateSelectedItem handle model loading and caching.
        // If specific "load and display" logic is needed (e.g. for loading from a save file),
        // it should be carefully designed.
        LOGGER.info("loadAndDisplayItem called for " + item.type() + ". isNewItem: " + isNewItem + ". (Currently a simplified placeholder)");

        if (item == null || item.getModelFilename() == null || item.getModelFilename().isEmpty()) {
            LOGGER.warning("loadAndDisplayItem: Invalid item or model filename.");
            return;
        }

        ModelLoadResult result = itemModelCache.computeIfAbsent(item.getModelFilename(), k ->
            ModelHelper.loadModelDataFromObj(k, item.color())
        );

        if (result != null && result.geometry != null) {
            if (furniture3DView != null) {
                final FurnitureItem itemToDisplay = item;
                final GeometryData geomToDisplay = result.geometry;
                 Platform.runLater(() -> furniture3DView.addOrUpdateItemNode(itemToDisplay, geomToDisplay));
                 LOGGER.info("Loaded and displayed in 3D: " + item.type());
            }
            if (isNewItem && !furnitureItems.contains(item)) {
                 // This was part of the original addSelectedItemToCanvas logic which should handle adding to list
                 // addFurnitureItem(item); // Avoid duplicate add if called after addFurnitureItem
            }
        } else {
            LOGGER.warning("Failed to load model data in loadAndDisplayItem for: " + item.getModelFilename());
        }
    }

    // --- Added Handler Methods for 3D Zoom --- 
    private void handleZoomIn3D() {
        if (furniture3DView != null) {
            Platform.runLater(() -> furniture3DView.zoomIn());
            LOGGER.fine("Zoom In 3D requested.");
        } else {
            LOGGER.warning("Cannot Zoom In 3D: furniture3DView is null.");
        }
    }

    private void handleZoomOut3D() {
        if (furniture3DView != null) {
            Platform.runLater(() -> furniture3DView.zoomOut());
            LOGGER.fine("Zoom Out 3D requested.");
        } else {
            LOGGER.warning("Cannot Zoom Out 3D: furniture3DView is null.");
        }
    }

    // Method: Clears and reloads all items in the 3D view
    private void refresh3DViewCompletely() {
        LOGGER.info("Refreshing 3D view completely...");
        if (furniture3DView == null) {
            LOGGER.warning("Cannot refresh 3D view, furniture3DView is null.");
            return;
        }

        // Ensure 3D view has current room dimensions/colors *before* adding items
        sendDimensionsAndColorsTo3DView();

        final List<FurnitureItem> itemsCopy = new ArrayList<>(this.furnitureItems);

        Platform.runLater(() -> {
            furniture3DView.clearAllItemNodes();
            int loadedCount = 0;
            for (FurnitureItem item : itemsCopy) {
                // Use the cache to get geometry data, load if missing
                ModelLoadResult result = itemModelCache.computeIfAbsent(item.getModelFilename(), k ->
                    ModelHelper.loadModelDataFromObj(k, item.color()) // Use item's color for loading
                );

                if (result != null && result.geometry != null) {
                    // Add node using the item from our list (which has correct transform)
                    // and the geometry from the loaded result.
                    furniture3DView.addOrUpdateItemNode(item, result.geometry);
                    loadedCount++;
                } else {
                     LOGGER.warning("Failed to get/load model geometry during refresh for: " + item.getModelFilename());
                }
            }
            LOGGER.info("Refresh 3D complete. Added " + loadedCount + " item nodes.");
        });
    }
} 