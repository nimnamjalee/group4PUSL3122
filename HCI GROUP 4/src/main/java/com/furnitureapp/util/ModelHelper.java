package com.furnitureapp.util;

import com.furnitureapp.model.FurnitureItem;
import com.furnitureapp.model.GeometryData; // Import the new record

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer; // Needed for Obj data
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder; // Import for LITTLE_ENDIAN
import java.util.List;
import java.util.ArrayList; // Added for collectAllNodes
import java.nio.IntBuffer; // Needed for face indices

// jgltf imports - these might need adjustment based on specific jgltf versions/modules
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.io.GltfModelReader;
import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.GltfConstants;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.ElementType;
import de.javagl.jgltf.model.GltfModels; // Utility class
import de.javagl.jgltf.model.MathUtils; // For matrix multiplication

// import de.javagl.jgltf.model.impl.DefaultGltfModel; // Might not be needed directly
// import de.javagl.jgltf.model.gl.TechniquesModel; // Likely not needed for bounds
// import de.javagl.jgltf.model.v2.MaterialModelV2; // For materials, not bounds
// import de.javagl.jgltf.model.v2.MeshPrimitiveModelV2; // Might be needed for iteration
// import de.javagl.jgltf.model.v2.PbrMetallicRoughness; // Material details

// ---> ADD OBJ IMPORTS <---
import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

// Bounding box calculation (assuming jgltf-model includes utilities)
// If not available directly, might need manual iteration over vertices.
// This part is speculative and might need correction based on actual library features.
// Let's assume a utility or manual calculation is feasible.

/**
 * Helper class for loading 3D model information (e.g., from GLB or OBJ files)
 * and creating FurnitureItem instances and extracting geometry data.
 */
public class ModelHelper {

    private static final Logger LOGGER = Logger.getLogger(ModelHelper.class.getName());

    // Simple wrapper class to return both Item and Geometry
    public static class ModelLoadResult {
        public final FurnitureItem item;
        public final GeometryData geometry;

        ModelLoadResult(FurnitureItem item, GeometryData geometry) {
            this.item = item;
            this.geometry = geometry;
        }
    }

