package ru.usernamedrew.model;

import java.util.ArrayList;
import java.util.List;

public class Face {
    private final List<Point3D> vertices;
    private final List<Integer> verticesIndices;

    public Face() {
        vertices = new ArrayList<>();
        verticesIndices = new ArrayList<>();
    }

    public Face(List<Point3D> vertices) {
        this.vertices = vertices;
        verticesIndices = new ArrayList<>();
    }

    public List<Point3D> getVertices() {
        return vertices;
    }

    public List<Integer> getVerticesIndices() {
        return verticesIndices;
    }

    public void addVertex(Point3D vertex) {
        vertices.add(vertex);
    }

    public void addVertexIndice(int vertexIndex) {
        verticesIndices.add(vertexIndex);
    }

    // Поверхностное копирование как и в классе многогранника
    public Face copy() {
        Face face = new Face();

        face.vertices.addAll(vertices);
        face.verticesIndices.addAll(verticesIndices);

        return face;
    }

    public Face transform(double[][] matrix) {
        Face face = new Face();

        for (Point3D vertex : vertices) {
            face.addVertex(vertex.transform(matrix));
        }
        face.verticesIndices.addAll(verticesIndices);

        return face;
    }
}
