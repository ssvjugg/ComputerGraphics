package ru.usernamedrew.model;

public class Camera {
    private Point3D position;

    private Point3D direction;
    private Point3D right;
    private Point3D up;

    private double yaw;   // вокруг Y
    private double pitch;

    private double fov = Math.PI / 3.0;
    private double aspect = 1.0;
    private double near = 0.1;
    private double far = 1000.0;

    public Camera(Point3D position, double yaw, double pitch) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;

        // начальный direction
        this.direction = new Point3D(0, 0, -1);

        updateVectors();
    }

    public Point3D getPosition() {
        return position;
    }

    public void setPosition(Point3D position) {
        this.position = position;
    }

    public Point3D getUp() {
        return up;
    }

    public void setUp(Point3D up) {
        this.up = up;
    }

    public void setFov(double fov) {
        this.fov = fov;
    }

    public void setAspect(double aspect) {
        this.aspect = aspect;
    }

    public void setNear(double near) {
        this.near = near;
    }

    public void setFar(double far) {
        this.far = far;
    }

    public void updateVectors() {
        pitch = Math.max(-89.0, Math.min(89.0, pitch));

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        direction = new Point3D(
                Math.cos(yawRad) * Math.cos(pitchRad),
                Math.sin(pitchRad),
                Math.sin(yawRad) * Math.cos(pitchRad)
        ).normalize();

        Point3D worldUp = new Point3D(0, 1, 0);
        right = direction.cross(worldUp).normalize();

        up = right.cross(direction).normalize();
    }

    public double[][] getViewMatrix() {
        return new double[][]{
                { right.x(), right.y(), right.z(), -right.dot(position) },
                { up.x(),    up.y(),    up.z(),    -up.dot(position) },
                { -direction.x(), -direction.y(), -direction.z(), direction.dot(position) },
                { 0, 0, 0, 1 }
        };
    }

    public double[][] getPerspectiveMatrix() {
        double f = 1.0 / Math.tan(fov / 2.0);
        double nf = 1.0 / (near - far);

        return new double[][]{
                { f / aspect, 0, 0, 0 },
                { 0, f, 0, 0 },
                { 0, 0, (far + near) * nf, -1 },
                { 0, 0, (2 * far * near) * nf, 0 }
        };
    }

    public void move(Point3D delta) {
        position = position.add(delta);
    }

    public void moveForward(double amount) {
        position = position.add(direction.multiply(amount));
    }

    public void moveRight(double amount) {
        position = position.add(right.multiply(amount));
    }

    public void moveUp(double amount) {
        position = position.add(up.multiply(amount));
    }

    public void rotate(double dyaw, double dpitch) {
        yaw += dyaw;
        pitch += dpitch;
        updateVectors();
    }
}
