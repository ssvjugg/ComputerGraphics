package ru.usernamedrew.ui;

import ru.usernamedrew.model.*;
import ru.usernamedrew.util.AffineTransform;
import ru.usernamedrew.util.ProjectionTransformer;
import ru.usernamedrew.util.ZBuffer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class GraphicsPanel extends JPanel {
    private final List<Polyhedron> scene = new ArrayList<>();

    private Polyhedron activePolyhedron;

    private String projectionType = "axonometric";
    private double scale = 50;
    private int centerX, centerY;
    private ZBuffer zBuffer;
    private boolean zBufferEnabled = false; // Флаг использования z-буфера
    private boolean backfaceCulling = true; // Флаг отсечения нелицевых граней
    //private Point3D viewVector = new Point3D(0, 0, -1); // Вектор обзора по умолчанию
    private List<Light> lights = new ArrayList<>();

    private Camera camera;

    public GraphicsPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 600));
    }

    public void addPolyhedron(Polyhedron polyhedron) {
        if (polyhedron != null) {
            scene.add(polyhedron);
            activePolyhedron = polyhedron; // Делаем активным последний добавленный
            repaint();
        }
    }

    public boolean isZBufferEnabled() {
        return zBufferEnabled;
    }

    public void setPolyhedron(Polyhedron polyhedron) {

        this.activePolyhedron = polyhedron;

        if (!scene.isEmpty()) {
            scene.set(scene.size() - 1, polyhedron);
        } else {
            scene.add(polyhedron);
        }

        repaint();
    }

    // Метод для полной очистки сцены
    public void clearScene() {
        scene.clear();
        activePolyhedron = null;
        repaint();
    }

    // Метод обновления активного объекта в списке после трансформации
    public void updateActivePolyhedron(Polyhedron transformed) {
        if (activePolyhedron != null && scene.contains(activePolyhedron)) {
            int index = scene.indexOf(activePolyhedron);
            scene.set(index, transformed);
            activePolyhedron = transformed;
            repaint();
        } else {
            // Если сцена пуста или объект потерян
            addPolyhedron(transformed);
        }
    }

    public void setProjectionType(String type) {
        this.projectionType = type;
        repaint();
    }

    public void setScale(double scale) {
        this.scale = scale;
        repaint();
    }

    public void setZBufferEnabled(boolean enabled) {
        this.zBufferEnabled = enabled;
        repaint();
    }

    public void setBackfaceCulling(boolean enabled) {
        this.backfaceCulling = enabled;
        repaint();
    }

    public void setViewVector(Point3D viewVector) {
        //this.viewVector = viewVector.normalize();
        repaint();
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        centerX = getWidth() / 2;
        centerY = getHeight() / 2;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Рисуем координатные оси
        drawCoordinateAxes(g2d);

        if (zBufferEnabled) {
            drawWithZBuffer(g2d);
        } else {
            for (Polyhedron p : scene) {
                drawPolyhedron(g2d, p);
            }
        }
    }

    //отрисовка координатных осей
    private void drawCoordinateAxes(Graphics2D g2d) {
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

    private void drawPolyhedron(Graphics2D g2d, Polyhedron polyhedron) {
        if (polyhedron == null) return;

        g2d.setStroke(new BasicStroke(2));

        int visibleFaces = 0;
        int totalFaces = polyhedron.getFaces().size();

        for (Face face : polyhedron.getFaces()) {
            List<Point3D> vertices = face.getVertices();
            if (vertices.size() < 2) continue;

            // Проверка видимости грани
            boolean isVisible = !backfaceCulling || isFaceVisible(face);
            if (isVisible) {
                visibleFaces++;
            } else {
                continue; // Пропускаем нелицевые грани
            }

            Point2D[] points = new Point2D[vertices.size()];
            for (int i = 0; i < vertices.size(); i++) {
                points[i] = projectPoint(vertices.get(i));
            }

            // Рисуем видимые грани
            g2d.setColor(Color.BLACK);
            for (int i = 0; i < points.length; i++) {
                Point2D start = points[i];
                Point2D end = points[(i + 1) % points.length];
                g2d.draw(new Line2D.Double(start.getX(), start.getY(), end.getX(), end.getY()));
            }
        }

        // Отладочная информация
        if (backfaceCulling) {
            g2d.setColor(Color.RED);
            g2d.drawString(String.format("Грани: %d/%d видимых", visibleFaces, totalFaces), 10, 20);
            //g2d.drawString(String.format("Вектор обзора: (%.2f, %.2f, %.2f)",
                    //viewVector.x(), viewVector.y(), viewVector.z()), 10, 40);
        }
    }

    private void drawWithZBuffer(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();

        if (zBuffer == null || zBuffer.getWidth() != width || zBuffer.getHeight() != height) {
            zBuffer = new ZBuffer(width, height);
        }
        zBuffer.clear();
        zBuffer.setCamera(camera);
        zBuffer.setLights(lights); // Устанавливаем источники света

        ProjectionTransformer projector;
        if (camera != null) {
            projector = new ProjectionTransformer(camera, scale, centerX, centerY);
        } else {
            projector = new ProjectionTransformer(projectionType, scale, centerX, centerY);
        }

        zBuffer.renderScene(scene, projector);
        zBuffer.display(g2d, getBackground());
        drawCoordinateAxes(g2d);
    }

    public void setLights(List<Light> lights) {
        this.lights = lights;
        if (zBuffer != null) {
            zBuffer.setLights(lights);
        }
        repaint();
    }

    // Проверка видимости грани
    private boolean isFaceVisible(Face face) {
        if (face.getVertices().size() < 3) return true;

        Point3D normal = face.getNormal();

        Point3D faceCenter = AffineTransform.getCenter(face.getVertices());
        double dotProduct;

        if ("perspective".equals(projectionType) && camera != null) {
            Point3D cameraPos = camera.getPosition();
            Point3D viewToFace = faceCenter.subtract(cameraPos).normalize();

            dotProduct = normal.dot(viewToFace);

            return dotProduct < 0;
        } else{
            return true;
        }
    }

    // Отрисовка нормалей граней для отладки
    private void drawFaceNormal(Graphics2D g2d, Face face) {
        // Находим центр грани
        Point3D faceCenter = AffineTransform.getCenter(face.getVertices());

        // Конец нормали
        Point3D normalEnd = faceCenter.add(face.getNormal().multiply(0.5));

        // Проецируем точки
        Point2D center2D = projectPoint(faceCenter);
        Point2D normalEnd2D = projectPoint(normalEnd);

        // Рисуем нормаль
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine((int) center2D.getX(), (int) center2D.getY(),
                (int) normalEnd2D.getX(), (int) normalEnd2D.getY());
    }

    // TODO
    private Point2D projectPoint(Point3D point3d) {
        Point3D transformedPoint;
        if ("perspective".equals(projectionType) && camera != null) {
            // Применяем view и perspective матрицы камеры
            double[][] view = camera.getViewMatrix();
            double[][] projection = camera.getPerspectiveMatrix();
            Point3D viewPoint = point3d.transform(view);
            transformedPoint = viewPoint.transform(projection);

            // Преобразуем к экранным координатам
            int screenX = (int) (centerX + transformedPoint.x() * scale);
            int screenY = (int) (centerY - transformedPoint.y() * scale);
            return new Point2D.Double(screenX, screenY);
        }


        Point3D scaledPoint = new Point3D(point3d.x() * scale, point3d.y() * scale, point3d.z() * scale);

        double[][] projectionMatrix;

        if ("perspective".equals(projectionType)) {
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
