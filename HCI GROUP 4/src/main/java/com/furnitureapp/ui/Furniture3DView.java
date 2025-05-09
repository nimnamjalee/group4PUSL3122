package com.furnitureapp.ui;

import com.furnitureapp.model.FurnitureItem;
import com.furnitureapp.model.GeometryData;
import com.furnitureapp.util.ModelHelper;

// --- Remove JOGL Imports ---
/*
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
*/

// --- Keep JavaFX Imports ---
import javafx.animation.AnimationTimer;
import javafx.scene.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.Cylinder;
import javafx.scene.control.Label;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.scene.transform.Transform;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.PointLight;
import javafx.geometry.Bounds;
import javafx.scene.transform.NonInvertibleTransformException;

// --- Keep jgltf Imports ---
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorDatas;
import de.javagl.jgltf.model.BufferViewModel;
import de.javagl.jgltf.model.BufferModel;
import de.javagl.jgltf.model.io.GltfModelReader;
// import de.javagl.jgltf.viewer.jfx.GltfModelViewerJfx; // Ensure this is commented if not used

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File; // Added missing import

/**
 * JavaFX component to display the 3D furniture arrangement with automatic rotation.
 */
public class Furniture3DView {

    private static final Logger LOGGER = Logger.getLogger(Furniture3DView.class.getName());
    private static final String MODELS_DIR = "assets/objects"; // Added constant
    private static final double DEFAULT_PIXELS_PER_METER = 50.0;
    private static final Color ROOM_BORDER_COLOR = Color.DARKGRAY;
    private static final Color ROOM_FILL_COLOR = new Color(0.95, 0.95, 0.95, 1.0); // Lighter gray fill
    private static final double WALL_THICKNESS = 5.0; // Thickness of walls in pixels
    private static final double INITIAL_3D_SCALE_FACTOR = 0.02; // NEW: Default scale for 3D items

    // --- Constants ---
    private static final double AXIS_LENGTH = 40.0; // For compass
    private static final double AXIS_RADIUS = 1.5;  // For compass
    private static final double ZOOM_STEP_FACTOR = 0.9; // Multiplier for zoom in (closer)
    private static final double MIN_CAMERA_DISTANCE = 50.0; // Min distance (prevent clipping)
    private static final double MAX_CAMERA_DISTANCE_FACTOR = 20.0; // Max distance relative to room size

    // --- JavaFX Scene Components ---
    private Group mainSceneRoot = new Group();       // Root for all 3D content
    private Group contentGroup = new Group();          // Group for floor + items + walls (this will be rotated)
    private PerspectiveCamera mainCamera;
    private Group roomGroup = new Group();              // For the floor and walls visualization
    private Group itemsGroup = new Group();             // Group to hold furniture item Nodes
    // --- Camera Transform Fields for easier control ---
    private Rotate cameraRotateX = new Rotate(-30, Rotate.X_AXIS); // Initial tilt
    private Rotate cameraRotateY = new Rotate(0, Rotate.Y_AXIS);
    private Translate cameraTranslate = new Translate(); // Handles zoom (Z) and height offset (Y)
    // --- Compass fields ---
    private Group compassRoot;         // Root for the compass visualization
    private Rotate compassRotateY = new Rotate(0, Rotate.Y_AXIS);
    private Rotate compassRotateX = new Rotate(0, Rotate.X_AXIS);

    // --- Wall Node References ---
    private Box northWallBox, eastWallBox, southWallBox, westWallBox;

    // --- State Variables ---
    private double pixelsPerMeter = DEFAULT_PIXELS_PER_METER;
    private double roomWidthMeters = 5.0;
    private double roomDepthMeters = 4.0;
    private double wallHeightMeters = 2.5; // Added wall height
    private Color[] wallColors = { Color.LIGHTGRAY, Color.LIGHTGRAY, Color.LIGHTGRAY, Color.LIGHTGRAY }; // Default FX Colors
    private List<FurnitureItem> currentItems = new ArrayList<>();
    private Map<FurnitureItem, Node> itemNodeMap = new HashMap<>(); // Map items to their JavaFX Nodes
    private boolean depthLightingEnabled = false; // Track current state

    // --- Animation ---
    private Rotate sceneRotationY = new Rotate(0, Rotate.Y_AXIS); // Rotation applied to contentGroup
    private AnimationTimer rotationTimer;
    private double rotationSpeed = 0.5; // Degrees per frame

    // --- REMOVED JOGL Fields ---
    // private GLCanvas glCanvas;
    // private FPSAnimator animator;
    // private GltfRenderer renderer;

    private AmbientLight ambientLight;
    private PointLight pointLight;

    public Furniture3DView() {
        initializeJavaFXComponents();
        setupAnimation();
    }

