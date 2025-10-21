package ru.usernamedrew.bezier;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class BezierSpline extends JPanel {
    private List<Point2D.Double> controlPoints = new ArrayList<>();
    private Point2D.Double selectedPoint = null;
    private boolean showControlLines = true;
    private boolean showControlPoints = true;

    // Цвета
    private final Color CURVE_COLOR = new Color(0, 100, 200);
    private final Color CONTROL_LINE_COLOR = new Color(150, 150, 150);
    private final Color CONTROL_POINT_COLOR = new Color(200, 50, 50);
    private final Color CONTROL_POINT_SELECTED_COLOR = new Color(255, 0, 0);

    public BezierSpline() {
        setBackground(Color.WHITE);
        setupMouseListeners();
    }

    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePress(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                selectedPoint = null;
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDrag(e);
            }
        });
    }

    private void handleMousePress(MouseEvent e) {
        Point2D.Double clickPoint = new Point2D.Double(e.getX(), e.getY());

        // Проверяем, кликнули ли на существующую точку
        for (Point2D.Double point : controlPoints) {
            if (point.distance(clickPoint) < 8) {
                selectedPoint = point;
                repaint();
                return;
            }
        }

        // Если не нашли точку и нажат Ctrl - удаляем ближайшую точку
        if (e.isControlDown()) {
            deleteNearestPoint(clickPoint);
            return;
        }

        // Добавляем новую точку
        controlPoints.add(clickPoint);
        selectedPoint = clickPoint;
        repaint();
    }

    private void handleMouseDrag(MouseEvent e) {
        if (selectedPoint != null) {
            selectedPoint.setLocation(e.getX(), e.getY());
            repaint();
        }
    }

    private void deleteNearestPoint(Point2D.Double clickPoint) {
        if (controlPoints.isEmpty()) return;

        Point2D.Double nearest = controlPoints.get(0);
        double minDistance = nearest.distance(clickPoint);

        for (Point2D.Double point : controlPoints) {
            double distance = point.distance(clickPoint);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = point;
            }
        }

        if (minDistance < 15) {
            controlPoints.remove(nearest);
            if (selectedPoint == nearest) {
                selectedPoint = null;
            }
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (controlPoints.size() >= 2) {
            drawBezierSpline(g2d);
        }

        if (showControlPoints) {
            drawControlPoints(g2d);
        }
    }

    private void drawBezierSpline(Graphics2D g2d) {
        // Рисуем контрольные линии
        if (showControlLines && controlPoints.size() >= 2) {
            g2d.setColor(CONTROL_LINE_COLOR);
            g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{5, 5}, 0));

            for (int i = 0; i < controlPoints.size() - 1; i++) {
                Point2D.Double p1 = controlPoints.get(i);
                Point2D.Double p2 = controlPoints.get(i + 1);
                g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
            }
        }

        // Рисуем кривые Безье
        if (controlPoints.size() >= 4) {
            g2d.setColor(CURVE_COLOR);
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (int i = 0; i <= controlPoints.size() - 4; i += 3) {
                drawCubicBezier(g2d,
                        controlPoints.get(i),
                        controlPoints.get(i + 1),
                        controlPoints.get(i + 2),
                        controlPoints.get(i + 3)
                );
            }
        }
    }

    private void drawCubicBezier(Graphics2D g2d, Point2D.Double p0, Point2D.Double p1, Point2D.Double p2, Point2D.Double p3) {
        Point2D.Double prev = p0;

        for (double t = 0.01; t <= 1.0; t += 0.01) {
            Point2D.Double current = calculateBezierPoint(p0, p1, p2, p3, t);
            g2d.drawLine((int) prev.x, (int) prev.y, (int) current.x, (int) current.y);
            prev = current;
        }
    }

    private Point2D.Double calculateBezierPoint(Point2D.Double p0, Point2D.Double p1, Point2D.Double p2, Point2D.Double p3, double t) {
        double u = 1 - t;
        double tt = t * t;
        double uu = u * u;
        double uuu = uu * u;
        double ttt = tt * t;

        double x = uuu * p0.x +
                3 * uu * t * p1.x +
                3 * u * tt * p2.x +
                ttt * p3.x;

        double y = uuu * p0.y +
                3 * uu * t * p1.y +
                3 * u * tt * p2.y +
                ttt * p3.y;

        return new Point2D.Double(x, y);
    }

    private void drawControlPoints(Graphics2D g2d) {
        for (Point2D.Double point : controlPoints) {
            if (point == selectedPoint) {
                g2d.setColor(CONTROL_POINT_SELECTED_COLOR);
                g2d.fillOval((int) point.x - 6, (int) point.y - 6, 12, 12);
            } else {
                g2d.setColor(CONTROL_POINT_COLOR);
                g2d.fillOval((int) point.x - 5, (int) point.y - 5, 10, 10);
            }

            // Белая обводка для лучшей видимости
            g2d.setColor(Color.WHITE);
            g2d.drawOval((int) point.x - 5, (int) point.y - 5, 10, 10);
        }
    }

    public void clearPoints() {
        controlPoints.clear();
        selectedPoint = null;
        repaint();
    }

    public void setShowControlLines(boolean show) {
        this.showControlLines = show;
        repaint();
    }

    public void setShowControlPoints(boolean show) {
        this.showControlPoints = show;
        repaint();
    }
}
