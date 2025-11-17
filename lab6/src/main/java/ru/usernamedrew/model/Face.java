package ru.usernamedrew.model;

import ru.usernamedrew.util.AffineTransform;

import java.util.ArrayList;
import java.util.List;

public class Face {
    private final List<Point3D> vertices;
    private final List<Integer> verticesIndices;
    private Point3D normal; // Добавляем нормаль грани

    public Face() {
        vertices = new ArrayList<>();
        verticesIndices = new ArrayList<>();
    }

    public Face(List<Point3D> vertices) {
        this.vertices = vertices;
        verticesIndices = new ArrayList<>();
        computeRawNormal(); // Вычисляем нормаль при создании
    }

    public List<Point3D> getVertices() {
        return vertices;
    }

    public List<Integer> getVerticesIndices() {
        return verticesIndices;
    }

    public Point3D getNormal() {
        return normal;
    }

    public void addVertex(Point3D vertex) {
        vertices.add(vertex);
        if (vertices.size() >= 3) {
            computeRawNormal(); // Пересчитываем нормаль при добавлении вершин
        }
    }

    public void computeRawNormal() {
        if (vertices.size() < 3) {
            normal = new Point3D(0, 0, 1);
            return;
        }

        // Берем первые три точки для вычисления нормали
        Point3D v1 = vertices.get(0);
        Point3D v2 = vertices.get(1);
        Point3D v3 = vertices.get(2);

        // Векторы в плоскости грани
        Point3D vec1 = v2.subtract(v1);
        Point3D vec2 = v3.subtract(v1);

        // Векторное произведение
        double nx = vec1.y() * vec2.z() - vec1.z() * vec2.y();
        double ny = vec1.z() * vec2.x() - vec1.x() * vec2.z();
        double nz = vec1.x() * vec2.y() - vec1.y() * vec2.x();

        // Нормализуем вектор
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0) {
            nx /= length;
            ny /= length;
            nz /= length;
        }

        normal = new Point3D(nx, ny, nz);
    }

    public void addVertexIndice(int vertexIndex) {
        verticesIndices.add(vertexIndex);
    }

    // Вычисление нормали грани
    public void orientNormal(Point3D objectCenter) {
        if (normal == null) {
            computeRawNormal(); // На всякий случай
        }

        Point3D faceCenter = AffineTransform.getCenter(vertices);

        Point3D centerToFace = faceCenter.subtract(objectCenter);

        // Скалярное произведение
        double dot = normal.dot(centerToFace);

        if (dot < 0) {
            normal = new Point3D(-normal.x(), -normal.y(), -normal.z());
        }
    }

    // Поверхностное копирование как и в классе многогранника
    public Face copy() {
        Face face = new Face();

        face.vertices.addAll(vertices);
        face.verticesIndices.addAll(verticesIndices);
        face.normal = normal; // Копируем нормаль

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