    private void initializeJavaFXComponents() {
        mainCamera = setupCamera();
        setupLighting();
        setupInitialScene();
        // No JOGL initialization needed
    }

    // Setup initial scene elements (lights, floor, content group)
    private void setupInitialScene() {
        drawRoom();      // Add floor
        contentGroup.getChildren().addAll(roomGroup, itemsGroup);
        
        // Set the pivot point for the rotation to the center of the floor
        double pivotX = roomWidthMeters * pixelsPerMeter / 2.0;
        double pivotZ = roomDepthMeters * pixelsPerMeter / 2.0;
        sceneRotationY.setPivotX(pivotX);
        sceneRotationY.setPivotZ(pivotZ);
        sceneRotationY.setPivotY(0); // Rotate around the floor plane Y=0

        // Apply the automatic rotation transform to the content group
        contentGroup.getTransforms().add(sceneRotationY);
        mainSceneRoot.getChildren().add(contentGroup); // Add content group to main root
    }

    // JavaFX Camera Setup
    private PerspectiveCamera setupCamera() {
        PerspectiveCamera camera = new PerspectiveCamera(true);
        
        double initialDistance = calculateInitialCameraDistance();
        double angle = -30; 

        // IMPORTANT: Keep reference to the position translate for zooming
        Translate position = new Translate(0, 0, -initialDistance); 
        Rotate lookAngle = new Rotate(angle, Rotate.X_AXIS); 
        
        double pivotX = roomWidthMeters * pixelsPerMeter / 2.0;
        double pivotZ = roomDepthMeters * pixelsPerMeter / 2.0;
        Translate pivot = new Translate(pivotX, 0, pivotZ);

        // Order: Pivot to center, Look down, Move back (position)
        camera.getTransforms().addAll(pivot, lookAngle, position);

        mainSceneRoot.getChildren().add(camera);

        camera.setNearClip(1.0); 
        camera.setFarClip(initialDistance * 5); // Adjust based on initial distance
        camera.setFieldOfView(60); 
        return camera;
    }

    // Helper to calculate initial/max camera distance
    private double calculateInitialCameraDistance() {
         double baseDim = Math.max(roomWidthMeters, roomDepthMeters) * pixelsPerMeter;
         return baseDim * 5.0; // Initial distance factor
    }
    
    private double getMaxCameraDistance() {
        double baseDim = Math.max(roomWidthMeters, roomDepthMeters) * pixelsPerMeter;
        return baseDim * MAX_CAMERA_DISTANCE_FACTOR;
    }

    // JavaFX Lighting (Ambient Only)
    private void setupLighting() {
        // Create both lights
        ambientLight = new AmbientLight(Color.rgb(200, 200, 200)); // Current bright ambient
        
        pointLight = new PointLight(Color.WHITE);
        // Position light near the ceiling, centered in the room
        updatePointLightPosition(); // Use helper to set initial position
        
        mainSceneRoot.getChildren().clear(); // Clear previous lights if any
        // Initially add only the ambient light
        mainSceneRoot.getChildren().add(ambientLight);
        depthLightingEnabled = false;
    }
    
    // Helper to update point light position based on room dimensions
    private void updatePointLightPosition() {
        if (pointLight != null) {
            double lightHeightOffset = -wallHeightMeters * pixelsPerMeter * 1.5; // Position above ceiling slightly
            pointLight.setTranslateX(roomWidthMeters * pixelsPerMeter / 2.0);   // Center X
            pointLight.setTranslateY(lightHeightOffset);                         // Above ceiling
            pointLight.setTranslateZ(roomDepthMeters * pixelsPerMeter / 2.0);    // Center Z
            LOGGER.finer("Updated point light position.");
        }
    }

    // Method to get the main camera for the SubScene
    public PerspectiveCamera getMainCamera() {
        return mainCamera;
    }

    // Method to get the root node for the main SubScene
    public Parent getMainSceneRoot() {
        return mainSceneRoot; 
    }

