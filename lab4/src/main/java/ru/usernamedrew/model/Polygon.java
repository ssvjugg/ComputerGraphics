package ru.usernamedrew.model;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class Polygon {
    private final List<Point2D.Double> vertices;
    private final Color color;

    public Polygon() {
        this.vertices = new ArrayList<>();
        this.color = new Color(
                (int) (Math.random() * 255),
                (int) (Math.random() * 255),
                (int) (Math.random() * 255)
        );
    }

    public void addVertex(Point2D.Double point) {
        vertices.add(point);
    }

    public List<Point2D.Double> getVertices() {
        return vertices;
    }

    public Color getColor() {
        return color;
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public void clear() {
        vertices.clear();
    }

    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    /**
     * Проверяет, является ли полигон выпуклым
     * Берутся три последовательные точки полигона (A, B, C)
     * Вычисляется векторное произведение векторов AB и BC
     * Если все произведения одного знака - полигон выпуклый
     * Если есть разные знаки - полигон невыпуклый
     */
    public boolean isConvex() {
        if (vertices.size() < 3) return false;

        boolean hasPositive = false;
        boolean hasNegative = false;

        for (int i = 0; i < vertices.size(); i++) {
            Point2D.Double a = vertices.get(i);
            Point2D.Double b = vertices.get((i + 1) % vertices.size());
            Point2D.Double c = vertices.get((i + 2) % vertices.size());

            double crossProduct = crossProduct(a, b, c);

            if (crossProduct > 0) hasPositive = true;
            if (crossProduct < 0) hasNegative = true;

            if (hasPositive && hasNegative) return false;
        }

        return true;
    }

    // Векторное произведение для определения ориентации
    private double crossProduct(Point2D.Double a, Point2D.Double b, Point2D.Double c) {
        return (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x);
    }

    /**
     * Определяет положение точки относительно ребра
     *
     * @return >0 - слева, <0 - справа, =0 - на прямой
     */
    public double pointRelativeToEdge(Point2D.Double a, Point2D.Double b, Point2D.Double p) {
        return (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
    }

    // проверяет принадлежность точки полигону (алгоритм с использованием ray casting)
    // Как работает:
    // Из точки пускается луч вправо
    // Считается количество пересечений луча с рёбрами полигона
    // Нечётное число пересечений - точка внутри
    // Чётное число - точка снаружи
    public boolean containsPoint(Point2D.Double testPoint) {
        if (vertices.size() < 3) return false;

        boolean contains = false;

        for (int i = 0, j = vertices.size() - 1; i < vertices.size(); j = i++) {
            Point2D.Double vi = vertices.get(i);
            Point2D.Double vj = vertices.get(j);

            if (((vi.y > testPoint.y) != (vj.y > testPoint.y)) &&
                    (testPoint.x < (vj.x - vi.x) * (testPoint.y - vi.y) / (vj.y - vi.y) + vi.x)) {
                contains = !contains;
            }
        }

        return contains;
    }

    //Получает классификацию положения точки относительно каждого ребра
    public List<String> getPointEdgeClassifications(Point2D.Double testPoint) {
        List<String> classifications = new ArrayList<>();

        for (int i = 0; i < vertices.size(); i++) {
            Point2D.Double a = vertices.get(i);
            Point2D.Double b = vertices.get((i + 1) % vertices.size());

            double result = pointRelativeToEdge(a, b, testPoint);
            String classification;

            if (result > 0) classification = "СЛЕВА от ребра " + (i + 1);
            else if (result < 0) classification = "СПРАВА от ребра " + (i + 1);
            else classification = "НА ребре " + (i + 1);

            classifications.add(classification);
        }

        return classifications;
    }

    // Проверяет положение произвольной точки относительно конкретного ребра
    public String checkPointAgainstEdge(Point2D.Double testPoint, int edgeIndex) {
        if (vertices.size() < 2 || edgeIndex < 0 || edgeIndex >= vertices.size()) {
            return "Некорректный индекс ребра";
        }

        Point2D.Double a = vertices.get(edgeIndex);
        Point2D.Double b = vertices.get((edgeIndex + 1) % vertices.size());

        double result = pointRelativeToEdge(a, b, testPoint);

        if (result > 0) {
            return String.format("Точка (%.1f, %.1f) находится СЛЕВА от ребра %d",
                    testPoint.x, testPoint.y, edgeIndex + 1);
        } else if (result < 0) {
            return String.format("Точка (%.1f, %.1f) находится СПРАВА от ребра %d",
                    testPoint.x, testPoint.y, edgeIndex + 1);
        } else {
            return String.format("Точка (%.1f, %.1f) находится НА ребре %d",
                    testPoint.x, testPoint.y, edgeIndex + 1);
        }
    }

    private Point2D.Double multiplyMatrixByVector(double[][] matrix, Point2D.Double point) {
        double x = point.x;
        double y = point.y;

        double newX = matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] * 1;
        double newY = matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] * 1;

        return new Point2D.Double(newX, newY);
    }

    public void transform(double[][] transformMatrix) {
        List<Point2D.Double> newVertices = new ArrayList<>();
        for (Point2D.Double vertex : vertices) {
            newVertices.add(multiplyMatrixByVector(transformMatrix, vertex));
        }
        vertices.clear();
        vertices.addAll(newVertices);
    }

    public Point2D.Double getCenter() {
        double centerX = 0, centerY = 0;
        if (vertices.isEmpty()) return new Point2D.Double(0, 0);

        for (Point2D.Double vertex : vertices) {
            centerX += vertex.x;
            centerY += vertex.y;
        }
        return new Point2D.Double(centerX / vertices.size(), centerY / vertices.size());
    }

    public double dot(Point2D.Double p1, Point2D.Double p2) {
        return p1.x * p2.x + p1.y * p2.y;
    }

    public Point2D.Double subtract(Point2D.Double p1, Point2D.Double p2) {
        return new Point2D.Double(p1.x - p2.x, p1.y - p2.y);
    }

    public Point2D.Double getNormal(Point2D p){
        return new Point2D.Double(-p.getY(), p.getX());
    }

    public Point2D.Double findIntersection(Point2D.Double a, Point2D.Double b, Point2D.Double c, Point2D.Double d) {
        Point2D.Double v_ab = subtract(b, a);
        Point2D.Double v_cd = subtract(d, c);
        Point2D.Double v_ac = subtract(a, c);

        Point2D.Double n = getNormal(v_cd);
        double denominator = dot(n, v_ab);

        double epsilon = 1e-9;
        if (Math.abs(denominator) < epsilon) {
            return null;
        }

        double numerator = -dot(n, v_ac);

        double t = numerator / denominator;

        if (t < 0 || t > 1) {
            return null;
        }

        double p_x = a.x + t * v_ab.x;
        double p_y = a.y + t * v_ab.y;
        Point2D.Double intersection = new Point2D.Double(p_x, p_y);

        double u_numerator = dot(v_cd, subtract(intersection, c));
        double u_denominator = dot(v_cd, v_cd);

        if (Math.abs(u_denominator) < epsilon) {
            if (Math.abs(intersection.x - c.x) < epsilon && Math.abs(intersection.y - c.y) < epsilon) {
                return c;
            }
            return null;
        }

        double u = u_numerator / u_denominator;

        if (u >= 0 && u <= 1) {
            return intersection;
        }

        return null;
    }
}