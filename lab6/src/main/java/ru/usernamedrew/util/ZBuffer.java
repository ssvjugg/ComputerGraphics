package ru.usernamedrew.util;

import ru.usernamedrew.model.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class ZBuffer {
    private double[][] zBuffer;
    private Color[][] frameBuffer;
    private int width, height;

    private Camera camera = null;
    private List<Light> lights = new ArrayList<>();
    private Color ambientLight = new Color(50, 50, 50);

    public ZBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        initializeBuffers();
        setupDefaultLighting();
    }

    private void setupDefaultLighting() {
        // Окружающий свет
        lights.add(new Light(new Color(255, 255, 255), 0.3));
        // Направленный свет
        lights.add(new Light(new Point3D(1, 1, -1), new Color(255, 255, 255), 0.7));
    }

    private void initializeBuffers() {
        zBuffer = new double[width][height];
        frameBuffer = new Color[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                zBuffer[x][y] = Double.MAX_VALUE;
                frameBuffer[x][y] = null;
            }
        }
    }

    public void clear() {
        initializeBuffers();
    }

    public void renderScene(List<Polyhedron> scene, ProjectionTransformer projector) {
        for (Polyhedron p : scene) {
            renderPolyhedron(p, projector);
        }
    }

    private void renderPolyhedron(Polyhedron polyhedron, ProjectionTransformer projector) {
        if (polyhedron == null) return;
        for (Face face : polyhedron.getFaces()) {
            rasterizeFace(face, polyhedron, projector);
        }
    }

    private void rasterizeFace(Face face, Polyhedron polyhedron, ProjectionTransformer projector) {
        List<Point3D> vertices = face.getVertices();
        if (vertices.size() < 3) return;

        VertexData[] vertexData = new VertexData[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            Point3D v = vertices.get(i);
            Point2D p2d = projector.project(v);

            double depth = (camera != null) ?
                    -v.transform(camera.getViewMatrix()).z() : -v.z();

            Point3D normal = polyhedron.getVertexNormal(v);
            vertexData[i] = new VertexData(p2d.getX(), p2d.getY(), depth, normal, v);
        }

        for (int i = 1; i < vertices.size() - 1; i++) {
            drawTriangle(vertexData[0], vertexData[i], vertexData[i + 1], polyhedron.getColor());
        }
    }

    private void drawTriangle(VertexData v1, VertexData v2, VertexData v3, Color baseColor) {
        int minX = (int) Math.max(0, Math.min(v1.x, Math.min(v2.x, v3.x)));
        int maxX = (int) Math.min(width - 1, Math.ceil(Math.max(v1.x, Math.max(v2.x, v3.x))));
        int minY = (int) Math.max(0, Math.min(v1.y, Math.min(v2.y, v3.y)));
        int maxY = (int) Math.min(height - 1, Math.ceil(Math.max(v1.y, Math.max(v2.y, v3.y))));

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                Point3D bary = barycentric(v1, v2, v3, new Point2D.Double(x, y));
                if (bary.x() >= 0 && bary.y() >= 0 && bary.z() >= 0) {
                    double depth = bary.x() * v1.z + bary.y() * v2.z + bary.z() * v3.z;
                    if (depth < zBuffer[x][y]) {
                        Point3D normal = interpolateNormal(v1, v2, v3, bary);
                        Point3D position = interpolatePosition(v1, v2, v3, bary);
                        Color shadedColor = calculateLighting(baseColor, normal, position);
                        zBuffer[x][y] = depth;
                        frameBuffer[x][y] = shadedColor;
                    }
                }
            }
        }
    }

    private Point3D barycentric(VertexData A, VertexData B, VertexData C, Point2D P) {
        Point3D v0 = new Point3D(B.x - A.x, C.x - A.x, A.x - P.getX());
        Point3D v1 = new Point3D(B.y - A.y, C.y - A.y, A.y - P.getY());
        Point3D u = v0.cross(v1);

        if (Math.abs(u.z()) < 1e-2) {
            return new Point3D(-1, 1, 1);
        }

        double w = 1.0 - (u.x() + u.y()) / u.z();
        double v = u.y() / u.z();
        double t = u.x() / u.z();
        return new Point3D(w, t, v);
    }

    private Point3D interpolateNormal(VertexData v1, VertexData v2, VertexData v3, Point3D bary) {
        Point3D n1 = v1.normal.multiply(bary.x());
        Point3D n2 = v2.normal.multiply(bary.y());
        Point3D n3 = v3.normal.multiply(bary.z());
        return n1.add(n2).add(n3).normalize();
    }

    private Point3D interpolatePosition(VertexData v1, VertexData v2, VertexData v3, Point3D bary) {
        Point3D p1 = v1.position.multiply(bary.x());
        Point3D p2 = v2.position.multiply(bary.y());
        Point3D p3 = v3.position.multiply(bary.z());
        return p1.add(p2).add(p3);
    }

    private Color calculateLighting(Color baseColor, Point3D normal, Point3D position) {
        Point3D viewDir = (camera != null) ?
                camera.getPosition().subtract(position).normalize() :
                new Point3D(0, 0, -1);

        double red = ambientLight.getRed() * baseColor.getRed() / 255.0 / 255.0;
        double green = ambientLight.getGreen() * baseColor.getGreen() / 255.0 / 255.0;
        double blue = ambientLight.getBlue() * baseColor.getBlue() / 255.0 / 255.0;

        for (Light light : lights) {
            Point3D lightDir;
            double attenuation = 1.0;

            if (light.getType() == Light.LightType.DIRECTIONAL) {
                lightDir = light.getDirection().multiply(-1);
            } else if (light.getType() == Light.LightType.POINT) {
                lightDir = light.getPosition().subtract(position).normalize();
                double distance = light.getPosition().distanceTo(position);
                attenuation = 1.0 / (1.0 + 0.1 * distance);
            } else {
                continue; // Ambient уже обработан
            }

            // Диффузная составляющая
            double diff = Math.max(normal.dot(lightDir), 0.0);
            red += diff * light.getIntensity() * light.getColor().getRed() * baseColor.getRed() / 255.0 / 255.0 * attenuation;
            green += diff * light.getIntensity() * light.getColor().getGreen() * baseColor.getGreen() / 255.0 / 255.0 * attenuation;
            blue += diff * light.getIntensity() * light.getColor().getBlue() * baseColor.getBlue() / 255.0 / 255.0 * attenuation;

            // Зеркальная составляющая
            Point3D reflectDir = lightDir.reflect(normal);
            double spec = Math.pow(Math.max(viewDir.dot(reflectDir), 0.0), 32);
            red += spec * light.getIntensity() * light.getColor().getRed() / 255.0 * attenuation;
            green += spec * light.getIntensity() * light.getColor().getGreen() / 255.0 * attenuation;
            blue += spec * light.getIntensity() * light.getColor().getBlue() / 255.0 * attenuation;
        }

        red = Math.min(1.0, Math.max(0.0, red));
        green = Math.min(1.0, Math.max(0.0, green));
        blue = Math.min(1.0, Math.max(0.0, blue));

        return new Color((int)(red * 255), (int)(green * 255), (int)(blue * 255));
    }

    public void display(Graphics2D g2d, Color backgroundColor) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (frameBuffer[x][y] != null) {
                    g2d.setColor(frameBuffer[x][y]);
                    g2d.drawLine(x, y, x, y);
                }
            }
        }
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public void setLights(List<Light> lights) {
        this.lights = lights;
    }

    public void addLight(Light light) {
        this.lights.add(light);
    }

    public void setAmbientLight(Color ambientLight) {
        this.ambientLight = ambientLight;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private static class VertexData {
        double x, y, z;
        Point3D normal;
        Point3D position;

        VertexData(double x, double y, double z, Point3D normal, Point3D position) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.normal = normal;
            this.position = position;
        }
    }
}