    /**
     * Creates the compass visualization Node.
     * @return A Group containing the compass elements.
     */
    public Parent createCompass() {
        // Check if compassRoot already exists
        if (compassRoot != null) {
            return compassRoot;
        }
        compassRoot = new Group(); // Initialize the compass root
        PhongMaterial redMaterial = new PhongMaterial(Color.RED);
        PhongMaterial greenMaterial = new PhongMaterial(Color.GREEN);
        PhongMaterial blueMaterial = new PhongMaterial(Color.BLUE);

        // --- X Axis (Red) ---
        Cylinder xAxis = new Cylinder(AXIS_RADIUS, AXIS_LENGTH);
        xAxis.setMaterial(redMaterial);
        xAxis.getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
        xAxis.setTranslateX(AXIS_LENGTH / 2.0);
        Label xLabel = new Label("X");
        xLabel.setTextFill(Color.RED);
        // Position label - requires scene graph context for proper positioning relative to viewport,
        // so this might need adjustment or a different approach (e.g., Billboard behavior).
        // For now, place relative to axis end.
        xLabel.getTransforms().add(new Translate(AXIS_LENGTH + 5, 0, 0));

        // --- Y Axis (Green) - UP in JavaFX ---
        Cylinder yAxis = new Cylinder(AXIS_RADIUS, AXIS_LENGTH);
        yAxis.setMaterial(greenMaterial);
        yAxis.setTranslateY(-(AXIS_LENGTH / 2.0)); // Y is up
        Label yLabel = new Label("Y");
        yLabel.setTextFill(Color.GREEN);
        yLabel.getTransforms().add(new Translate(0, -AXIS_LENGTH - 15, 0)); // Adjusted Y offset

        // --- Z Axis (Blue) ---
        Cylinder zAxis = new Cylinder(AXIS_RADIUS, AXIS_LENGTH);
        zAxis.setMaterial(blueMaterial);
        zAxis.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
        zAxis.setTranslateZ(AXIS_LENGTH / 2.0);
        Label zLabel = new Label("Z");
        zLabel.setTextFill(Color.BLUE);
        zLabel.getTransforms().add(new Translate(0, 0, AXIS_LENGTH + 5));

        compassRoot.getChildren().addAll(xAxis, yAxis, zAxis /*, xLabel, yLabel, zLabel */); // Labels might be tricky without scene graph context
        // Apply rotations for compass orientation
        compassRoot.getTransforms().addAll(compassRotateY, compassRotateX);
        return compassRoot;
    }

    // Draw JavaFX floor and walls
    private void drawRoom() {
        roomGroup.getChildren().clear(); // Clear previous floor and walls
        
        double roomWidthPixels = roomWidthMeters * pixelsPerMeter;
        double roomDepthPixels = roomDepthMeters * pixelsPerMeter;
        double wallHeightPixels = wallHeightMeters * pixelsPerMeter;

        // --- Floor --- 
        PhongMaterial floorMaterial = new PhongMaterial(ROOM_FILL_COLOR);
        Box floor = new Box(roomWidthPixels, WALL_THICKNESS, roomDepthPixels); // Use thickness
        floor.setMaterial(floorMaterial);
        // Position floor so its TOP surface is at Y=0 and its center is at the room's XZ center
        floor.setTranslateX(roomWidthPixels / 2.0);   // Added X translation
        floor.setTranslateY(-WALL_THICKNESS / 2.0); // Corrected: Move center DOWN by half thickness
        floor.setTranslateZ(roomDepthPixels / 2.0);    // Added Z translation
        roomGroup.getChildren().add(floor);

        // --- Walls --- 
        // Create materials for walls (index: 0=N, 1=E, 2=S, 3=W)
        PhongMaterial[] wallMaterials = new PhongMaterial[4];
        for (int i = 0; i < 4; i++) {
            wallMaterials[i] = new PhongMaterial(wallColors[i]);
        }
        
        // North Wall (Back, along X axis at Z=0)
        Box northWall = new Box(roomWidthPixels, wallHeightPixels, WALL_THICKNESS);
        northWall.setMaterial(wallMaterials[0]);
        northWall.setTranslateX(roomWidthPixels / 2.0); // Center X
        northWall.setTranslateY(-wallHeightPixels / 2.0); // Center Y (relative to floor at Y=0)
        northWall.setTranslateZ(WALL_THICKNESS / 2.0); // Position Z at the back edge
        this.northWallBox = northWall; // Store reference
        roomGroup.getChildren().add(this.northWallBox);

        // East Wall (Right, along Z axis at X=Width) - UNCOMMENTED
        Box eastWall = new Box(WALL_THICKNESS, wallHeightPixels, roomDepthPixels);
        eastWall.setMaterial(wallMaterials[1]);
        eastWall.setTranslateX(roomWidthPixels - (WALL_THICKNESS / 2.0)); // Position X at the right edge
        eastWall.setTranslateY(-wallHeightPixels / 2.0);
        eastWall.setTranslateZ(roomDepthPixels / 2.0); // Center Z
        this.eastWallBox = eastWall; // Store reference
        roomGroup.getChildren().add(this.eastWallBox);

        // South Wall (Front, along X axis at Z=Depth) - UNCOMMENTED
        Box southWall = new Box(roomWidthPixels, wallHeightPixels, WALL_THICKNESS);
        southWall.setMaterial(wallMaterials[2]);
        southWall.setTranslateX(roomWidthPixels / 2.0); 
        southWall.setTranslateY(-wallHeightPixels / 2.0);
        southWall.setTranslateZ(roomDepthPixels - (WALL_THICKNESS / 2.0)); // Position Z at the front edge
        this.southWallBox = southWall; // Store reference
        roomGroup.getChildren().add(this.southWallBox);

        // West Wall (Left, along Z axis at X=0)
        Box westWall = new Box(WALL_THICKNESS, wallHeightPixels, roomDepthPixels);
        westWall.setMaterial(wallMaterials[3]);
        westWall.setTranslateX(WALL_THICKNESS / 2.0); // Position X at the left edge
        westWall.setTranslateY(-wallHeightPixels / 2.0);
        westWall.setTranslateZ(roomDepthPixels / 2.0);
        this.westWallBox = westWall; // Store reference
        roomGroup.getChildren().add(this.westWallBox);

        LOGGER.finer("Drew room floor and walls centered at origin in roomGroup");
    }

