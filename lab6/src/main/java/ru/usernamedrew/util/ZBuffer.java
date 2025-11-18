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

    // Параметры освещения
    private Point3D lightDirection;
    private Color ambientColor;
    private Color diffuseColor;

    public ZBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        initializeBuffers();

        // Параметры освещения по умолчанию
        this.lightDirection = new Point3D(0, 0, -1).normalize();
        this.ambientColor = new Color(50, 50, 50);
        this.diffuseColor = new Color(200, 200, 200);
    }

    private void initializeBuffers() {
        zBuffer = new double[width][height];
        frameBuffer = new Color[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                zBuffer[x][y] = Double.NEGATIVE_INFINITY;
                frameBuffer[x][y] = null;
            }
        }
    }

    public void clear() {
        initializeBuffers();
    }

    public void renderPolyhedron(Polyhedron polyhedron, ProjectionTransformer projector) {
        if (polyhedron == null) return;

        for (Face face : polyhedron.getFaces()) {
            rasterizeFace(face, projector);
        }
    }

    private void rasterizeFace(Face face, ProjectionTransformer projector) {
        List<Point3D> vertices = face.getVertices();
        if (vertices.size() < 3) return;

        // Проецируем вершины и вычисляем их глубину
        Point2D[] projected = new Point2D[vertices.size()];
        double[] depths = new double[vertices.size()];

        for (int i = 0; i < vertices.size(); i++) {
            Point3D vertex = vertices.get(i);
            projected[i] = projector.project(vertex);
            depths[i] = -vertex.z(); // Глубина (меньшее z = ближе к наблюдателю)
        }

        // Находим ограничивающий прямоугольник
        int minX = Math.max(0, (int) Math.floor(minX(projected)));
        int maxX = Math.min(width - 1, (int) Math.ceil(maxX(projected)));
        int minY = Math.max(0, (int) Math.floor(minY(projected)));
        int maxY = Math.min(height - 1, (int) Math.ceil(maxY(projected)));

        if (minX >= maxX || minY >= maxY) return;

        // Вычисляем нормаль для освещения
        Point3D normal = face.getNormal();
        double intensity = calculateLighting(normal);

        // Цвет грани с учетом освещения
        Color faceColor = calculateFaceColor(intensity);

        // Растеризация методом сканирующих строк
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (isPointInPolygon(x, y, projected)) {
                    // Интерполяция глубины
                    double depth = interpolateDepth(x, y, projected, depths);

                    // Проверка z-буфера
                    if (depth > zBuffer[x][y]) {
                        zBuffer[x][y] = depth;
                        frameBuffer[x][y] = faceColor;
                    }
                }
            }
        }
    }

    private double minX(Point2D[] points) {
        double min = Double.MAX_VALUE;
        for (Point2D p : points) {
            if (p.getX() < min) min = p.getX();
        }
        return min;
    }

    private double maxX(Point2D[] points) {
        double max = Double.MIN_VALUE;
        for (Point2D p : points) {
            if (p.getX() > max) max = p.getX();
        }
        return max;
    }

    private double minY(Point2D[] points) {
        double min = Double.MAX_VALUE;
        for (Point2D p : points) {
            if (p.getY() < min) min = p.getY();
        }
        return min;
    }

    private double maxY(Point2D[] points) {
        double max = Double.MIN_VALUE;
        for (Point2D p : points) {
            if (p.getY() > max) max = p.getY();
        }
        return max;
    }

    private boolean isPointInPolygon(int x, int y, Point2D[] polygon) {
        int crossings = 0;
        int n = polygon.length;

        for (int i = 0; i < n; i++) {
            Point2D a = polygon[i];
            Point2D b = polygon[(i + 1) % n];

            if ((a.getY() <= y && b.getY() > y) || (b.getY() <= y && a.getY() > y)) {
                double t = (y - a.getY()) / (b.getY() - a.getY());
                double intersectX = a.getX() + t * (b.getX() - a.getX());

                if (intersectX > x) {
                    crossings++;
                }
            }
        }

        return (crossings % 2) == 1;
    }

    private double interpolateDepth(int x, int y, Point2D[] projected, double[] depths) {
        // Упрощенная интерполяция - среднее значение по вершинам
        double sum = 0;
        int count = 0;

        for (int i = 0; i < projected.length; i++) {
            double dist = Math.sqrt(Math.pow(projected[i].getX() - x, 2) +
                    Math.pow(projected[i].getY() - y, 2));
            if (dist < 1.0) { // Если близко к вершине
                return depths[i];
            }
            sum += depths[i] / (dist + 0.1); // Взвешенное по расстоянию
            count++;
        }

        return sum / count;
    }

    private double calculateLighting(Point3D normal) {
        // Простое затенение по Фонгу
        double diffuse = Math.max(0, normal.x() * lightDirection.x() +
                normal.y() * lightDirection.y() +
                normal.z() * lightDirection.z());

        return Math.min(1.0, 0.2 + 0.8 * diffuse); // ambient + diffuse
    }

    private Color calculateFaceColor(double intensity) {
        int r = Math.min(255, ambientColor.getRed() + (int)(diffuseColor.getRed() * intensity));
        int g = Math.min(255, ambientColor.getGreen() + (int)(diffuseColor.getGreen() * intensity));
        int b = Math.min(255, ambientColor.getBlue() + (int)(diffuseColor.getBlue() * intensity));

        return new Color(r, g, b);
    }

    public void display(Graphics2D g2d, Color backgroundColor) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (frameBuffer[x][y] != null) {
                    g2d.setColor(frameBuffer[x][y]);
                    g2d.drawLine(x, y, x, y);
                } else if (backgroundColor != null) {
                    g2d.setColor(backgroundColor);
                    g2d.drawLine(x, y, x, y);
                }
            }
        }
    }

    // Геттеры и сеттеры для параметров
    public void setLightDirection(Point3D lightDirection) {
        this.lightDirection = lightDirection.normalize();
    }

    public void setAmbientColor(Color ambientColor) {
        this.ambientColor = ambientColor;
    }

    public void setDiffuseColor(Color diffuseColor) {
        this.diffuseColor = diffuseColor;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
