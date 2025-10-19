package ru.usernamedrew.midpoint_displacement;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Midpoint extends JPanel {

    private static class Segment {
        Point2D.Double p1;
        Point2D.Double p2;

        public Segment(Point2D.Double p1, Point2D.Double p2) {
            this.p1 = p1;
            this.p2 = p2;
        }
    }

    private final List<Point2D.Double> allPoints = new ArrayList<>();
    private final LinkedList<Segment> activeSegments = new LinkedList<>();

    private Point2D.Double firstPoint;
    private Point2D.Double secondPoint;
    private double r = 0.3;
    private final JPanel drawingPanel;
    private Timer animationTimer;
    private int DELAY = 400;

    private final JTextField rField;
    private final JTextField delayField;

    public Midpoint() {
        setLayout(new BorderLayout());

        rField = new JTextField(String.valueOf(r), 5);
        delayField = new JTextField(String.valueOf(DELAY), 5);

        drawingPanel = getDrawingPanel();
        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
        });

        JButton clearButton = new JButton("Очистить");
        clearButton.addActionListener(e -> clearCanvas());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(new JLabel("Коэфф. r:"));
        buttonPanel.add(rField);
        buttonPanel.add(new JLabel("Задержка (мс):"));
        buttonPanel.add(delayField);
        buttonPanel.add(clearButton);

        add(drawingPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        animationTimer = createTimer();
    }

    private Timer createTimer() {
        return new Timer(DELAY, e -> {
            stepMidpointDisplacement();
            drawingPanel.repaint();
        });
    }

    private void stepMidpointDisplacement() {
        if (activeSegments.isEmpty()) {
            animationTimer.stop();
            return;
        }

        LinkedList<Segment> newSegments = new LinkedList<>();

        while (!activeSegments.isEmpty()) {
            Segment currentSegment = activeSegments.poll();

            Point2D.Double p1 = currentSegment.p1;
            Point2D.Double p2 = currentSegment.p2;

            if (p2.getX() - p1.getX() < 2) {
                continue;
            }

            double dx = p2.getX() - p1.getX();
            double dy = p2.getY() - p1.getY();
            double len = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));

            double displacement = ThreadLocalRandom.current().nextDouble(-r * len, r * len);
            double midX = (p1.getX() + p2.getX()) / 2;
            double midY = (p1.getY() + p2.getY()) / 2 + displacement;

            Point2D.Double center = new Point2D.Double(midX, midY);

            allPoints.add(center);
            allPoints.sort(Comparator.comparingDouble(Point2D.Double::getX));

            newSegments.add(new Segment(p1, center));
            newSegments.add(new Segment(center, p2));
        }

        activeSegments.addAll(newSegments);
    }

    private void readAndApplyParameters() {
        try {
            double newR = Double.parseDouble(rField.getText());
            if (newR > 0) {
                this.r = newR;
            }
            int newDelay = Integer.parseInt(delayField.getText());

            if (newDelay >= 10) {
                this.DELAY = newDelay;
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Используются старые значения.",
                    "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleMouseClick(MouseEvent e) {
        if (animationTimer.isRunning()) {
            return;
        }

        if (firstPoint == null) {
            firstPoint = new Point2D.Double(e.getX(), e.getY());
            allPoints.add(firstPoint);
            drawingPanel.repaint();
        } else if (secondPoint == null) {
            secondPoint = new Point2D.Double(e.getX(), e.getY());
            allPoints.add(secondPoint);

            readAndApplyParameters();

            if (animationTimer != null) {
                animationTimer.stop();
            }
            animationTimer = createTimer();

            allPoints.sort(Comparator.comparingDouble(Point2D.Double::getX));
            firstPoint = allPoints.get(0);
            secondPoint = allPoints.get(allPoints.size() - 1);

            activeSegments.add(new Segment(firstPoint, secondPoint));

            animationTimer.start();
        }
    }

    private JPanel getDrawingPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(Color.BLUE);
                for (int i = 0; i < allPoints.size() - 1; i++) {
                    Point2D p1 = allPoints.get(i);
                    Point2D p2 = allPoints.get(i + 1);
                    g2d.drawLine((int) p1.getX(), (int) p1.getY(),
                            (int) p2.getX(), (int) p2.getY());
                }
            }
        };

        panel.setBackground(Color.WHITE);
        return panel;
    }

    private void clearCanvas() {
        animationTimer.stop();
        allPoints.clear();
        activeSegments.clear();
        firstPoint = null;
        secondPoint = null;
        drawingPanel.repaint();
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Midpoint Displacement");
            Midpoint panel = new Midpoint();

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.add(panel);
            frame.setVisible(true);
        });
    }
}