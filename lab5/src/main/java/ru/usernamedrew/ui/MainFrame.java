package ru.usernamedrew.ui;

import ru.usernamedrew.lsystem.LSystem;
import ru.usernamedrew.lsystem.LSystemParser;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainFrame extends JFrame {
    private LSystemFractalPanel fractalPanel;
    private LSystem currentLSystem;

    public MainFrame() {
        setTitle("L-системы: Фрактальные узоры с улучшенными деревьями");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        initializeComponents();
        createTestFiles();

        // Автозагрузка снежинки Коха при запуске
        loadFractal("koch.txt", 4, 1.0, false, false);
    }

    private void initializeComponents() {
        fractalPanel = new LSystemFractalPanel();

        // Панель управления
        JPanel controlPanel = new JPanel(new FlowLayout());

        // Выбор фрактала
        JComboBox<String> fractalCombo = new JComboBox<>(new String[]{
                "Снежинка Коха", "Кривая дракона", "Дерево 1", "Дерево 2",
                "Дерево 3 (улучшенное)", "Дерево 4 (улучшенное)", "Куст (улучшенный)"
        });

        // Параметры
        JSpinner iterSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 8, 1));
        JSpinner scaleSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.01, 5.0, 0.1));
        JCheckBox randomCheckbox = new JCheckBox("Случайность");
        JCheckBox enhancedCheckbox = new JCheckBox("Улучшенное дерево");

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
            boolean useEnhanced = enhancedCheckbox.isSelected() && isTreeFractal(selectedFractal);

            loadFractal(filename, iterations, scale, useRandomness, useEnhanced);
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

        JButton bezierButton = new JButton("Сплайны Безье");
        bezierButton.setBackground(new Color(100, 150, 200));
        bezierButton.setForeground(Color.WHITE);
        bezierButton.addActionListener(e -> {
            BezierFrame bezierFrame = new BezierFrame();
            bezierFrame.setVisible(true);
        });

        controlPanel.add(bezierButton);

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
        controlPanel.add(enhancedCheckbox);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(generateButton);
        controlPanel.add(Box.createHorizontalStrut(5));
        controlPanel.add(resetScaleButton);

        // Настройка основного layout
        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.NORTH);
        add(fractalPanel, BorderLayout.CENTER);

        // Информационная панель
        JLabel infoLabel = new JLabel("Улучшенное дерево: толщина ствола → тонкие ветки | коричневый → зеленый | случайные углы");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setForeground(Color.GRAY);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void loadFractal(String filename, int iterations, double scale, boolean useRandomness, boolean useEnhanced) {
        try {
            currentLSystem = LSystemParser.parseFromFile(filename);
            currentLSystem.setUseThicknessAndColor(useEnhanced);
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
            case "Кривая дракона": return "dragon.txt";
            case "Дерево 1": return "tree1.txt";
            case "Дерево 2": return "tree2.txt";
            case "Дерево 3 (улучшенное)": return "tree3_enhanced.txt";
            case "Дерево 4 (улучшенное)": return "tree4_enhanced.txt";
            case "Куст (улучшенный)": return "bush_enhanced.txt";
            default: return "koch.txt";
        }
    }

    private boolean isTreeFractal(String fractalName) {
        return fractalName.contains("Дерево") || fractalName.contains("Куст");
    }

    private void createTestFiles() {
        // Существующие фракталы
        String kochSnowflake = "F 60 0\nF->F-F++F-F";
        String dragonCurve = "X 90 0\nX->X+YF+\nY->-FX-Y";
        String tree1 = "X 20 90\nF->FF\nX->F[+X]F[-X]+X";
        String tree2 = "X 22.5 90\nF->FF\nX->F[+X][-X]FX";

        // Новые улучшенные деревья
        String tree3Enhanced = "X 25 90\nF->FF\nX->F[+X][-X]F[+X][-X]FX";
        String tree4Enhanced = "X 30 90\nF->FF\nX->F[+FX][-FX][+FX][-FX]X";
        String bushEnhanced = "X 35 90\nF->FF\nX->F[+X][-X][+X][-X]F[+X][-X]X";

        try {
            // Старые файлы
            Files.write(Paths.get("koch.txt"), kochSnowflake.getBytes());
            Files.write(Paths.get("dragon.txt"), dragonCurve.getBytes());
            Files.write(Paths.get("tree1.txt"), tree1.getBytes());
            Files.write(Paths.get("tree2.txt"), tree2.getBytes());

            // Новые улучшенные файлы
            Files.write(Paths.get("tree3_enhanced.txt"), tree3Enhanced.getBytes());
            Files.write(Paths.get("tree4_enhanced.txt"), tree4Enhanced.getBytes());
            Files.write(Paths.get("bush_enhanced.txt"), bushEnhanced.getBytes());

            System.out.println("Тестовые файлы созданы!");
        } catch (IOException e) {
            System.err.println("Ошибка создания тестовых файлов: " + e.getMessage());
        }
    }
}