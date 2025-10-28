package ru.usernamedrew.ui;

import ru.usernamedrew.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;

public class GraphicsPanel extends JPanel {
    private Polyhedron polyhedron;
    private String projectionType = "axonometric";
    private double scale = 50;
    private int centerX, centerY;

    public GraphicsPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 600));
    }

    public void setPolyhedron(Polyhedron polyhedron) {
        this.polyhedron = polyhedron;
        repaint();
    }

    public void setProjectionType(String type) {
        this.projectionType = type;
        repaint();
    }

    public void setScale(double scale) {
        this.scale = scale;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        centerX = getWidth() / 2;
        centerY = getHeight() / 2;

        if (polyhedron == null) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Рисуем координатные оси
        drawCoordinateAxes(g2d);

        // Рисуем многогранник
        drawPolyhedron(g2d);
    }

    private void drawCoordinateAxes(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        g2d.drawLine(centerX, centerY, centerX + 100, centerY); // X axis
        g2d.setColor(Color.GREEN);
        g2d.drawLine(centerX, centerY, centerX, centerY - 100); // Y axis
        g2d.setColor(Color.BLUE);
        g2d.drawLine(centerX, centerY, centerX, centerY + 100); // Z axis

        g2d.setColor(Color.BLACK);
        g2d.drawString("X", centerX + 105, centerY);
        g2d.drawString("Y", centerX, centerY - 105);
        g2d.drawString("Z", centerX, centerY + 105);
    }

    private void drawPolyhedron(Graphics2D g2d) {
        if (polyhedron == null) return;

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));

        for (Face face : polyhedron.getFaces()) {
            List<Point3D> vertices = face.getVertices();
            if (vertices.size() < 2) continue;

            Point2D[] points = new Point2D[vertices.size()];
            for (int i = 0; i < vertices.size(); i++) {
                points[i] = projectPoint(vertices.get(i));
            }

            // Рисуем грани как полигоны
            for (int i = 0; i < points.length; i++) {
                Point2D start = points[i];
                Point2D end = points[(i + 1) % points.length];
                g2d.draw(new Line2D.Double(start.getX(), start.getY(), end.getX(), end.getY()));
            }
        }
    }

    private Point2D projectPoint(Point3D point3d) {
        double x = point3d.x() * scale;
        double y = point3d.y() * scale;
        double z = point3d.z() * scale;

        if ("perspective".equals(projectionType)) {
            // Перспективная проекция
            double distance = 5;
            double factor = distance / (distance - z);
            x = x * factor;
            y = y * factor;
        } else {
            // Аксонометрическая проекция
            double angle = Math.PI / 6; // 30 градусов
            double xTemp = x * Math.cos(angle) - z * Math.sin(angle);
            double zTemp = x * Math.sin(angle) + z * Math.cos(angle);
            x = xTemp;
            z = zTemp;
        }

        return new Point2D.Double(centerX + x, centerY - y);
    }
}
