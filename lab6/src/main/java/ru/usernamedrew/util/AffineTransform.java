package ru.usernamedrew.util;

import ru.usernamedrew.model.Point3D;

import java.util.List;

public class AffineTransform {

    public static double[][] createIdentityMatrix() {
        return new double[][] {
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        };
    }

    public static double[][] createTranslationMatrix(double dx, double dy, double dz) {
        return new double[][] {
                {1, 0, 0, dx},
                {0, 1, 0, dy},
                {0, 0, 1, dz},
                {0, 0, 0, 1}
        };
    }

    public static double[][] createScalingMatrix(double sx, double sy, double sz) {
        return new double[][] {
                {sx, 0, 0, 0},
                {0, sy, 0, 0},
                {0, 0, sz, 0},
                {0, 0, 0, 1}
        };
    }

    public static double[][] createRotationXMatrix(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new double[][] {
                {1, 0, 0, 0},
                {0, cos, -sin, 0},
                {0, sin, cos, 0},
                {0, 0, 0, 1}
        };
    }

    public static double[][] createRotationYMatrix(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new double[][] {
                {cos, 0, sin, 0},
                {0, 1, 0, 0},
                {-sin, 0, cos, 0},
                {0, 0, 0, 1}
        };
    }

    public static double[][] createRotationZMatrix(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new double[][] {
                {cos, -sin, 0, 0},
                {sin, cos, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        };
    }

    public static double[][] multiplyMatrices(double[][] a, double[][] b) {
        double[][] result = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result[i][j] = 0;
                for (int k = 0; k < 4; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return result;
    }

    public static double[][] createReflectionMatrix(String plane) {
        return switch (plane.toLowerCase()) {
            case "xy" -> new double[][] {
                    {1, 0, 0, 0},
                    {0, 1, 0, 0},
                    {0, 0, -1, 0},
                    {0, 0, 0, 1}
            };
            case "xz" -> new double[][] {
                    {1, 0, 0, 0},
                    {0, -1, 0, 0},
                    {0, 0, 1, 0},
                    {0, 0, 0, 1}
            };
            case "yz" -> new double[][] {
                    {-1, 0, 0, 0},
                    {0, 1, 0, 0},
                    {0, 0, 1, 0},
                    {0, 0, 0, 1}
            };
            default -> createIdentityMatrix();
        };
    }

    public static Point3D getCenter(List<Point3D> points) {
        double sumX = 0, sumY = 0, sumZ = 0;
        for (Point3D vertex : points) {
            sumX += vertex.x();
            sumY += vertex.y();
            sumZ += vertex.z();
        }
        int count = points.size();
        return new Point3D(sumX / count, sumY / count, sumZ / count);
    }

    public static double[][] createAxonometricProjectionMatrix(double angle) {
        double cosA = Math.cos(angle);
        double sinA = Math.sin(angle);
        double factor = 0.5;

        return new double[][] {
                {cosA, 0, sinA, 0},
                {sinA * factor, 1, -cosA * factor, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 1}
        };
    }

    public static double[][] createPerspectiveProjectionMatrix(double distance) {
        return new double[][] {
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, -1.0 / distance, 1}
        };
    }
}
