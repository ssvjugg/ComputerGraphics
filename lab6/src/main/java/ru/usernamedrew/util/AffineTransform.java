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

    public static double[][] createRotationAroundArbitraryAxis(Point3D A, Point3D V, double angle) {
        // Перенести прямую L в центр координат на –А (-a,-b,-c)
        double[][] T_neg = createTranslationMatrix(-A.x(), -A.y(), -A.z());

        double l = V.x();
        double m = V.y();
        double n = V.z();

        // совмещаем V с плоскостью XZ
        double r = Math.sqrt(m * m + n * n);
        double[][] Rx = createIdentityMatrix();
        double[][] Rx_inv = createIdentityMatrix();

        if (r > 1e-6) {
            double cosAlpha = n / r;
            double sinAlpha = m / r;
            double alpha = Math.atan2(sinAlpha, cosAlpha);

            Rx = createRotationXMatrix(alpha);
            Rx_inv = createRotationXMatrix(-alpha);
        }

        // совмещаем с осью Z
        double[][] Ry = createIdentityMatrix();
        double[][] Ry_inv = createIdentityMatrix();

        if (r > 1e-6) {
            double cosBeta = r;
            double sinBeta = -l;
            double beta = Math.atan2(sinBeta, cosBeta);

            Ry = createRotationYMatrix(beta);
            Ry_inv = createRotationYMatrix(-beta);
        }

        // поворачиваем на угол
        double[][] Rz = createRotationZMatrix(angle);

        // перенос обратно
        double[][] T_pos = createTranslationMatrix(A.x(), A.y(), A.z());

        // объединяем все матрицы
        double[][] M1 = multiplyMatrices(Rx, T_neg);
        double[][] M2 = multiplyMatrices(Ry, M1);

        double[][] M3 = multiplyMatrices(Rz, M2);

        double[][] M4 = multiplyMatrices(Rx_inv, M3);
        double[][] M5 = multiplyMatrices(Ry_inv, M4);

        double[][] M_total = multiplyMatrices(T_pos, M5);

        return M_total;
    }
}
