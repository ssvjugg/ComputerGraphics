package ru.usernamedrew.ui;

import ru.usernamedrew.lsystem.LSystem;
import ru.usernamedrew.lsystem.LSystemParser;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class MainFrame extends JFrame {
    private LSystemFractalPanel fractalPanel;
    private LSystem currentLSystem;

    public MainFrame() {
        setTitle("L-системы: Фрактальные узоры");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);

        initializeComponents();
        createTestFiles();

        // Автозагрузка снежинки Коха при запуске
        loadFractal("koch.txt", 4, 1.0, false);
    }

    private void initializeComponents() {
        fractalPanel = new LSystemFractalPanel();

        // Панель управления
        JPanel controlPanel = new JPanel(new FlowLayout());

        // Выбор фрактала
        JComboBox<String> fractalCombo = new JComboBox<>(new String[]{
                "Снежинка Коха", "Кривая дракона", "Дерево 1", "Дерево 2", "Квадратный остров Коха"
        });

        // Параметры
        JSpinner iterSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 8, 1));
        JSpinner scaleSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.01, 5.0, 0.1)); // Увеличил диапазон
        JCheckBox randomCheckbox = new JCheckBox("Случайность");

        // Кнопка генерации
        JButton generateButton = new JButton("Сгенерировать");
        generateButton.setBackground(new Color(70, 130, 180));
        generateButton.setForeground(Color.WHITE);

        // Обработчик кнопки
        generateButton.addActionListener(e -> {
            String selectedFractal = (String) fractalCombo.getSelectedItem();
            String filename = getFilenameForFractal(selectedFractal);
            int iterations = (Integer) iterSpinner.getValue();
            double scale = (Double) scaleSpinner.getValue();
            boolean useRandomness = randomCheckbox.isSelected();

            loadFractal(filename, iterations, scale, useRandomness);
        });

        // Кнопка сброса масштаба
        JButton resetScaleButton = new JButton("Сброс масштаба");
        resetScaleButton.addActionListener(e -> {
            scaleSpinner.setValue(1.0);
            if (currentLSystem != null) {
                fractalPanel.setScale(1.0);
                repaint();
            }
        });

        // Добавляем компоненты на панель
        controlPanel.add(new JLabel("Фрактал:"));
        controlPanel.add(fractalCombo);
        controlPanel.add(Box.createHorizontalStrut(10));

        controlPanel.add(new JLabel("Итерации:"));
        controlPanel.add(iterSpinner);
        controlPanel.add(Box.createHorizontalStrut(10));

        controlPanel.add(new JLabel("Масштаб:"));
        controlPanel.add(scaleSpinner);
        controlPanel.add(Box.createHorizontalStrut(10));

        controlPanel.add(randomCheckbox);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(generateButton);
        controlPanel.add(Box.createHorizontalStrut(5));
        controlPanel.add(resetScaleButton);

        // Настройка основного layout
        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.NORTH);
        add(fractalPanel, BorderLayout.CENTER);

        // Информационная панель
        JLabel infoLabel = new JLabel("Итерации: детализация | Масштаб: относительный размер | Сброс: вернуть масштаб 1.0");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setForeground(Color.GRAY);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void loadFractal(String filename, int iterations, double scale, boolean useRandomness) {
        try {
            currentLSystem = LSystemParser.parseFromFile(filename);
            currentLSystem.generate(iterations, 10.0, useRandomness);
            fractalPanel.setLSystem(currentLSystem);
            fractalPanel.setScale(scale);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка загрузки фрактала: " + ex.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getFilenameForFractal(String fractalName) {
        switch (fractalName) {
            case "Снежинка Коха": return "koch.txt";
            case "Кривая дракона": return "dragon.txt";
            case "Дерево 1": return "tree1.txt";
            case "Дерево 2": return "tree2.txt";
            case "Квадратный остров Коха": return "koch_island.txt";
            default: return "koch.txt";
        }
    }

    private void createTestFiles() {
        // Снежинка Коха
        String kochSnowflake = "F 60 0\n" +
                "F->F-F++F-F";

        // Кривая дракона
        String dragonCurve = "X 90 0\n" +
                "X->X+YF+\n" +
                "Y->-FX-Y";

        // Дерево 1
        String tree1 = "X 20 90\n" +
                "F->FF\n" +
                "X->F[+X]F[-X]+X";

        // Дерево 2
        String tree2 = "X 22.5 90\n" +
                "F->FF\n" +
                "X->F[+X][-X]FX";

        // Квадратный остров Коха
        String kochIsland = "F 90 0\n" +
                "F->F+F-F-FF+F+F-F";

        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("koch.txt"), kochSnowflake.getBytes());
            java.nio.file.Files.write(java.nio.file.Paths.get("dragon.txt"), dragonCurve.getBytes());
            java.nio.file.Files.write(java.nio.file.Paths.get("tree1.txt"), tree1.getBytes());
            java.nio.file.Files.write(java.nio.file.Paths.get("tree2.txt"), tree2.getBytes());
            java.nio.file.Files.write(java.nio.file.Paths.get("koch_island.txt"), kochIsland.getBytes());
            System.out.println("Тестовые файлы созданы!");
        } catch (IOException e) {
            System.err.println("Ошибка создания тестовых файлов: " + e.getMessage());
        }
    }
}