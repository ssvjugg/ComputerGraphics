package ru.usernamedrew.ui;

import ru.usernamedrew.model.*;
import ru.usernamedrew.util.AffineTransform;

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


    //отрисовка координатных осей
    private void drawCoordinateAxes(Graphics2D g2d) {
        //делаем все теже матричные преобразования как и для многогранника
        double axisLength = 3.0;

        Point3D origin3D = new Point3D(0, 0, 0);

        Point3D xAxisEnd3D = new Point3D(axisLength, 0, 0);
        Point3D yAxisEnd3D = new Point3D(0, axisLength, 0);
        Point3D zAxisEnd3D = new Point3D(0, 0, axisLength);

        Point2D origin2D = projectPoint(origin3D);
        Point2D xAxisEnd2D = projectPoint(xAxisEnd3D);
        Point2D yAxisEnd2D = projectPoint(yAxisEnd3D);
        Point2D zAxisEnd2D = projectPoint(zAxisEnd3D);

        int ox = (int) origin2D.getX();
        int oy = (int) origin2D.getY();

        g2d.setColor(Color.RED);
        g2d.drawLine(ox, oy, (int) xAxisEnd2D.getX(), (int) xAxisEnd2D.getY());

        g2d.setColor(Color.GREEN);
        g2d.drawLine(ox, oy, (int) yAxisEnd2D.getX(), (int) yAxisEnd2D.getY());

        g2d.setColor(Color.BLUE);
        g2d.drawLine(ox, oy, (int) zAxisEnd2D.getX(), (int) zAxisEnd2D.getY());

        g2d.setColor(Color.BLACK);

        g2d.drawString("X", (int) xAxisEnd2D.getX() + 5, (int) xAxisEnd2D.getY());

        g2d.drawString("Y", (int) yAxisEnd2D.getX() - 5, (int) yAxisEnd2D.getY() - 5);

        g2d.drawString("Z", (int) zAxisEnd2D.getX() + 5, (int) zAxisEnd2D.getY() + 5);
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
        Point3D scaledPoint = new Point3D(point3d.x() * scale, point3d.y() * scale, point3d.z() * scale);

        double[][] projectionMatrix;

        if ("perspective".equals(projectionType)) {
            // Перспективная проекция
            double distance = 500;
            projectionMatrix = AffineTransform.createPerspectiveProjectionMatrix(distance);
        } else {
            double angle = Math.PI / 6;

            projectionMatrix = AffineTransform.createAxonometricProjectionMatrix(angle);
        }

        Point3D projectedPoint = scaledPoint.transform(projectionMatrix);
        return new Point2D.Double(centerX + projectedPoint.x(), centerY - projectedPoint.y());
    }
}
