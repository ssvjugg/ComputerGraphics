package ru.usernamedrew.models;

public class TurtleState {
    public Point position;  // Где находится
    public double angle;    // Куда смотрит (в градусах)
    public double step;     // Какой шаг делает

    public TurtleState(Point position, double angle, double step) {
        this.position = position.copy();
        this.angle = angle;
        this.step = step;
    }

    public TurtleState copy() {
        return new TurtleState(position, angle, step);
    }
}