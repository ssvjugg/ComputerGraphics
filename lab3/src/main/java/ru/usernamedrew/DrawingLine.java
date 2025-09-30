package ru.usernamedrew;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;


public class DrawingLine {

    static class PointWithIntensity {
        int x, y;
        float intensity;

        public PointWithIntensity(int x, int y, float intensity) {
            this.x = x;
            this.y = y;
            this.intensity = intensity;
        }
    }

    public static int[] swap(int a, int b) {
        int temp = a;
        a = b;
        b = temp;
        return new int[]{a, b};
    }

    public static ArrayList<Point> plotLineLow(int x0, int x1, int y0, int y1) {
        ArrayList<Point> linePoints = new ArrayList<>();
        int dx = x1 - x0;
        int dy = y1 - y0;
        int yi = 1;
        if (dy < 0) {
            yi = -1;
            dy = -dy;
        }
        int D = 2 * dy - dx;
        int y = y0;
        for (int x = x0; x <= x1; x++) {
            linePoints.add(new Point(x, y));
            if (D > 0) {
                y += yi;
                D += 2 * (dy - dx);
            } else {
                D += 2 * dy;
            }
        }
        return linePoints;
    }

    public static ArrayList<Point> plotLineHigh(int x0, int x1, int y0, int y1) {
        ArrayList<Point> linePoints = new ArrayList<>();
        int dx = x1 - x0;
        int dy = y1 - y0;
        int xi = 1;
        if (dx < 0) {
            xi = -1;
            dx = -dx;
        }
        int D = 2 * dx - dy;
        int x = x0;
        for (int y = y0; y <= y1; y++) {
            linePoints.add(new Point(x, y));
            if (D > 0) {
                x += xi;
                D += 2 * (dx - dy);
            } else {
                D += 2 * dx;
            }
        }
        return linePoints;
    }

    public static ArrayList<Point> BresenhamLineAlgorithm(int x0, int x1, int y0, int y1) {
//        ArrayList<Point> linePoints = new ArrayList<>();
//
//        int dx = Math.abs(x1 - x0);
//        int dy = Math.abs(y1 - y0);
//        int error = 0;
//        int deltaError = dy + 1;
//        int y = y0;
//        int diry = (y1 > y0) ? 1 : -1;
//
//        for (int x = x0; x <= x1; x++) {
//            linePoints.add(new Point(x, y));
//            error += deltaError;
//            if (error >= dx + 1) {
//                y += diry;
//                error -= dx + 1;
//            }
//        }
//        return linePoints;
        if (Math.abs(y1 - y0) < Math.abs(x1 - x0)) {
            if (x0 > x1) {
                return plotLineLow(x1, x0, y1, y0);
            } else {
                return plotLineLow(x0, x1, y0, y1);
            }
        } else {
            if (y0 > y1) {
                return plotLineHigh(x1, x0, y1, y0);
            } else {
                return plotLineHigh(x0, x1, y0, y1);
            }
        }
    }


    public static ArrayList<PointWithIntensity> WuLineAlgorithm(int x0, int x1, int y0, int y1) {
        ArrayList<PointWithIntensity> linePoints = new ArrayList<>();

        boolean steep = Math.abs(y1 - y0) > Math.abs(x1 - x0);

        if (steep) {
            int[] temp = swap(x0, y0);
            x0 = temp[0];
            y0 = temp[1];

            int[] temp1 = swap(x1, y1);
            x1 = temp1[0];
            y1 = temp1[1];
        }

        if (x0 > x1) {
            int[] temp = swap(x0, x1);
            x0 = temp[0];
            x1 = temp[1];

            int[] temp1 = swap(y0, y1);
            y0 = temp1[0];
            y1 = temp1[1];
        }

        linePoints.add(new PointWithIntensity(x1, y1, 1));
        float dx = x1 - x0;
        float dy = y1 - y0;
        float gradient = dx == 0 ? 1.0f : dy / dx;
        float y = y0 + gradient;
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            if (steep) {
                linePoints.add(new PointWithIntensity((int) y, x, 1 - (y - (int) y)));
                linePoints.add(new PointWithIntensity((int) y + 1, x, y - (int) y));
            } else {
                linePoints.add(new PointWithIntensity(x, (int) y, 1 - (y - (int) y)));
                linePoints.add(new PointWithIntensity(x, (int) y + 1, y - (int) y));
            }
            y += gradient;
        }
        return linePoints;
    }

    public static class LineDrawerPanel extends JPanel {
        private ArrayList<Point> bresenhamLinePoints = new ArrayList<>();
        private ArrayList<PointWithIntensity> wuLinePoints = new ArrayList<>();

        private boolean drawBresenhamLine = false;
        private static final int SCALE = 5;

        public LineDrawerPanel(int x0, int x1, int y0, int y1) {
            bresenhamLinePoints = DrawingLine.BresenhamLineAlgorithm(x0, x1, y0, y1);
            wuLinePoints = DrawingLine.WuLineAlgorithm(x0, x1, y0, y1);
        }

        public void setAlgo(boolean drawBresenhamLine) {
            this.drawBresenhamLine = drawBresenhamLine;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (drawBresenhamLine) {
                drawBresenhamLine(g);
            } else {
                drawWuLine(g);
            }

        }

        private void drawBresenhamLine(Graphics g) {
            g.setColor(Color.BLACK);
            for (Point point : bresenhamLinePoints) {
                g.fillRect(point.x * SCALE, point.y * SCALE, SCALE, SCALE);
            }
        }

        private void drawWuLine(Graphics g) {
            for (PointWithIntensity wp : wuLinePoints) {
                int alpha = (int) (wp.intensity * 255);
                alpha = Math.max(0, Math.min(255, alpha));

                Color shadedColor = new Color(0, 0, 0, alpha);
                g.setColor(shadedColor);

                g.fillRect(wp.x * SCALE, wp.y * SCALE, SCALE, SCALE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Drawing Line");
            LineDrawerPanel panel = new LineDrawerPanel(50, 20, 25, 10);
            panel.setPreferredSize(new Dimension(400, 300));

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            panel.setAlgo(true);
        });
    }
}