    // Update room dimensions - Recalculates bounds and might adjust camera
    public void setRoomDimensions(double widthMeters, double depthMeters, double heightMeters) { // Added height parameter
        boolean changed = false;
        if (Math.abs(this.roomWidthMeters - widthMeters) > 1e-6) {
             this.roomWidthMeters = Math.max(0.1, widthMeters);
             changed = true;
        }
         if (Math.abs(this.roomDepthMeters - depthMeters) > 1e-6) {
             this.roomDepthMeters = Math.max(0.1, depthMeters);
             changed = true;
        }
        if (Math.abs(this.wallHeightMeters - heightMeters) > 1e-6) {
             this.wallHeightMeters = Math.max(0.1, heightMeters);
             changed = true;
        }

        if (changed) {
             drawRoom(); // Redraw floor and walls with new dimensions
             updateCameraPositioning(); // Adjust camera based on new room size
             updatePointLightPosition(); // Update light position when room changes
             LOGGER.info("3D View Room dimensions updated.");
        }
    }
    
     // Set wall colors
     public void setWallColors(Color[] colors) {
         if (colors != null && colors.length == 4) {
            boolean changed = false;
            for(int i=0; i<4; i++){
                if(!Objects.equals(this.wallColors[i], colors[i])){
                    this.wallColors[i] = colors[i] != null ? colors[i] : Color.LIGHTGRAY;
                    changed = true;
                }
            }
            if(changed){
                LOGGER.info("Updating wall colors.");
                drawRoom(); // Redraw room/walls with new colors
            }
         } else {
             LOGGER.warning("setWallColors called with invalid array.");
         }
     }

    // Update pixelsPerMeter - Requires rescaling everything
    public void setPixelsPerMeter(double ppm) {
        if (ppm <= 0 || Math.abs(this.pixelsPerMeter - ppm) < 1e-6) return; // No change or invalid
        
        LOGGER.info("Updating pixelsPerMeter to: " + ppm);
        this.pixelsPerMeter = ppm;
        
        // Redraw room with new scale
        drawRoom();
        
        // Adjust camera positioning based on new scale
        updateCameraPositioning();
        updatePointLightPosition(); // Update light position when scale changes

        // Update transforms for all existing items to reflect new scale
        // Need to iterate over a copy of keys to avoid ConcurrentModificationException if map changes
        List<FurnitureItem> itemsToUpdate = new ArrayList<>(itemNodeMap.keySet());
        for (FurnitureItem item : itemsToUpdate) {
            updateItemTransforms(item); // This will re-apply scale using the new pixelsPerMeter
        }
    }

    // Clears all items from the 3D scene
    public void clearAllItemNodes() {
        itemsGroup.getChildren().clear();          
        itemNodeMap.clear(); 
        LOGGER.info("Cleared all items from 3D view.");
    }

    // Adds a new item node or updates an existing one (geometry assumed loaded)
    public void addOrUpdateItemNode(FurnitureItem item, GeometryData geometry) {
        if (item == null || geometry == null) {
            LOGGER.warning("Attempted to add/update item node with null item or geometry.");
            return;
        }
        // Remove existing node for this item first, if any (safer than update)
        removeItemNode(item); 

        Node itemNode = null;
        try {
             itemNode = createNodeFromGeometry(geometry, item.color());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating node from geometry for: " + item.getModelFilename(), e);
            itemNode = createErrorPlaceholderNode(item); // Fallback
        }
        
        if (itemNode != null) {
             applyItemTransforms(item, itemNode); // Apply transforms
             itemsGroup.getChildren().add(itemNode);
             itemNodeMap.put(item, itemNode);
             LOGGER.finer("Added/Updated node for: " + item.type());
        }
    }

