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
                (int)(Math.random() * 255),
                (int)(Math.random() * 255),
                (int)(Math.random() * 255)
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
}
