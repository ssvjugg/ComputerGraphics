package ru.usernamedrew;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class FloodFill extends JFrame {
    private static final int PANEL_HEIGHT = 50;
    private static final int COLOR_PANEL_HEIGHT = 30;

    private BufferedImage canvas;
    private BufferedImage loadedImg;
    private JPanel canvasPanel;
    private Color borderColor = Color.BLACK;
    private boolean drawingBorder = false;
    private Point lastPoint;
    private JLabel statusLabel;
    private int offsetX = 0;
    private int offsetY = PANEL_HEIGHT + COLOR_PANEL_HEIGHT;

    public FloodFill() {
        setTitle("Pattern Flood Fill");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);

        initializeCanvas();
        setupUI();
    }

    private void initializeCanvas() {
        canvas = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = canvas.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 1000, 800);
        g2d.dispose();
    }

    private void loadCustomPattern() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Изображения", "jpg", "jpeg", "png", "gif", "bmp"));
        fileChooser.setCurrentDirectory(new File("./src/main/resources"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                loadedImg = ImageIO.read(file);
                updateStatus("Текстура загружена: " + loadedImg.getWidth() + "x" + loadedImg.getHeight());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Ошибка загрузки: " + e.getMessage());
            }
        }
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // Панель управления
        JPanel controlPanel = new JPanel();

        JButton borderBtn = new JButton("Рисовать границу");
        JButton colorFillBtn = new JButton("Залить цветом");
        JButton patternFillBtn = new JButton("Залить текстурой");
        JButton clearBtn = new JButton("Очистить");
        JButton loadPatternBtn = new JButton("Загрузить текстуру");

        JComboBox<String> patternModeCombo = new JComboBox<>(new String[]{
                "Циклическое повторение", "Растянуть", "По центру"
        });

        borderBtn.addActionListener(e -> {
            drawingBorder = true;
            updateStatus("Режим: рисование границы");
        });

        colorFillBtn.addActionListener(e -> {
            drawingBorder = false;
            updateStatus("Режим: заливка цветом");
        });

        patternFillBtn.addActionListener(e -> {
            drawingBorder = false;
            updateStatus("Режим: заливка текстурой");
        });

        clearBtn.addActionListener(e -> {
            initializeCanvas();
            canvasPanel.repaint();
            updateStatus("Холст очищен");
        });

        loadPatternBtn.addActionListener(e -> loadCustomPattern());

        controlPanel.add(borderBtn);
        controlPanel.add(colorFillBtn);
        controlPanel.add(patternFillBtn);
        controlPanel.add(clearBtn);
        controlPanel.add(loadPatternBtn);
        controlPanel.add(new JLabel("Режим:"));
        controlPanel.add(patternModeCombo);

        statusLabel = new JLabel("Готов к работе");


        // Основная панель для рисования
        canvasPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(canvas, 0, 0, null);
            }
        };

        canvasPanel.setPreferredSize(new Dimension(1000, 800));
        canvasPanel.setBackground(Color.WHITE);

        // Обработчики мыши
        canvasPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (drawingBorder) {
                    lastPoint = e.getPoint();
                } else {
                    Color targetColor = new Color(canvas.getRGB(e.getX(), e.getY()));

                    if (e.getButton() == MouseEvent.BUTTON1) { // Левая кнопка - цвет
                        floodFillColor(e.getX(), e.getY(), targetColor, Color.RED);
                    } else if (e.getButton() == MouseEvent.BUTTON3) { // Правая кнопка - текстура
                        if (loadedImg == null) {
                            updateStatus("Загрузите изображение");
                            return;
                        }
                        offsetX = e.getX();
                        offsetY = e.getY();
                        fillRecursiveImg(e.getX(), e.getY(), targetColor);
                    }
                    canvasPanel.repaint();
                }
            }
        });

        canvasPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (drawingBorder && lastPoint != null) {
                    Graphics2D g2d = canvas.createGraphics();
                    g2d.setColor(borderColor);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawLine(lastPoint.x, lastPoint.y, e.getX(), e.getY());
                    g2d.dispose();
                    lastPoint = e.getPoint();
                    canvasPanel.repaint();
                }
            }
        });

        add(controlPanel, BorderLayout.NORTH);
        add(canvasPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void fillRecursiveImg(int x, int y, Color targetColor) {
        if (x < 0 || x >= canvas.getWidth() || y < 0 || y >= canvas.getHeight())
            return;

        Color curColor = new Color(canvas.getRGB(x, y));
        if (!colorsEqual(curColor, targetColor))
            return;

        // Находим левую границу
        int left = x;
        while (left >= 0) {
            Color color = new Color(canvas.getRGB(left, y));
            if (!colorsEqual(color, targetColor)) break;
            left--;
        }
        left++;

        // Находим правую границу
        int right = x;
        while (right < canvas.getWidth()) {
            Color color = new Color(canvas.getRGB(right, y));
            if (!colorsEqual(color, targetColor)) break;
            right++;
        }
        right--;

        // Заполняем найденный отрезок
        for (int i = left; i <= right; ++i) {
            int imgX = i - offsetX;
            int imgY = y - offsetY;

            // Обработка циклического повторения (wrap-around)
            imgX = (imgX % loadedImg.getWidth() + loadedImg.getWidth()) % loadedImg.getWidth();
            imgY = (imgY % loadedImg.getHeight() + loadedImg.getHeight()) % loadedImg.getHeight();

            if (imgX >= 0 && imgX < loadedImg.getWidth() &&
                    imgY >= 0 && imgY < loadedImg.getHeight()) {

                Color patternColor = new Color(loadedImg.getRGB(imgX, imgY));
                canvas.setRGB(i, y, patternColor.getRGB());
            }
        }

        // Рекурсивный вызов для строк выше и ниже
        for (int i = left; i <= right; i++) {
            fillRecursiveImg(i, y - 1, targetColor);
            fillRecursiveImg(i, y + 1, targetColor);
        }
    }

    // 1а) Алгоритм заливки цветом
    private void floodFillColor(int x, int y, Color targetColor, Color fillColor) {
        if (x < 0 || x >= canvas.getWidth() || y < 0 || y >= canvas.getHeight())
            return;

        Color curColor = new Color(canvas.getRGB(x, y));
        if (!colorsEqual(curColor, targetColor))
            return;

        // Находим левую границу
        int left = x;
        while (left >= 0) {
            Color color = new Color(canvas.getRGB(left, y));
            if (!colorsEqual(color, targetColor)) break;
            left--;
        }
        left++;

        // Находим правую границу
        int right = x;
        while (right < canvas.getWidth()) {
            Color color = new Color(canvas.getRGB(right, y));
            if (!colorsEqual(color, targetColor)) break;
            right++;
        }
        right--;

        // Заполняем найденный отрезок
        for (int i = left; i <= right; ++i) {
            canvas.setRGB(i, y, fillColor.getRGB());
        }

        // Рекурсивный вызов для строк выше и ниже
        for (int i = left; i <= right; i++) {
            floodFillColor(i, y - 1, targetColor, fillColor);
            floodFillColor(i, y + 1, targetColor, fillColor);
        }
    }

    // Вспомогательный метод для сравнения цветов
    private boolean colorsEqual(Color color1, Color color2) {
        return color1.getRGB() == color2.getRGB();
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    // Метод для обработки больших изображений (альтернативная реализация)
    public void fillRecursiveImgLarge(int x, int y, Color targetColor) {
        if (loadedImg.getWidth() > 256 || loadedImg.getHeight() > 256) {
            // Для больших изображений используем масштабирование
            fillRecursiveImgScaled(x, y, targetColor);
        } else {
            // Для маленьких - оригинальный алгоритм
            fillRecursiveImg(x, y, targetColor);
        }
    }

    private void fillRecursiveImgScaled(int x, int y, Color targetColor) {
        // Упрощенная версия для больших изображений
        // Можно добавить логику масштабирования здесь
        fillRecursiveImg(x, y, targetColor);
    }
}