    // Updates the color of an existing item node
    public void updateItemColor(FurnitureItem oldItem, FurnitureItem newItem) {
         if (oldItem == null || newItem == null) {
             LOGGER.warning("updateItemColor called with null oldItem or newItem.");
             return;
         }
         Node itemNode = itemNodeMap.remove(oldItem); // Get node and remove old mapping

         if (itemNode != null) {
             if (itemNode instanceof Group itemGroup) { 
                 for (Node child : itemGroup.getChildren()) {
                     if (child instanceof MeshView meshView) {
                         if(meshView.getMaterial() instanceof PhongMaterial material) {
                             material.setDiffuseColor(convertAwtToFxColor(newItem.color())); // Use newItem's color
                             LOGGER.finer("Updated color for node originally for: " + oldItem.type() + " to new item color from: " + newItem.type());
                             // Consider if multiple meshviews in a group need updating, for now, assume one primary mesh
                         }
                     }
                 }
             } else {
                  LOGGER.warning("Could not update color: Node for item " + oldItem.type() + " is not the expected Group structure.");
             }
             itemNodeMap.put(newItem, itemNode); // Re-add mapping with the newItem as key
         } else {
             LOGGER.warning("Node not found in map for oldItem: " + oldItem.type() + " during color update. Cannot update 3D color.");
         }
    }

    // Updates the transforms of an existing item node (when item INSTANCE changes, e.g. scale from spinner)
    public void updateItemTransforms(FurnitureItem oldItem, FurnitureItem newItem) {
        if (oldItem == null || newItem == null) {
            LOGGER.warning("updateItemTransforms (old/new) called with null oldItem or newItem.");
            return;
        }
        Node itemNode = itemNodeMap.remove(oldItem); // Get node and remove old mapping

        if (itemNode != null) {
            applyItemTransforms(newItem, itemNode); // Apply transforms using newItem's properties
            itemNodeMap.put(newItem, itemNode);     // Re-add mapping with the newItem as key
            LOGGER.finer("Updated transforms for node originally for: " + oldItem.type() + " using new item data: " + newItem.type());
        } else {
            LOGGER.warning("Node not found in map for oldItem: " + oldItem.type() + " during transform update (old/new). Cannot update 3D transforms.");
        }
    }

    // NEW: Overloaded method for internal updates where the item instance itself hasn't changed
    // e.g., when pixelsPerMeter changes, the item's own scale property is the same,
    // but its visual representation in 3D needs re-calculation.
    public void updateItemTransforms(FurnitureItem item) {
        if (item == null) {
            LOGGER.warning("updateItemTransforms (single item) called with null item.");
            return;
        }
        Node itemNode = itemNodeMap.get(item); // Get the node associated with this item
        if (itemNode != null) {
            applyItemTransforms(item, itemNode); // Re-apply transforms using the item's current state
            // No need to re-put in map as the key (item) hasn't changed instance
            LOGGER.finer("Refreshed transforms for node: " + item.type() + " (e.g., due to pixelsPerMeter change)");
        } else {
            LOGGER.warning("Node not found in map for item: " + item.type() + " during single-item transform update.");
        }
    }

    // Removes an item node from the scene
    public void removeItemNode(FurnitureItem item) {
        if (item == null) return;
        Node removedNode = itemNodeMap.remove(item);
        if (removedNode != null) {
            itemsGroup.getChildren().remove(removedNode);
            LOGGER.finer("Removed node for: " + item.type());
        }
    }

