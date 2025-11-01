package ru.usernamedrew.model;

import java.util.List;
import java.util.function.BiFunction;

public class SurfaceFactory {
    public static Polyhedron createSurface(BiFunction<Double, Double, Double> function, double x0, double x1, double y0, double y1, int nx, int ny) {
        Polyhedron surface = new Polyhedron();
        Point3D[][] grid = new Point3D[nx + 1][ny + 1];

        //шаги
        double dx = (x1 - x0) / nx;
        double dy = (y1 - y0) / ny;

        //генерация вершин
        for (int i = 0; i <= nx; i++) {
            double x = x0 + i * dx;
            for (int j = 0; j <= ny; j++) {
                double y = y0 + j * dy;
                double z = function.apply(x, y);
                Point3D vertex = new Point3D(x, y, z);
                grid[i][j] = vertex;
                surface.addVertex(vertex);
            }
        }

        //генерация граней
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                Point3D v1 = grid[i][j];
                Point3D v2 = grid[i + 1][j];
                Point3D v3 = grid[i + 1][j + 1];
                Point3D v4 = grid[i][j + 1];
                Face face = new Face(List.of(v1, v2, v3, v4));
                surface.addFace(face);
            }
        }
        return surface;
    }

    public static Double paraboloid(Double x, Double y) {
        return x * x + y * y;
    }

    public static Double sinCosSurface(Double x, Double y) {
        return Math.sin(x) * Math.cos(y);
    }
}
