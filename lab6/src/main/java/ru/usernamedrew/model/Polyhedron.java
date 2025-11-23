package ru.usernamedrew.model;

import ru.usernamedrew.util.AffineTransform;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Polyhedron {
    private final List<Face> faces;
    private final List<Point3D> vertices;
    private final List<Point3D> vertexNormals;
    private Color color;

    public Polyhedron() {
        faces = new ArrayList<>();
        vertices = new ArrayList<>();
        vertexNormals = new ArrayList<>();
        color = Color.WHITE;
    }

    public Polyhedron(List<Face> faces, List<Point3D> vertices) {
        this.faces = faces;
        this.vertices = vertices;
        this.vertexNormals = new ArrayList<>();
        this.color = Color.WHITE;
        computeVertexNormals();
    }

    public List<Face> getFaces() {
        return faces;
    }

    public List<Point3D> getVertices() {
        return vertices;
    }

    public List<Point3D> getVertexNormals() {
        return vertexNormals;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void addFace(Face face) {
        faces.add(face);
    }

    public void addVertex(Point3D point) {
        vertices.add(point);
        vertexNormals.add(new Point3D(0, 0, 0));
    }

    public Polyhedron copy() {
        Polyhedron polyhedron = new Polyhedron();
        polyhedron.faces.addAll(faces);
        polyhedron.vertices.addAll(vertices);
        polyhedron.vertexNormals.addAll(vertexNormals);
        polyhedron.color = color;
        return polyhedron;
    }

    public Polyhedron transform(double[][] matrix) {
        Polyhedron polyhedron = new Polyhedron();

        for (Point3D point : vertices) {
            polyhedron.addVertex(point.transform(matrix));
        }

        // Трансформируем нормали (только вращение)
        double[][] rotationMatrix = {
                {matrix[0][0], matrix[0][1], matrix[0][2], 0},
                {matrix[1][0], matrix[1][1], matrix[1][2], 0},
                {matrix[2][0], matrix[2][1], matrix[2][2], 0},
                {0, 0, 0, 1}
        };

        for (int i = 0; i < vertexNormals.size(); i++) {
            Point3D transformedNormal = vertexNormals.get(i).transform(rotationMatrix).normalize();
            polyhedron.vertexNormals.set(i, transformedNormal);
        }

        for (Face face : faces) {
            polyhedron.addFace(face.transform(matrix));
        }

        polyhedron.color = this.color;
        return polyhedron;
    }

    public Point3D getCenter() {
        Set<Point3D> uniqueVertices = new HashSet<>();
        for (Face face : getFaces()) {
            uniqueVertices.addAll(face.getVertices());
        }
        if (uniqueVertices.isEmpty()) {
            return new Point3D(0, 0, 0);
        }
        return AffineTransform.getCenter(new ArrayList<>(uniqueVertices));
    }

    public void recalculateNormals() {
        Point3D objectCenter = getCenter();
        for (Face face : getFaces()) {
            face.computeRawNormal();
            face.orientNormal(objectCenter);
        }
        computeVertexNormals();
    }

    public void computeVertexNormals() {
        // Очищаем нормали
        vertexNormals.clear();
        for (int i = 0; i < vertices.size(); i++) {
            vertexNormals.add(new Point3D(0, 0, 0));
        }

        // Суммируем нормали граней
        for (Face face : faces) {
            Point3D faceNormal = face.getNormal();
            for (Point3D vertex : face.getVertices()) {
                int vertexIndex = vertices.indexOf(vertex);
                if (vertexIndex >= 0) {
                    Point3D currentNormal = vertexNormals.get(vertexIndex);
                    vertexNormals.set(vertexIndex, currentNormal.add(faceNormal));
                }
            }
        }

        // Нормализуем
        for (int i = 0; i < vertexNormals.size(); i++) {
            vertexNormals.set(i, vertexNormals.get(i).normalize());
        }
    }

    public Point3D getVertexNormal(int vertexIndex) {
        if (vertexIndex >= 0 && vertexIndex < vertexNormals.size()) {
            return vertexNormals.get(vertexIndex);
        }
        return new Point3D(0, 0, 1);
    }

    public Point3D getVertexNormal(Point3D vertex) {
        int index = vertices.indexOf(vertex);
        return getVertexNormal(index);
    }
}