    // --- Renamed createNodeFromGeometry (logic mostly unchanged) --- 
    private Node createNodeFromGeometry(GeometryData geometry, java.awt.Color awtColor) {
        // Geometry data is already processed and provided
        float[] points = geometry.points();
        float[] normals = geometry.normals();
        float[] texCoords = geometry.texCoords();
        int[] faces = geometry.faces(); // Expects (p1, n1, t1, p2, n2, t2, ...) format

        // Basic validation
        if (points == null || faces == null || points.length == 0 || faces.length == 0) {
             LOGGER.warning("Attempted to create node with invalid geometry data.");
             // Cannot create an error placeholder here easily without the original item, 
             // but this should ideally be caught during loading in ModelHelper.
             return new Group(); // Return empty group
        }
        if (normals == null || normals.length != points.length) {
            LOGGER.warning("Normals array is null or size mismatch, using placeholders.");
            normals = createPlaceholderNormals(points.length);
        }
        if (texCoords == null || texCoords.length != (points.length / 3 * 2)) {
             LOGGER.warning("TexCoords array is null or size mismatch, using placeholders.");
             texCoords = createPlaceholderTexCoords(points.length);
        }
        
        // --- Create JavaFX TriangleMesh --- 
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        mesh.getPoints().setAll(points);
        mesh.getNormals().setAll(normals);   
        mesh.getTexCoords().setAll(texCoords); 

        // The faces array from GeometryData should be in p1,n1,t1, p2,n2,t2 format after conversion
        // Log its size to check against the warning
        if (faces == null || faces.length == 0) {
             LOGGER.warning("GeometryData provided null or empty faces array.");
             return new Group();
        }
        LOGGER.finer("Creating TriangleMesh with faces array size: " + faces.length);
        // Sanity check: If size isn't multiple of 9, log error and return empty
        // getFaceElementSize for POINT_NORMAL_TEXCOORD is 3 indices * 3 vertices = 9?
        // Actually, getFaceElementSize() refers to the number of elements per *vertex* in the face definition (p, n, t) => 3
        // So the faces array length must be divisible by 3 * 3 = 9. Let's re-check that logic.
        // From JavaFX docs: faces is [p1, t1, p2, t2, p3, t3...] if POINT_TEXCOORD. 
        // So POINT_NORMAL_TEXCOORD is likely [p1, n1, t1, p2, n2, t2, p3, n3, t3, ...] - Yes, length must be multiple of 9.
        
        int elementsPerVertex = 3; // p, n, t
        int verticesPerFace = 3; // triangle
        int indicesPerFace = elementsPerVertex * verticesPerFace; // Should be 9
        if (faces.length % indicesPerFace != 0) {
            LOGGER.severe("FATAL: Faces array size (" + faces.length + ") is not divisible by indicesPerFace (" + indicesPerFace + "). Cannot create valid TriangleMesh.");
            // This indicates a problem in the OBJ parsing or conversion in ModelHelper.
            return new Group(); // Return empty group to prevent JavaFX error
        }

        mesh.getFaces().setAll(faces); // Use the processed faces directly

        // --- Create MeshView and Material --- 
        MeshView meshView = new MeshView(mesh);
        PhongMaterial material = new PhongMaterial();
        
        // Convert AWT color to JavaFX color
        material.setDiffuseColor(convertAwtToFxColor(awtColor));
        material.setSpecularColor(Color.rgb(50,50,50)); 
        meshView.setMaterial(material);
        
        // --- Center Base at Y=0 --- 
        Bounds bounds = meshView.getBoundsInLocal(); 
        double minY = bounds.getMinY();
        double centerX = bounds.getCenterX();
        double centerZ = bounds.getCenterZ();
        Translate baseTranslate = new Translate(-centerX, -minY, -centerZ);
        meshView.getTransforms().add(baseTranslate);

        LOGGER.finer("Created JavaFX Node from geometry data.");
        // Return a Group containing the single MeshView for now.
        // If OBJ loader returns multiple parts, this needs adjustment.
        return new Group(meshView); 
    }
    
    // Helper to convert AWT Color to JavaFX Color (add if not present)
    private Color convertAwtToFxColor(java.awt.Color awtColor) {
        if (awtColor == null) return Color.GRAY; 
        return Color.rgb(
            awtColor.getRed(), 
            awtColor.getGreen(), 
            awtColor.getBlue(), 
            (double) awtColor.getAlpha() / 255.0
        );
    }

    // --- Placeholder Generators (Keep or move to ModelHelper) --- 
    private float[] createPlaceholderNormals(int pointDataLength) {
         float[] normals = new float[pointDataLength];
         for(int i=0; i < pointDataLength / 3; i++){
             normals[i*3 + 0] = 0.0f;
             normals[i*3 + 1] = 1.0f; // Default Up (Y)
             normals[i*3 + 2] = 0.0f;
         }
         return normals;
    }
    private float[] createPlaceholderTexCoords(int pointDataLength) {
         // Need 2 tex coords (s, t) for every 3 point coords (x, y, z)
         int numVertices = pointDataLength / 3;
         return new float[numVertices * 2]; // Defaults to all 0.0f
    }

    // Placeholder for error indication (keep this)
    private Node createErrorPlaceholderNode(FurnitureItem item) {
        double size = pixelsPerMeter * 0.5; // Fixed size for error sphere
        Sphere sphere = new Sphere(size / 2.0);
        sphere.setMaterial(new PhongMaterial(Color.RED));
        // Position based on item's intended location - handled by applyItemTransforms
        // sphere.setTranslateY(-size / 2.0); // Y is up, move half height down - REMOVE this, base should be at 0
        return new Group(sphere);
    }

