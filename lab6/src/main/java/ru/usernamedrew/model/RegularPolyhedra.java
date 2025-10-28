package ru.usernamedrew.model;

import java.util.ArrayList;
import java.util.List;

public class RegularPolyhedra {

    public static Polyhedron createTetrahedron() {
        Polyhedron tetrahedron = new Polyhedron();

        // Вершины тетраэдра
        Point3D v1 = new Point3D(0, 0, 0);
        Point3D v2 = new Point3D(1, 0, 0);
        Point3D v3 = new Point3D(0.5, Math.sqrt(3)/2, 0);
        Point3D v4 = new Point3D(0.5, Math.sqrt(3)/6, Math.sqrt(6)/3);

        tetrahedron.addVertex(v1);
        tetrahedron.addVertex(v2);
        tetrahedron.addVertex(v3);
        tetrahedron.addVertex(v4);

        // Грани
        Face face1 = new Face(List.of(v1, v2, v3));
        Face face2 = new Face(List.of(v1, v2, v4));
        Face face3 = new Face(List.of(v1, v3, v4));
        Face face4 = new Face(List.of(v2, v3, v4));

        tetrahedron.addFace(face1);
        tetrahedron.addFace(face2);
        tetrahedron.addFace(face3);
        tetrahedron.addFace(face4);

        return tetrahedron;
    }

    public static Polyhedron createHexahedron() {
        Polyhedron hexahedron = new Polyhedron();

        // Вершины куба
        List<Point3D> vertices = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    vertices.add(new Point3D(i, j, k));
                }
            }
        }

        vertices.forEach(hexahedron::addVertex);

        // Грани куба
        int[][] faces = {
                {0, 1, 3, 2}, {4, 5, 7, 6}, // front, back
                {0, 1, 5, 4}, {2, 3, 7, 6}, // bottom, top
                {0, 2, 6, 4}, {1, 3, 7, 5}  // left, right
        };

        for (int[] faceIndices : faces) {
            Face face = new Face();
            for (int index : faceIndices) {
                face.addVertex(vertices.get(index));
            }
            hexahedron.addFace(face);
        }

        return hexahedron;
    }

    public static Polyhedron createOctahedron() {
        Polyhedron octahedron = new Polyhedron();

        // Вершины октаэдра
        Point3D[] vertices = {
                new Point3D(0, 0, 1),   // верхняя
                new Point3D(1, 0, 0),   // передняя-правая
                new Point3D(0, 1, 0),   // передняя-левая
                new Point3D(-1, 0, 0),  // задняя-левая
                new Point3D(0, -1, 0),  // задняя-правая
                new Point3D(0, 0, -1)   // нижняя
        };

        for (Point3D vertex : vertices) {
            octahedron.addVertex(vertex);
        }

        // Грани октаэдра
        int[][] faces = {
                {0, 1, 2}, {0, 2, 3}, {0, 3, 4}, {0, 4, 1}, // верхние
                {5, 2, 1}, {5, 3, 2}, {5, 4, 3}, {5, 1, 4}  // нижние
        };

        for (int[] faceIndices : faces) {
            Face face = new Face();
            for (int index : faceIndices) {
                face.addVertex(vertices[index]);
            }
            octahedron.addFace(face);
        }

        return octahedron;
    }
}