    /**
     * Loads information and geometry from an OBJ file.
     * Calculates the base footprint and largest dimension for normalization.
     * Extracts geometry data suitable for JavaFX rendering.
     *
     * @param objFilePath Path to the .obj file.
     * @param defaultColor The color to assign to the FurnitureItem.
     * @return A ModelLoadResult containing the FurnitureItem and GeometryData, or null if loading fails.
     */
    public static ModelLoadResult loadModelDataFromObj(String objFilePath, Color defaultColor) {
        Path path = Paths.get(objFilePath);
        String filename = path.getFileName().toString();
        String type = filename.replaceFirst("[.][^.]+$", ""); 

        Obj obj = null;
        try (InputStream inputStream = new FileInputStream(objFilePath)) {
            obj = ObjReader.read(inputStream);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read OBJ file: " + objFilePath, e);
            return null;
        }

        Obj renderableObj = ObjUtils.convertToRenderable(obj);

        // --- Extract Geometry Data --- 
        FloatBuffer verticesBuffer = ObjData.getVertices(renderableObj);
        float[] points = bufferToArray(verticesBuffer);
        if (points == null) {
            LOGGER.log(Level.SEVERE, "OBJ file has no vertex data: " + objFilePath);
            return null;
        }

        FloatBuffer normalsBuffer = ObjData.getNormals(renderableObj);
        float[] normals = bufferToArray(normalsBuffer);
        if (normals == null) {
            LOGGER.log(Level.WARNING, "OBJ file has no normal data, creating placeholders: " + objFilePath);
            normals = createPlaceholderNormals(points.length); 
        }
        
        FloatBuffer texCoordsBuffer = ObjData.getTexCoords(renderableObj, 2); 
        float[] texCoords = bufferToArray(texCoordsBuffer);
        if (texCoords == null) {
            LOGGER.log(Level.WARNING, "OBJ file has no texture coordinate data, creating placeholders: " + objFilePath);
            texCoords = createPlaceholderTexCoords(points.length); 
        }

        // --- Extract Indices Separately and Interleave --- 
        IntBuffer vIndicesBuffer = ObjData.getFaceVertexIndices(renderableObj); // Point indices
        IntBuffer nIndicesBuffer = ObjData.getFaceNormalIndices(renderableObj); // Normal indices
        IntBuffer tIndicesBuffer = ObjData.getFaceTexCoordIndices(renderableObj); // TexCoord indices

        int[] vIndices = bufferToArray(vIndicesBuffer);
        int[] nIndices = bufferToArray(nIndicesBuffer);
        int[] tIndices = bufferToArray(tIndicesBuffer);

        // Validation: After convertToRenderable, these should exist and match lengths
        if (vIndices == null || nIndices == null || tIndices == null) {
             LOGGER.log(Level.SEVERE, "OBJ file missing vertex, normal, or texCoord indices after conversion: " + objFilePath);
             return null;
        }
        if (vIndices.length != nIndices.length || vIndices.length != tIndices.length) {
             LOGGER.log(Level.SEVERE, 
                 "Index array length mismatch after conversion! V:" + vIndices.length + 
                 " N:" + nIndices.length + " T:" + tIndices.length + " for file: " + objFilePath);
             return null;
        }
        if (vIndices.length % 3 != 0) {
             LOGGER.log(Level.SEVERE, "Number of vertex indices (" + vIndices.length + ") is not divisible by 3 (not triangles?): " + objFilePath);
             return null;
        }

        // Manually create the interleaved faces array [p1, n1, t1, p2, n2, t2, ...]
        int numFaceVertices = vIndices.length;
        int[] faces = new int[numFaceVertices * 3]; 
        for (int i = 0; i < numFaceVertices; i++) {
            faces[i * 3 + 0] = vIndices[i]; // Point index
            faces[i * 3 + 1] = nIndices[i]; // Normal index
            faces[i * 3 + 2] = tIndices[i]; // TexCoord index
        }
        
        // Package geometry data
        GeometryData geometry = new GeometryData(points, normals, texCoords, faces);

        // --- Calculate Bounding Box and Dimensions from Vertices --- 
        float[] min = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY};
        float[] max = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        for (int i = 0; i < points.length; i += 3) {
             min[0] = Math.min(min[0], points[i]);
             min[1] = Math.min(min[1], points[i+1]);
             min[2] = Math.min(min[2], points[i+2]);
             max[0] = Math.max(max[0], points[i]);
             max[1] = Math.max(max[1], points[i+1]);
             max[2] = Math.max(max[2], points[i+2]);
        }
        double width = max[0] - min[0];
        double height = max[1] - min[1]; 
        double depth = max[2] - min[2];
        width = Math.max(1e-6, width);
        height = Math.max(1e-6, height);
        depth = Math.max(1e-6, depth);
        double baseLargestDimension = Math.max(width, Math.max(height, depth)); 

        // --- Create 2D Base Footprint --- 
        Shape baseFootprint = new Rectangle2D.Double(-(width / 2.0), -(depth / 2.0), width, depth );

        LOGGER.log(Level.INFO, "Loaded OBJ model: {0}, Type: {1}, Verts: {2}",
                   new Object[]{filename, type, points.length / 3});

        FurnitureItem item = new FurnitureItem(baseFootprint, defaultColor, objFilePath, type, baseLargestDimension);
        
