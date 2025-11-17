package ru.usernamedrew.model;

public record Point3D(double x, double y, double z) {
    private static final double EPSILON = 1e-9;

    // Аффинные преобразования
    public Point3D transform(double[][] matrix) {
        double xNew = matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] * z + matrix[0][3];
        double yNew = matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] * z + matrix[1][3];
        double zNew = matrix[2][0] * x + matrix[2][1] * y + matrix[2][2] * z + matrix[2][3];
        double w = matrix[3][0] * x + matrix[3][1] * y + matrix[3][2] * z + matrix[3][3];

        if (Math.abs(w - 1.0) > EPSILON) {
            if (Math.abs(w) > EPSILON) {
                xNew /= w;
                yNew /= w;
                zNew /= w;
            }
        }

        return new Point3D(xNew, yNew, zNew);
    }

    public Point3D subtract(Point3D other) {
        return new Point3D(x - other.x, y - other.y, z - other.z);
    }

    public Point3D add(Point3D other) {
        return new Point3D(x + other.x, y + other.y, z + other.z);
    }

    public Point3D multiply(double scalar) {
        return new Point3D(x * scalar, y * scalar, z * scalar);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Point3D normalize() {
        double len = length();
        if (len == 0) return this;
        return new Point3D(x / len, y / len, z / len);
    }

    public double dot(Point3D o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public Point3D cross(Point3D o) {
        double cx = y * o.z - z * o.y;
        double cy = z * o.x - x * o.z;
        double cz = x * o.y - y * o.x;
        return new Point3D(cx, cy, cz);
    }

    public double distanceTo(Point3D o) {
        double dx = x - o.x;
        double dy = y - o.y;
        double dz = z - o.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
