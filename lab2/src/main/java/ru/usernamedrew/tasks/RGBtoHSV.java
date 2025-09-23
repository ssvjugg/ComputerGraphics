package ru.usernamedrew.tasks;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class RGBtoHSV {
    public static class RGBtoHSVConverter extends JFrame {
        private BufferedImage originalImage;
        private BufferedImage convertedImage;
        private JLabel imageLabel;
        private JSlider hueSlider, saturationSlider, valueSlider;
        private JButton openButton, saveButton;
        private JFileChooser fileChooser;

        public RGBtoHSVConverter() {
            initializeUI();
        }

        private void initializeUI() {
            setTitle("RGB to HSV Converter");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());

            // Панель для изображения
            imageLabel = new JLabel();
            imageLabel.setHorizontalAlignment(JLabel.CENTER);
            JScrollPane scrollPane = new JScrollPane(imageLabel);
            add(scrollPane, BorderLayout.CENTER);

            // Панель управления
            JPanel controlPanel = new JPanel(new GridLayout(6, 1, 5, 5));
            controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Ползунки для HSV параметров
            hueSlider = createSlider("Оттенок (Hue):", -180, 180);
            saturationSlider = createSlider("Насыщенность (Saturation):", -100, 100);
            valueSlider = createSlider("Яркость (Value):", -100, 100);

            openButton = new JButton("Открыть изображение");
            saveButton = new JButton("Сохранить изображение");
            saveButton.setEnabled(false);

            openButton.addActionListener(e -> openImage());
            saveButton.addActionListener(e -> saveImage());

            controlPanel.add(openButton);
            controlPanel.add(saveButton);
            controlPanel.add(hueSlider);
            controlPanel.add(saturationSlider);
            controlPanel.add(valueSlider);

            add(controlPanel, BorderLayout.EAST);

            setSize(1000, 700);
            setLocationRelativeTo(null);
        }

        private JSlider createSlider(String label, int min, int max) {
            JPanel panel = new JPanel(new BorderLayout());

            JSlider slider = new JSlider(min, max, 0);
            slider.setMajorTickSpacing((max - min) / 4);
            slider.setMinorTickSpacing((max - min) / 20);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            slider.setToolTipText(label);

            slider.addChangeListener(e -> updateImage());

            panel.add(slider, BorderLayout.CENTER);
            return slider;
        }

        private void openImage() {
            if (fileChooser == null) {
                fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                        "Изображения", "jpg", "jpeg", "png", "bmp", "gif"));
            }

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    originalImage = ImageIO.read(fileChooser.getSelectedFile());
                    if (originalImage != null) {
                        displayImage(originalImage);
                        saveButton.setEnabled(true);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка загрузки изображения", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void saveImage() {
            if (convertedImage == null) return;

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    String format = getFileExtension(file.getName());
                    ImageIO.write(convertedImage, format, file);
                    JOptionPane.showMessageDialog(this, "Изображение успешно сохранено");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка сохранения изображения", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private String getFileExtension(String fileName) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                return fileName.substring(dotIndex + 1).toLowerCase();
            }
            return "png";
        }

        private void displayImage(BufferedImage image) {
            ImageIcon icon = new ImageIcon(image);
            imageLabel.setIcon(icon);
        }

        private void updateImage() {
            if (originalImage == null) return;

            convertedImage = applyHSVAdjustment(originalImage);
            displayImage(convertedImage);
        }

        private BufferedImage applyHSVAdjustment(BufferedImage original) {
            int width = original.getWidth();
            int height = original.getHeight();
            BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            // Получаем значения с ползунков
            float hueAdjustment = hueSlider.getValue() / 360.0f;
            float saturationAdjustment = saturationSlider.getValue() / 100.0f;
            float valueAdjustment = valueSlider.getValue() / 100.0f;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = original.getRGB(x, y);

                    // Извлекаем RGB компоненты
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    // Конвертируем RGB в HSV
                    float[] hsv = rgbToHsv(r, g, b);

                    // Применяем корректировки
                    hsv[0] = (hsv[0] + hueAdjustment) % 1.0f;
                    if (hsv[0] < 0) hsv[0] += 1.0f;

                    hsv[1] = Math.max(0, Math.min(1, hsv[1] + saturationAdjustment));
                    hsv[2] = Math.max(0, Math.min(1, hsv[2] + valueAdjustment));

                    // Конвертируем обратно в RGB
                    int newRgb = hsvToRgb(hsv[0], hsv[1], hsv[2]);
                    result.setRGB(x, y, newRgb);
                }
            }

            return result;
        }

        // Преобразование RGB в HSV
        private float[] rgbToHsv(int r, int g, int b) {
            float[] hsv = new float[3];

            float red = r / 255.0f;
            float green = g / 255.0f;
            float blue = b / 255.0f;

            float max = Math.max(red, Math.max(green, blue));
            float min = Math.min(red, Math.min(green, blue));
            float delta = max - min;

            // Расчитываем HUE
            if (delta == 0) {
                hsv[0] = 0;
            } else if (max == red) {
                hsv[0] = (green - blue) / delta;
            } else if (max == green) {
                hsv[0] = 2 + (blue - red) / delta;
            } else {
                hsv[0] = 4 + (red - green) / delta;
            }

            hsv[0] *= 60;
            if (hsv[0] < 0) {
                hsv[0] += 360;
            }
            hsv[0] /= 360; // Нормализуем к [0, 1]

            // Расчет насыщености
            hsv[1] = (max == 0) ? 0 : delta / max;

            // Расчет яркости
            hsv[2] = max;

            return hsv;
        }

        // Преобразование HSV в RGB
        private int hsvToRgb(float h, float s, float v) {
            h *= 360; // Денормализуем HUE

            int r, g, b;

            if (s == 0) {
                r = g = b = (int) (v * 255);
            } else {
                h /= 60;
                int i = (int) Math.floor(h);
                float f = h - i;
                float p = v * (1 - s);
                float q = v * (1 - s * f);
                float t = v * (1 - s * (1 - f));

                switch (i) {
                    case 0:
                        r = (int) (v * 255);
                        g = (int) (t * 255);
                        b = (int) (p * 255);
                        break;
                    case 1:
                        r = (int) (q * 255);
                        g = (int) (v * 255);
                        b = (int) (p * 255);
                        break;
                    case 2:
                        r = (int) (p * 255);
                        g = (int) (v * 255);
                        b = (int) (t * 255);
                        break;
                    case 3:
                        r = (int) (p * 255);
                        g = (int) (q * 255);
                        b = (int) (v * 255);
                        break;
                    case 4:
                        r = (int) (t * 255);
                        g = (int) (p * 255);
                        b = (int) (v * 255);
                        break;
                    default:
                        r = (int) (v * 255);
                        g = (int) (p * 255);
                        b = (int) (q * 255);
                        break;
                }
            }

            return (r << 16) | (g << 8) | b;
        }

        public static void main(String[] args) {
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getLookAndFeel());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                new RGBtoHSVConverter().setVisible(true);
            });
        }
    }
}
