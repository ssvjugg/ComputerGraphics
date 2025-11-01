// PolyhedronIO.java - полностью переработанный класс для формата OBJ
package ru.usernamedrew.util;

import ru.usernamedrew.model.*;

import java.io.*;
import java.util.List;

public class PolyhedronIO {

    public static void saveToFile(Polyhedron polyhedron, String filename) throws IOException {
        // Добавляем расширение .obj если его нет
        if (!filename.toLowerCase().endsWith(".obj")) {
            filename += ".obj";
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Записываем комментарий
            writer.println("# 3D Model exported from Java 3D Application");
            writer.println("# Vertices: " + polyhedron.getVertices().size());
            writer.println("# Faces: " + polyhedron.getFaces().size());
            writer.println();

            // Записываем вершины (v x y z)
            writer.println("# Vertex list");
            for (Point3D vertex : polyhedron.getVertices()) {
                writer.printf("v %.6f %.6f %.6f%n", vertex.x(), vertex.y(), vertex.z());
            }

            writer.println();

            // Записываем грани (f v1 v2 v3 ...)
            writer.println("# Face list");
            for (Face face : polyhedron.getFaces()) {
                StringBuilder faceLine = new StringBuilder("f");
                for (Point3D vertex : face.getVertices()) {
                    int vertexIndex = polyhedron.getVertices().indexOf(vertex) + 1; // OBJ индексы начинаются с 1
                    faceLine.append(" ").append(vertexIndex);
                }
                writer.println(faceLine.toString());
            }
        }
    }

    public static Polyhedron loadFromFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            List<Point3D> vertices = new java.util.ArrayList<>();
            List<Face> faces = new java.util.ArrayList<>();

            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Пропускаем комментарии и пустые строки
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length < 2) {
                    continue;
                }

                String keyword = parts[0];

                // Обрабатываем вершины
                if ("v".equals(keyword) && parts.length >= 4) {
                    try {
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        double z = Double.parseDouble(parts[3]);
                        vertices.add(new Point3D(x, y, z));
                    } catch (NumberFormatException e) {
                        System.err.println("Ошибка чтения вершины: " + line);
                    }
                }
                // Обрабатываем грани
                else if ("f".equals(keyword) && parts.length >= 3) {
                    Face face = new Face();
                    for (int i = 1; i < parts.length; i++) {
                        try {
                            // OBJ формат может содержать: vertex/texture/normal
                            // Нам нужен только vertex индекс (первое число)
                            String vertexPart = parts[i].split("/")[0];
                            int vertexIndex = Integer.parseInt(vertexPart) - 1; // OBJ индексы с 1, наши с 0

                            if (vertexIndex >= 0 && vertexIndex < vertices.size()) {
                                face.addVertex(vertices.get(vertexIndex));
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Ошибка чтения грани: " + line);
                        }
                    }
                    if (!face.getVertices().isEmpty()) {
                        faces.add(face);
                    }
                }
            }

            return new Polyhedron(faces, vertices);
        }
    }
}