package ru.usernamedrew.model;

import ru.usernamedrew.util.AffineTransform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Polyhedron {
    private final List<Face> faces;
    private final List<Point3D> vertices;

    public Polyhedron() {
        faces = new ArrayList<>();
        vertices = new ArrayList<>();
    }

    public Polyhedron(List<Face> faces, List<Point3D> vertices) {
        this.faces = faces;
        this.vertices = vertices;
    }

    public List<Face> getFaces() {
        return faces;
    }

    public List<Point3D> getVertices() {
        return vertices;
    }

    public void addFace(Face face) {
        faces.add(face);
    }

    public void addVertex(Point3D point) {
        vertices.add(point);
    }

    public Polyhedron copy() {
        Polyhedron polyhedron = new Polyhedron();

        polyhedron.faces.addAll(faces);
        polyhedron.vertices.addAll(vertices);

        return polyhedron;
    }

    public Polyhedron transform(double[][] matrix) {
        Polyhedron polyhedron = new Polyhedron();

        for (Point3D point : vertices) {
            polyhedron.addVertex(point.transform(matrix));
        }

        for (Face face : faces) {
            polyhedron.addFace(face.transform(matrix));
        }

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
        return AffineTransform.getCenter(new java.util.ArrayList<>(uniqueVertices));
    }

    public void recalculateNormals() {
        Point3D objectCenter = getCenter();
        for (Face face : getFaces()) {
            face.computeRawNormal(); // Сначала базовая нормаль
            face.orientNormal(objectCenter); // Затем ориентация
        }
    }
}
