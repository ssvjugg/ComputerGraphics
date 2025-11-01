// новый класс для сохранения и загрузки моделей
package ru.usernamedrew.util;

import ru.usernamedrew.model.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PolyhedronIO {

    public static void saveToFile(Polyhedron polyhedron, String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Записываем вершины
            writer.println("VERTICES:");
            for (Point3D vertex : polyhedron.getVertices()) {
                writer.printf("%f %f %f%n", vertex.x(), vertex.y(), vertex.z());
            }

            // Записываем грани
            writer.println("FACES:");
            for (Face face : polyhedron.getFaces()) {
                StringBuilder faceLine = new StringBuilder();
                for (Point3D vertex : face.getVertices()) {
                    int vertexIndex = polyhedron.getVertices().indexOf(vertex);
                    if (vertexIndex != -1) {
                        faceLine.append(vertexIndex).append(" ");
                    }
                }
                writer.println(faceLine.toString().trim());
            }
        }
    }

    public static Polyhedron loadFromFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            List<Point3D> vertices = new ArrayList<>();
            List<Face> faces = new ArrayList<>();

            String line;
            boolean readingVertices = false;
            boolean readingFaces = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.equals("VERTICES:")) {
                    readingVertices = true;
                    readingFaces = false;
                    continue;
                } else if (line.equals("FACES:")) {
                    readingVertices = false;
                    readingFaces = true;
                    continue;
                } else if (line.isEmpty()) {
                    continue;
                }

                if (readingVertices) {
                    String[] coords = line.split("\\s+");
                    if (coords.length == 3) {
                        double x = Double.parseDouble(coords[0]);
                        double y = Double.parseDouble(coords[1]);
                        double z = Double.parseDouble(coords[2]);
                        vertices.add(new Point3D(x, y, z));
                    }
                } else if (readingFaces) {
                    String[] indices = line.split("\\s+");
                    Face face = new Face();
                    for (String indexStr : indices) {
                        int index = Integer.parseInt(indexStr);
                        if (index >= 0 && index < vertices.size()) {
                            face.addVertex(vertices.get(index));
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