    // Apply position, rotation, scale from FurnitureItem to a JavaFX Node
    private void applyItemTransforms(FurnitureItem item, Node node) {
        Translate itemTranslate = new Translate(item.getTx(), 0, item.getTy());
        Rotate itemRotate = new Rotate(Math.toDegrees(item.getRotationRadians()), Rotate.Y_AXIS); 
        Rotate flipToYUp = new Rotate(180, Rotate.X_AXIS); 
        Rotate defaultYRotation = new Rotate(180, Rotate.Y_AXIS); // Changed: Default 180-degree Y rotation (was 90)

        // --- Scaling ---
        // Apply the item's scale factor * default 3D factor * world scale (pixelsPerMeter)
        double baseScaleX = item.getScaleX() * INITIAL_3D_SCALE_FACTOR;
        double baseScaleY = item.getScaleY() * INITIAL_3D_SCALE_FACTOR;
        double finalScaleX = baseScaleX * pixelsPerMeter;
        double finalScaleY = baseScaleY * pixelsPerMeter;
        // Use finalScaleX for Z scaling to maintain aspect ratio from 2D view
        Scale itemScale = new Scale(finalScaleX, finalScaleY, finalScaleX); 
        
        LOGGER.log(Level.FINER, "Calculating scale for {0}: itemScale={1}/{2}, initial3D={3}, ppm={4} -> finalScale=({5}, {6}, {5})", 
            new Object[]{item.type(), item.getScaleX(), item.getScaleY(), INITIAL_3D_SCALE_FACTOR, pixelsPerMeter, finalScaleX, finalScaleY});

        node.getTransforms().clear(); 
        node.getTransforms().addAll(
            itemTranslate,      // 5. Move to final position
            itemRotate,         // 4. Rotate around item's origin (Y-axis) based on item's property
            defaultYRotation,   // 3. Apply default 180-degree Y rotation (was 90)
            flipToYUp,          // 2. Flip model to Y-up 
            itemScale           // 1. Apply scale 
        );
        
        // Log message already includes defaultYRotation object, which will now reflect 180 degrees
        LOGGER.finer("Applied transforms to " + item.type() + ": T=" + itemTranslate + ", R_item=" + itemRotate + ", R_defaultY=" + defaultYRotation + ", Flip=" + flipToYUp + ", S(final)=" + itemScale);
    }

    // Adjust camera position based on room size changes (also updates distance)
    public void updateCameraPositioning() {
        if (mainCamera == null) return;
        double newDistance = calculateInitialCameraDistance(); 
        double maxDistance = getMaxCameraDistance();
        
        Translate position = null;
        Translate pivot = null;
        for(Transform t : mainCamera.getTransforms()){
            if(t instanceof Translate){ 
                 // Assume first Translate is pivot, second is position
                 if (pivot == null) pivot = (Translate)t;
                 else position = (Translate)t; 
            }
        }

        if(position != null) {
            // Update distance, ensuring it doesn't exceed max based on new room size
            double clampedDistance = Math.min(Math.abs(position.getZ()), maxDistance);
            // Ensure distance is not less than min
            clampedDistance = Math.max(clampedDistance, MIN_CAMERA_DISTANCE);
            position.setY(0); 
            position.setZ(-clampedDistance); // Update clamped distance
            LOGGER.finer("Updated camera position Z=" + position.getZ());
        }
        if(pivot != null){
            pivot.setX(roomWidthMeters * pixelsPerMeter / 2.0);
            pivot.setZ(roomDepthMeters * pixelsPerMeter / 2.0);
             LOGGER.finer("Updated camera pivot: X=" + pivot.getX() + " Z=" + pivot.getZ());
        }
         mainCamera.setFarClip(maxDistance * 1.2); // Update far clip based on max possible distance
    }