        return new ModelLoadResult(item, geometry);
    }

    // Helper function to convert Buffer to array or return null
    private static float[] bufferToArray(FloatBuffer buffer) {
        if (buffer == null || buffer.remaining() == 0) return null;
        float[] arr = new float[buffer.remaining()];
        buffer.get(arr);
        return arr;
    }
    private static int[] bufferToArray(IntBuffer buffer) {
        if (buffer == null || buffer.remaining() == 0) return null;
        int[] arr = new int[buffer.remaining()];
        buffer.get(arr);
        return arr;
    }

    // Placeholder generators (can be copied from Furniture3DView or kept here)
    private static float[] createPlaceholderNormals(int pointDataLength) {
        float[] normals = new float[pointDataLength]; // Same size as points
        for(int i=0; i < pointDataLength / 3; i++){
            normals[i*3 + 0] = 0.0f;
            normals[i*3 + 1] = 1.0f; // Default Up (Y)
            normals[i*3 + 2] = 0.0f;
        }
        return normals;
    }

    private static float[] createPlaceholderTexCoords(int pointDataLength) {
        int numVertices = pointDataLength / 3;
        return new float[numVertices * 2]; // Defaults to all 0.0f
    }

    /**
     * Loads basic information from a GLB file to create a FurnitureItem.
     * Calculates the base footprint and largest dimension for normalization.
     *
     * @param glbPath Path to the .glb file.
     * @param color   Default color if needed (GLB materials should ideally be used).
     * @return A new FurnitureItem, or null if loading fails.
     */
    public static FurnitureItem loadFurnitureItemFromGlb(String glbPath, Color color) {
        Path path = Paths.get(glbPath);
        String filename = path.getFileName().toString();
        String type = filename.replaceFirst("[.][^.]+$", ""); // Basic type extraction

        try (InputStream inputStream = new FileInputStream(glbPath)) {
            GltfModelReader reader = new GltfModelReader();
            GltfModel gltfModel = reader.read(path.toUri());

            // --- Calculate Bounding Box and Dimensions ---
            float[] min = new float[3]; 
            float[] max = new float[3]; 
            calculateBoundingBox(gltfModel, min, max);

            double width = max[0] - min[0];
            double height = max[1] - min[1];
            double depth = max[2] - min[2];

            // Handle cases where bounds might be zero or invalid
            width = Math.max(1e-6, width);
            height = Math.max(1e-6, height);
            depth = Math.max(1e-6, depth);

            double baseLargestDimension = Math.max(width, Math.max(height, depth));

            // --- Create 2D Base Footprint (Centered at Origin) ---
            Shape baseFootprint = new Rectangle2D.Double(
                    -(width / 2.0), 
                    -(depth / 2.0), 
                    width,          
                    depth           
            );

            LOGGER.log(Level.INFO, "Loaded model info: {0}, Type: {1}, MaxDim: {2}, Footprint: {3}",
                       new Object[]{filename, type, baseLargestDimension, baseFootprint.getBounds2D()});

            return new FurnitureItem(baseFootprint, color, glbPath, type, baseLargestDimension);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read GLB file: " + glbPath, e);
            return null;
        } catch (Exception e) { 
            LOGGER.log(Level.SEVERE, "Failed to process GLB model data: " + glbPath, e);
            return null;
        }
    }

    // Adjust the main calculateBoundingBox method signature to pass the model down
    private static void calculateBoundingBox(GltfModel model, float[] min, float[] max) {
        // Remove placeholder logic and re-enable actual calculation
        min[0] = Float.POSITIVE_INFINITY;
        min[1] = Float.POSITIVE_INFINITY;
        min[2] = Float.POSITIVE_INFINITY;
        max[0] = Float.NEGATIVE_INFINITY;
        max[1] = Float.NEGATIVE_INFINITY;
        max[2] = Float.NEGATIVE_INFINITY;

        // Iterate through all scenes and their nodes to process all meshes
        for (SceneModel sceneModel : model.getSceneModels()) {
            for (NodeModel nodeModel : sceneModel.getNodeModels()) {
                // Process this node and its children recursively, passing the main model
                processNodeForBounds(nodeModel, model, min, max); // Pass model here
            }
        }

        // Handle cases where no geometry was found
        if (min[0] == Float.POSITIVE_INFINITY) {
             LOGGER.log(Level.WARNING, "No geometry found in model to calculate bounds, using default 1x1x1 cube.");
             min[0] = -0.5f; min[1] = -0.5f; min[2] = -0.5f;
             max[0] = 0.5f;  max[1] = 0.5f;  max[2] = 0.5f;
        }

        LOGGER.log(Level.FINE, "Calculated Bounds: Min({0}, {1}, {2}), Max({3}, {4}, {5})",
                   new Object[]{min[0], min[1], min[2], max[0], max[1], max[2]});
    }

    // Recursive helper to process nodes and their meshes
    private static void processNodeForBounds(NodeModel nodeModel, GltfModel gltfModel, float[] globalMin, float[] globalMax) {
        // Try getting the mesh models associated with this node
        List<MeshModel> meshModels = nodeModel.getMeshModels(); // Try getMeshModels()
        MeshModel meshModel = null;

        if (meshModels != null && !meshModels.isEmpty()) {
            if (meshModels.size() > 1) {
                 LOGGER.log(Level.WARNING, "Node " + nodeModel + " has multiple MeshModels, using the first one.");
            }
            meshModel = meshModels.get(0); // Get the first (and likely only) mesh model
        } 
        // else: Node doesn't have a mesh, which is valid.
        
        // --- Original logic using meshModel if found ---
        if (meshModel != null) {
            float[] nodeTransform = nodeModel.getMatrix(); // Get node transform

            for (MeshPrimitiveModel primitive : meshModel.getMeshPrimitiveModels()) {
                AccessorModel positionAccessor = primitive.getAttributes().get("POSITION");
                // Ensure accessor exists, is VEC3, and components are FLOATs
                if (positionAccessor != null &&
                    positionAccessor.getElementType() == ElementType.VEC3 &&
                    positionAccessor.getComponentType() == GltfConstants.GL_FLOAT) {

                    AccessorData positionData = positionAccessor.getAccessorData();
                    int numVertices = positionAccessor.getCount();
                    // Use createByteBuffer() to get the ByteBuffer
                    ByteBuffer byteBuffer = positionData.createByteBuffer();
                    // Ensure LITTLE_ENDIAN byte order is used for reading floats
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                    int byteStride = positionAccessor.getByteStride();
                    // If byteStride is 0 or invalid, assume tightly packed VEC3 floats
                    if (byteStride <= 0) {
                        byteStride = 3 * Float.BYTES;
                    }
                    int byteOffset = positionAccessor.getByteOffset();
                    
                    // Use getData() to get the ByteBuffer - Incorrect
                    // ByteBuffer byteBuffer = positionData.getData(); 

                    for (int i = 0; i < numVertices; i++) {
                        int currentOffset = byteOffset + i * byteStride;

                        // Check buffer limit before reading
                        if (currentOffset + 3 * Float.BYTES > byteBuffer.limit()) {
                             LOGGER.log(Level.SEVERE, "Buffer underflow trying to read vertex " + i + " at offset " + currentOffset);
                             break; // Stop processing this accessor
                        }

                        // Use ByteBuffer.getFloat(index)
                        float x = byteBuffer.getFloat(currentOffset + 0 * Float.BYTES);
                        float y = byteBuffer.getFloat(currentOffset + 1 * Float.BYTES);
                        float z = byteBuffer.getFloat(currentOffset + 2 * Float.BYTES);

                        // Use local coordinates directly
                        globalMin[0] = Math.min(globalMin[0], x);
                        globalMin[1] = Math.min(globalMin[1], y);
                        globalMin[2] = Math.min(globalMin[2], z);
                        globalMax[0] = Math.max(globalMax[0], x);
                        globalMax[1] = Math.max(globalMax[1], y);
                        globalMax[2] = Math.max(globalMax[2], z);
                    }
                } else if (positionAccessor != null) {
                     LOGGER.log(Level.WARNING, "Skipping POSITION accessor due to incompatible element/component type: " +
                                                positionAccessor.getElementType() + "/" + positionAccessor.getComponentType());
                }
            }
        }

        // Recursively process children nodes
        for (NodeModel childNode : nodeModel.getChildren()) {
            processNodeForBounds(childNode, gltfModel, globalMin, globalMax); // Pass gltfModel
        }
    }

    // Helper to recursively collect all nodes from a glTF scene graph structure (Might be needed by GLB loader)
    private static void collectAllNodes(List<NodeModel> nodes, List<NodeModel> allNodes) {
        if (nodes == null) return;
        for (NodeModel node : nodes) {
            allNodes.add(node);
            collectAllNodes(node.getChildren(), allNodes);
        }
    }
} 