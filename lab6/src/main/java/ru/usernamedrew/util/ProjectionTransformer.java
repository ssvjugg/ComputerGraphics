// ProjectionTransformer.java - класс для проекционных преобразований
package ru.usernamedrew.util;

import ru.usernamedrew.model.Camera;
import ru.usernamedrew.model.Point3D;
import java.awt.geom.Point2D;

public class ProjectionTransformer {
    private final Camera camera;
    private String projectionType;
    private double scale;
    private int centerX, centerY;

    public ProjectionTransformer(Camera camera, double scale, int centerX, int centerY) {
        this.camera = camera;
        this.scale = scale;
        this.centerX = centerX;
        this.centerY = centerY;
        this.projectionType = null;
    }

    public ProjectionTransformer(String projectionType, double scale, int centerX, int centerY) {
        this.camera = null;
        this.projectionType = projectionType;
        this.scale = scale;
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public Point2D project(Point3D point3d) {
        if (camera != null) {
            return projectWithCamera(point3d);
        } else {
            return projectLegacy(point3d);
        }
    }

    private Point2D projectWithCamera(Point3D point3d) {
        double[][] viewMatrix = camera.getViewMatrix();
        double[][] projMatrix = camera.getPerspectiveMatrix();

        // World → View space
        Point3D viewSpace = point3d.transform(viewMatrix);

        // View → Clip space
        Point3D clipSpace = viewSpace.transform(projMatrix);

        int screenX = centerX + (int) (clipSpace.x() * scale);
        int screenY = centerY - (int) (clipSpace.y() * scale);

        return new Point2D.Double(screenX, screenY);
    }

    private Point2D projectLegacy(Point3D point3d) {
        Point3D scaled = new Point3D(point3d.x() * scale, point3d.y() * scale, point3d.z() * scale);

        double[][] matrix;

        if ("perspective".equals(projectionType)) {
            double distance = 500.0; // можно сделать параметром, но пока фиксировано
            matrix = AffineTransform.createPerspectiveProjectionMatrix(distance);
        } else {
            double angle = Math.PI / 6; // 30 градусов — стандартная аксонометрия
            matrix = AffineTransform.createAxonometricProjectionMatrix(angle);
        }

        Point3D projected = scaled.transform(matrix);

        return new Point2D.Double(
                centerX + projected.x(),
                centerY - projected.y()
        );
    }

    // Геттеры и сеттеры
    public void setProjectionType(String projectionType) {
        this.projectionType = projectionType;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void setCenter(int centerX, int centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }
}