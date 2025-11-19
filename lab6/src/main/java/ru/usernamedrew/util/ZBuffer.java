// ZBuffer.java - отдельный класс для реализации алгоритма z-буфера
package ru.usernamedrew.util;

import ru.usernamedrew.model.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

public class ZBuffer {
    private double[][] zBuffer;
    private Color[][] frameBuffer;
    private int width, height;

    private Camera camera = null;

    public ZBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        initializeBuffers();
    }

    private void initializeBuffers() {
        zBuffer = new double[width][height];
        frameBuffer = new Color[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                zBuffer[x][y] = Double.MAX_VALUE;
                frameBuffer[x][y] = null;
            }
        }
    }

    public void clear() {
        initializeBuffers();
    }

    public void renderScene(List<Polyhedron> scene, ProjectionTransformer projector) {
        for (Polyhedron p : scene) {
            renderPolyhedron(p, projector);
        }
    }

    private void renderPolyhedron(Polyhedron polyhedron, ProjectionTransformer projector) {
        if (polyhedron == null) return;

        for (Face face : polyhedron.getFaces()) {
            rasterizeFace(face, projector);
        }
    }

    private void rasterizeFace(Face face, ProjectionTransformer projector) {
        List<Point3D> vertices = face.getVertices();
        if (vertices.size() < 3) return;

        // Вычисляем цвет грани (плоское затенение)
        Point3D normal = face.getNormal();
        Color faceColor = calculateFaceColor(normal);

        // Подготавливаем вершины (экранные координаты + глубина)
        Point3D[] screenVerts = new Point3D[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            Point3D v = vertices.get(i);
            Point2D p2d = projector.project(v);

            double depth;
            if (camera != null) {
                // Z в пространстве камеры (отрицательный перед камерой, поэтому берем -Z для дистанции)
                depth = -v.transform(camera.getViewMatrix()).z();
            } else {
                // Старый режим без камеры
                depth = -v.z();
            }

            screenVerts[i] = new Point3D(p2d.getX(), p2d.getY(), depth);
        }

        // Триангуляция грани (Triangle Fan) для поддержки многоугольников > 3 вершин
        // 0-1-2, 0-2-3, 0-3-4...
        for (int i = 1; i < vertices.size() - 1; i++) {
            drawTriangle(screenVerts[0], screenVerts[i], screenVerts[i + 1], faceColor);
        }
    }

    private void drawTriangle(Point3D v1, Point3D v2, Point3D v3, Color color) {
        // Ограничивающий прямоугольник треугольника
        int minX = (int) Math.max(0, Math.min(v1.x(), Math.min(v2.x(), v3.x())));
        int maxX = (int) Math.min(width - 1, Math.ceil(Math.max(v1.x(), Math.max(v2.x(), v3.x()))));
        int minY = (int) Math.max(0, Math.min(v1.y(), Math.min(v2.y(), v3.y())));
        int maxY = (int) Math.min(height - 1, Math.ceil(Math.max(v1.y(), Math.max(v2.y(), v3.y()))));

        // Проход по пикселям
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                // Вычисляем барицентрические координаты
                Point3D bary = barycentric(v1, v2, v3, new Point2D.Double(x, y));

                // Если точка внутри треугольника (все координаты >= 0)
                if (bary.x() >= 0 && bary.y() >= 0 && bary.z() >= 0) {
                    // Интерполяция глубины: z = w1*z1 + w2*z2 + w3*z3
                    double depth = bary.x() * v1.z() + bary.y() * v2.z() + bary.z() * v3.z();

                    // Z-Test: если новый пиксель ближе (меньше Z), рисуем его
                    if (depth < zBuffer[x][y]) {
                        zBuffer[x][y] = depth;
                        frameBuffer[x][y] = color;
                    }
                }
            }
        }
    }

    // Вычисление барицентрических координат для точки P относительно треугольника ABC
    private Point3D barycentric(Point3D A, Point3D B, Point3D C, Point2D P) {
        Point3D v0 = new Point3D(B.x() - A.x(), C.x() - A.x(), A.x() - P.getX());
        Point3D v1 = new Point3D(B.y() - A.y(), C.y() - A.y(), A.y() - P.getY());

        // Векторное произведение для поиска u, v, 1
        Point3D u = v0.cross(v1);

        // Если u.z близок к 0, значит треугольник вырожденный
        if (Math.abs(u.z()) < 1e-2) {
            return new Point3D(-1, 1, 1);
        }

        double w = 1.0 - (u.x() + u.y()) / u.z();
        double v = u.y() / u.z();
        double t = u.x() / u.z(); // t это третья координата (обычно u, v, w)

        // Возвращаем веса (1-v-t, t, v) -> (wA, wB, wC)
        return new Point3D(w, t, v);
    }

    private Color calculateFaceColor(Point3D normal) {
        // Простое освещение
        Point3D lightDir = new Point3D(0, 0, -1).normalize(); // Свет светит от камеры
        double intensity = Math.max(0.2, normal.dot(lightDir)); // Ambient 0.2
        intensity = Math.min(1.0, intensity);

        int val = (int) (255 * intensity);
        return new Color(val, val, val);
    }

    public void display(Graphics2D g2d, Color backgroundColor) {
        // Отрисовка буфера кадра на экран
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (frameBuffer[x][y] != null) {
                    g2d.setColor(frameBuffer[x][y]);
                    g2d.drawLine(x, y, x, y);
                }
            }
        }
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
