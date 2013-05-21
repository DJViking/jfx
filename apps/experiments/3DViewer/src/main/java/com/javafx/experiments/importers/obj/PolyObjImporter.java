/*
 * Copyright (c) 2008, 2013 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.importers.obj;


import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import com.javafx.experiments.shape3d.PolygonMesh;
import com.javafx.experiments.shape3d.PolygonMeshView;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * OBJ object loader
 */
public class PolyObjImporter {

    private int vertexIndex(int vertexIndex) {
        if (vertexIndex < 0) {
            return vertexIndex + vertexes.size() / 3;
        } else {
            return vertexIndex - 1;
        }
    }

    private int uvIndex(int uvIndex) {
        if (uvIndex < 0) {
            return uvIndex + uvs.size() / 2;
        } else {
            return uvIndex - 1;
        }
    }

    private static boolean debug = true;
    private static float scale = 1;
    private static boolean flatXZ = false;

    static void log(String string) {
        if (debug) {
            System.out.println(string);
        }
    }

    public Set<String> getMeshes() {
        return meshes.keySet();
    }

    private Map<String, PolygonMesh> meshes = new HashMap<>();
    private Map<String, Material> materials = new HashMap<>();
    private List<Map<String, Material>> materialLibrary = new ArrayList<>();
    private String objFilename;

    public PolyObjImporter(String filename) throws FileNotFoundException, IOException {
        this.objFilename = filename;
        log("Reading filename = " + filename);
        read(new FileInputStream(filename));
    }

    public PolyObjImporter(InputStream inputStream) throws IOException {
        read(inputStream);
    }
    
    public PolygonMesh getMesh() {
        return meshes.values().iterator().next();
    }
    
    public Material getMaterial() {
        return materials.values().iterator().next();
    }
    
    public PolygonMesh getMesh(String key) {
        return meshes.get(key);
    }

    public Material getMaterial(String key) {
        return materials.get(key);
    }
    
    public PolygonMeshView buildPolygonMeshView(String key) {
        PolygonMeshView polygonMeshView = new PolygonMeshView();
        polygonMeshView.setId(key);
        polygonMeshView.setMaterial(materials.get(key));
        polygonMeshView.setMesh(meshes.get(key));
//        polygonMeshView.setCullFace(CullFace.NONE); TODO
        return polygonMeshView;
    }
    
    public static void setDebug(boolean debug) {
        PolyObjImporter.debug = debug;
    }

    public static void setScale(float scale) {
        PolyObjImporter.scale = scale;
    }

    private FloatArrayList vertexes = new FloatArrayList();
    private FloatArrayList uvs = new FloatArrayList();
    private List<int[]> faces = new ArrayList<>();
    private IntegerArrayList smoothingGroups = new IntegerArrayList();
    private Material material = new PhongMaterial(Color.WHITE);
    private int facesStart = 0;
    private int smoothingGroupsStart = 0;
    
    private void read(InputStream inputStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        int currentSmoothGroup = 0;
        String key = "default";
        while ((line = br.readLine()) != null) {
            try {
                if (line.startsWith("g ") || line.equals("g")) {
                    addMesh(key);
                    key = line.length() > 2 ? line.substring(2) : "default";
                    log("key = " + key);
                } else if (line.startsWith("v ")) {
                    String[] split = line.substring(2).trim().split(" +");
                    float x = Float.parseFloat(split[0]) * scale;
                    float y = Float.parseFloat(split[1]) * scale;
                    float z = Float.parseFloat(split[2]) * scale;

    //                log("x = " + x + ", y = " + y + ", z = " + z);

                    vertexes.add(x);
                    vertexes.add(y);
                    vertexes.add(z);

                    if (flatXZ) {
                        uvs.add(x);
                        uvs.add(z);
                    }
                } else if (line.startsWith("vt ")) {
                    String[] split = line.substring(3).trim().split(" +");
                    float u = split[0].trim().equalsIgnoreCase("nan") ? Float.NaN : Float.parseFloat(split[0]);
                    float v = split[1].trim().equalsIgnoreCase("nan") ? Float.NaN : Float.parseFloat(split[1]);

    //                log("u = " + u + ", v = " + v);

                    uvs.add(u);
                    uvs.add(1 - v);
                } else if (line.startsWith("f ")) {
                    String[] split = line.substring(2).trim().split(" +");
                    int[] faceIndexes = new int[split.length*2];
                    for (int i = 0; i < split.length; i++) {
                        String[] split2 = split[i].split("/");
                        faceIndexes[i*2] = vertexIndex(Integer.parseInt(split2[0]));
                        faceIndexes[(i*2)+1] = (split2.length > 1) ? uvIndex(Integer.parseInt(split2[1])) : 0;
                    }
                    faces.add(faceIndexes);
//                        smoothingGroups.add(currentSmoothGroup); TODO
                } else if (line.startsWith("s ")) {
                    if (line.substring(2).equals("off")) {
                        currentSmoothGroup = 0;
                    } else {
                        currentSmoothGroup = Integer.parseInt(line.substring(2));
                    }
                } else if (line.startsWith("mtllib ")) {
                    // setting materials lib
                    String[] split = line.substring("mtllib ".length()).trim().split(" +");
                    for (String filename : split) {
                        MtlReader mtlReader = new MtlReader(filename, objFilename);
                        materialLibrary.add(mtlReader.getMaterials());
                    }
                } else if (line.startsWith("usemtl ")) {
                    addMesh(key);
                    // setting new material for next mesh
                    String materialName = line.substring("usemtl ".length());
                    for (Map<String, Material> mm : materialLibrary) {
                        Material m = mm.get(materialName);
                        if (m != null) {
                            material = m;
                            break;
                        }
                    }
                } else if (line.isEmpty() || line.startsWith("#")) {
                    // comments and empty lines are ignored
                } else if (line.startsWith("vn ")) {
                    // normals are ignored
                } else {
                    log("line skipped: " + line);
                }
            } catch (Exception ex) {
                Logger.getLogger(MtlReader.class.getName()).log(Level.SEVERE, "Failed to parse line:" + line, ex);
            }
        }
        addMesh(key);
        
        log("Totally loaded " + (vertexes.size() / 3.) + " vertexes, " 
                + (uvs.size() / 2.) + " uvs, " 
                + (faces.size() / 6.) + " faces, " 
                + smoothingGroups.size() + " smoothing groups.");
    }
    