    // --- Animation Setup --- 
    private void setupAnimation() {
         rotationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Increment rotation angle
                double currentAngle = sceneRotationY.getAngle();
                // Ensure angle stays within 0-360 range
                double newAngle = (currentAngle + rotationSpeed) % 360;
                sceneRotationY.setAngle(newAngle);

                // --- Update Wall Visibility based on Angle ---
                // Normalize angle to ensure it's positive for easier quadrant checks
                double normalizedAngle = (newAngle % 360 + 360) % 360;

                // Default: all walls visible initially
                boolean showNorth = true;
                boolean showEast = true;
                boolean showSouth = true;
                boolean showWest = true;

                // Hide the two walls generally facing *AWAY* from the camera to create cutaway.
                if (normalizedAngle >= 5 && normalizedAngle < 95) { 
                    // Camera looking roughly from +X side (towards -X). Hide North and East walls.
                    showNorth = false;
                    showEast = false;
                } else if (normalizedAngle >= 95 && normalizedAngle < 185) { 
                    // Camera looking roughly from +Z side (towards -Z). Hide East and South walls.
                    showEast = false;
                    showSouth = false;
                } else if (normalizedAngle >= 185 && normalizedAngle < 275) { 
                    // Camera looking roughly from -X side (towards +X). Hide South and West walls.
                    showSouth = false;
                    showWest = false;
                } else { 
                    // Camera looking roughly from -Z side (towards +Z). Hide West and North walls (0-45 and 315-360).
                    showWest = false;
                    showNorth = false;
                }

                // Apply visibility (check if nodes exist, though they should after drawRoom)
                if (northWallBox != null) northWallBox.setVisible(showNorth);
                if (eastWallBox != null) eastWallBox.setVisible(showEast);
                if (southWallBox != null) southWallBox.setVisible(showSouth);
                if (westWallBox != null) westWallBox.setVisible(showWest);
            }
        };
    }

    public void startRotationAnimation() {
         if (rotationTimer != null) {
             LOGGER.info("Starting 3D rotation animation.");
             rotationTimer.start();
         }
    }

    public void stopRotationAnimation() {
         if (rotationTimer != null) {
             LOGGER.info("Stopping 3D rotation animation.");
             rotationTimer.stop();
         }
    }

    // Method to clean up resources (if any needed for JavaFX)
    public void cleanup() {
        stopRotationAnimation(); // Stop timer on cleanup
        clearAllItemNodes(); // Clear nodes on cleanup
        LOGGER.info("Cleaning up Furniture3DView (JavaFX)...");
        // No explicit cleanup needed for animator or glCanvas anymore
        // JavaFX nodes will be garbage collected when no longer referenced.
        // If specific listeners or bindings were added, they should be removed here.
        mainSceneRoot.getChildren().clear();
        itemNodeMap.clear();
        currentItems.clear();
    }

    // --- NEW Zoom Methods --- 
    public void zoomIn() {
        modifyZoom(ZOOM_STEP_FACTOR);
    }
    
    public void zoomOut() {
        modifyZoom(1.0 / ZOOM_STEP_FACTOR);
    }
    
    private void modifyZoom(double factor) {
         if (mainCamera == null) return;
         Translate position = null;
         // Find the position transform (assuming it's the last Translate added)
         for (int i = mainCamera.getTransforms().size() - 1; i >= 0; i--) {
             if (mainCamera.getTransforms().get(i) instanceof Translate) {
                 // Heuristic: Assume the last Translate is the position/distance one
                 // Need to be careful if more Translates are added later.
                 // A safer way is to store a direct reference to the position Translate node.
                 Transform t = mainCamera.getTransforms().get(i);
                 boolean isPivot = false;
                 // Check if it might be the pivot based on common values (less reliable)
                 // if (Math.abs(t.getTx() - roomWidthMeters * pixelsPerMeter / 2.0) < 1e-6 && 
                 //     Math.abs(t.getTy() - 0) < 1e-6 && 
                 //     Math.abs(t.getTz() - roomDepthMeters * pixelsPerMeter / 2.0) < 1e-6) {
                 //    isPivot = true;
                 // } 
                 // For now, assume the *last* Translate is the distance one.
                 position = (Translate) t;
                 break;
             }
         }
         
         if (position != null) {
            double currentZ = position.getZ();
            double newZ = currentZ * factor;
            double maxDistance = getMaxCameraDistance();

            // Clamp the new Z value (distance is negative Z)
            newZ = Math.max(newZ, -maxDistance); // Furthest away (most negative Z)
            newZ = Math.min(newZ, -MIN_CAMERA_DISTANCE); // Closest (least negative Z)

            if (Math.abs(newZ - currentZ) > 1e-3) { // Only update if changed significantly
                position.setZ(newZ);
                // Optional: Adjust far clip dynamically, though updateCameraPositioning handles the max case.
                // mainCamera.setFarClip(Math.abs(newZ) * 1.2); 
                LOGGER.fine("Camera Zoomed: Factor=" + factor + ", New Z=" + newZ);
            } else {
                LOGGER.fine("Camera Zoom clamped, no change applied.");
            }
         } else {
             LOGGER.warning("Could not find camera position Translate transform to apply zoom.");
         }
    }

    // --- NEW Method to Toggle Lighting --- 
    public void toggleDepthLighting(boolean enable) {
        if (enable == depthLightingEnabled) {
            LOGGER.fine("Depth lighting state already set to: " + enable);
            return; // No change needed
        }
        
        // Remove existing lights first (safer)
        // Use instanceof checks for safety, although we know what we added
        mainSceneRoot.getChildren().removeIf(node -> node instanceof AmbientLight || node instanceof PointLight);
        
        if (enable) {
            if (pointLight != null) { // Ensure it's instantiated
                 mainSceneRoot.getChildren().add(pointLight);
                 depthLightingEnabled = true;
                 LOGGER.info("Enabled Depth Lighting (PointLight).");
            } else { 
                 LOGGER.warning("PointLight was null, cannot enable depth lighting."); 
            }
        } else {
             if (ambientLight != null) { // Ensure it's instantiated
                 mainSceneRoot.getChildren().add(ambientLight);
                 depthLightingEnabled = false;
                 LOGGER.info("Disabled Depth Lighting (AmbientLight).");
            } else {
                 LOGGER.warning("AmbientLight was null, cannot disable depth lighting."); 
            }
        }
    }

} 