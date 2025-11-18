// ProjectionTransformer.java - класс для проекционных преобразований
package ru.usernamedrew.util;

import ru.usernamedrew.model.Point3D;
import java.awt.geom.Point2D;

public class ProjectionTransformer {
    private String projectionType;
    private double scale;
    private int centerX, centerY;

    public ProjectionTransformer(String projectionType, double scale, int centerX, int centerY) {
        this.projectionType = projectionType;
        this.scale = scale;
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public Point2D project(Point3D point3d) {
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