    private void addMesh(String key) {
        if (facesStart >= faces.size()) {
            // we're only interested in faces
            smoothingGroupsStart = smoothingGroups.size();
            return;
        }
        Map<Integer, Integer> vertexMap = new HashMap<>(vertexes.size() / 2);
        Map<Integer, Integer> uvMap = new HashMap<>(uvs.size() / 2);
        FloatArrayList newVertexes = new FloatArrayList(vertexes.size() / 2);
        FloatArrayList newUVs = new FloatArrayList(uvs.size() / 2);

        int[][] faceArrays = new int[faces.size()-facesStart][];
        
        for (int i = facesStart; i < faces.size();i++) {
            int[] faceIndexes = faces.get(i);
            for (int j=0;j<faceIndexes.length;j+=2){
                int vi = faceIndexes[j];
                Integer nvi = vertexMap.get(vi);
                if (nvi == null) {
                    nvi = newVertexes.size() / 3;
                    vertexMap.put(vi, nvi);
                    newVertexes.add(vertexes.get(vi * 3));
                    newVertexes.add(vertexes.get(vi * 3 + 1));
                    newVertexes.add(vertexes.get(vi * 3 + 2));
                }
                faceIndexes[j] = nvi;
//                faces.set(i, nvi);
                int uvi = faceIndexes[j+1];
                Integer nuvi = uvMap.get(uvi);
                if (nuvi == null) {
                    nuvi = newUVs.size() / 2;
                    uvMap.put(uvi, nuvi);
                    if (uvi >= 0) {
                        newUVs.add(uvs.get(uvi * 2));
                        newUVs.add(uvs.get(uvi * 2 + 1));
                    } else {
                        newUVs.add(0f);
                        newUVs.add(0f);
                    }
                }
                faceIndexes[j+1] = nuvi;
//                faces.set(i + 1, nuvi);
            }
            faceArrays[i-facesStart] = faceIndexes;
        }

        // TODO
//        mesh.setPoints(newVertexes.toFloatArray());
//        mesh.setTexCoords(newUVs.toFloatArray());
//        mesh.setFaces(((IntegerArrayList) faces.subList(facesStart, faces.size())).toIntArray());
//        mesh.setFaceSmoothingGroups(((IntegerArrayList) smoothingGroups.subList(smoothingGroupsStart, smoothingGroups.size())).toIntArray());


        PolygonMesh mesh = new PolygonMesh(
                newVertexes.toFloatArray(),
                newUVs.toFloatArray(),
                faceArrays
        );
        System.out.println("mesh.points = " + Arrays.toString(mesh.points));
        System.out.println("mesh.texCoords = " + Arrays.toString(mesh.texCoords));
        System.out.println("mesh.faces: ");
        for (int[] face: mesh.faces) {
            System.out.println("    face:: "+Arrays.toString(face));
        }

        int keyIndex = 2;
        String keyBase = key;
        while (meshes.get(key) != null) {
            key = keyBase + " (" + keyIndex++ + ")";
        }
        meshes.put(key, mesh);
        materials.put(key, material);
        
        log("Added mesh '" + key + "' of " + (mesh.points.length/3) + " vertexes, "
                + (mesh.texCoords.length/2) + " uvs, "
                + mesh.faces.length + " faces, "
                + 0 + " smoothing groups.");
        log("material diffuse color = " + ((PhongMaterial) material).getDiffuseColor());
        log("material diffuse map = " + ((PhongMaterial) material).getDiffuseMap());
        
        facesStart = faces.size();
        smoothingGroupsStart = smoothingGroups.size();
    }

    public static void setFlatXZ(boolean flatXZ) {
        PolyObjImporter.flatXZ = flatXZ;
    }
}