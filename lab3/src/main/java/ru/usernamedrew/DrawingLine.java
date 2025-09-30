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

    public static ArrayList<Point> BresenhamLineAlgorithm(int x0, int x1, int y0, int y1) {
        ArrayList<Point> linePoints = new ArrayList<>();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int error = 0;
        int deltaError = dy + 1;
        int y = y0;
        int diry = (y1 > y0) ? 1 : -1;

        for (int x = x0; x <= x1; x++) {
            linePoints.add(new Point(x, y));
            error += deltaError;
            if (error >= dx + 1) {
                y += diry;
                error -= dx + 1;
            }
        }
        return linePoints;
    }


    public static ArrayList<PointWithIntensity> WuLineAlgorithm(int x0, int x1, int y0, int y1) {
        ArrayList<PointWithIntensity> linePoints = new ArrayList<>();
        linePoints.add(new PointWithIntensity(x1, y1, 1));
        float dx = x1 - x0;
        float dy = y1 - y0;
        float gradient = dy / dx;
        float y = y0 + gradient;
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            linePoints.add(new PointWithIntensity(x, (int) y, 1 - (y - (int) y)));
            linePoints.add(new PointWithIntensity(x, (int) y + 1, y - (int) y));
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
            LineDrawerPanel panel = new LineDrawerPanel(10, 50, 10, 40);
            panel.setPreferredSize(new Dimension(400, 300));

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            panel.setAlgo(false);
        });
    }
}

