package ru.usernamedrew.models;

import java.awt.Color;

public class TurtleState {
    public Point position;
    public double angle;
    public double step;
    public double thickness;  // Толщина линии
    public Color color;       // Цвет линии

    public TurtleState(Point position, double angle, double step) {
        this.position = position.copy();
        this.angle = angle;
        this.step = step;
        this.thickness = 1.0;
        this.color = Color.BLACK;
    }

    public TurtleState(Point position, double angle, double step, double thickness, Color color) {
        this.position = position.copy();
        this.angle = angle;
        this.step = step;
        this.thickness = thickness;
        this.color = color;
    }

    public TurtleState copy() {
        return new TurtleState(position, angle, step, thickness, color);
    }
}