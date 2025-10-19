package ru.usernamedrew.models;

import java.awt.Color;

public class LineSegment {
    public Point start;
    public Point end;
    public double thickness;
    public Color color;

    public LineSegment(Point start, Point end, double thickness, Color color) {
        this.start = start.copy();
        this.end = end.copy();
        this.thickness = thickness;
        this.color = color;
    }
}