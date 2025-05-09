package com.furnitureapp.model;

/**
 * Simple record to hold processed 3D geometry data suitable for creating a JavaFX MeshView.
 */
public record GeometryData(
    float[] points,      // Vertex positions (x, y, z, x, y, z, ...)
    float[] normals,     // Vertex normals (nx, ny, nz, nx, ny, nz, ...)
    float[] texCoords,   // Texture coordinates (u, v, u, v, ...)
    int[] faces          // Face indices (p1, n1, t1, p2, n2, t2, ... for POINT_NORMAL_TEXCOORD)
) {} 