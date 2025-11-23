package ru.usernamedrew.model;

import java.awt.Color;

public class Light {
    public enum LightType {
        DIRECTIONAL,
        POINT,
        AMBIENT
    }

    private LightType type;
    private Point3D position;
    private Point3D direction;
    private Color color;
    private double intensity;

    // Конструктор для направленного света
    public Light(Point3D direction, Color color, double intensity) {
        this.type = LightType.DIRECTIONAL;
        this.direction = direction.normalize();
        this.color = color;
        this.intensity = intensity;
    }

    // Конструктор для точечного света
    public Light(Color color, Point3D position, double intensity) {
        this.type = LightType.POINT;
        this.position = position;
        this.color = color;
        this.intensity = intensity;
    }

    // Конструктор для окружающего света
    public Light(Color color, double intensity) {
        this.type = LightType.AMBIENT;
        this.color = color;
        this.intensity = intensity;
    }

    // Геттеры
    public LightType getType() { return type; }
    public Point3D getPosition() { return position; }
    public Point3D getDirection() { return direction; }
    public Color getColor() { return color; }
    public double getIntensity() { return intensity; }

    // Сеттеры
    public void setPosition(Point3D position) { this.position = position; }
    public void setDirection(Point3D direction) { this.direction = direction.normalize(); }
    public void setColor(Color color) { this.color = color; }
    public void setIntensity(double intensity) { this.intensity = intensity; }
}