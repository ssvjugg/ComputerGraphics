package ru.usernamedrew;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class BoundaryTracer extends JPanel {
    // Хранит изображение для работы
    private BufferedImage image;
    // Список для хранения всех точек границы в порядке обхода
    private final ArrayList<Point> points = new ArrayList<>();
    // Размер окна и изображения
    private final int size = 600;

    public BoundaryTracer() {
        createSimpleImage();
        findBoundary();
        setPreferredSize(new Dimension(size, size));
    }

    private void createSimpleImage() {
        image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);

        g.setColor(Color.BLUE);
        g.fillRect(150, 150, 300, 300);

        g.setColor(Color.RED);
        ((Graphics2D)g).setStroke(new BasicStroke(3));
        g.drawRect(150, 150, 300, 300);

        g.dispose(); // Освобождаем ресурсы
    }

    private void findBoundary() {
        // Находим первую точку границы (любую красную точку)
        Point start = findStart();
        if (start == null) return; // Если граница не найдена - выходим

        // Текущая точка - начинаем со стартовой
        Point current = start;
        // Предыдущая точка - искусственно создаем точку слева от стартовой
        // Это нужно для определения начального направления движения
        Point prev = new Point(start.x - 1, start.y);

        // Цикл обхода границы до тех пор, пока не вернемся в начальную точку
        do {
            // Добавляем текущую точку в список
            points.add(new Point(current.x, current.y));

            // Находим следующую точку границы
            Point next = findNext(current, prev);

            // Перемещаемся: предыдущая = текущая, текущая = следующая
            prev = current;
            current = next;

            // Условие выхода: вернулись в начало ИЛИ прошли слишком много точек (защита от зацикливания)
        } while (!current.equals(start) && points.size() < 1000);

        // Рисуем найденную границу поверх изображения
        drawBoundary();
    }

    // Поиск начальной точки границы (первой красной точки на изображении)
    private Point findStart() {
        // Проходим по всем пикселям изображения построчно
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                if (isRed(image.getRGB(x, y)))
                    return new Point(x, y);
        return null;
    }

    // Проверка, является ли цвет красным
    private boolean isRed(int rgb) {
        Color c = new Color(rgb);
        return c.getRed() > 200 && c.getGreen() < 50 && c.getBlue() < 50;
    }

    // Поиск следующей точки границы от текущей позиции
    private Point findNext(Point curr, Point prev) {
        // Все возможные направления движения (8-связность):
        // 0: вправо, 1: вправо-вниз, 2: вниз, 3: влево-вниз,
        // 4: влево, 5: влево-вверх, 6: вверх, 7: вправо-вверх
        int[][] dirs = {{1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}, {0,-1}, {1,-1}};

        // Определяем направление от предыдущей точки к текущей
        int startDir = getDir(prev, curr);

        // Проверяем все 8 направлений по часовой стрелке, начиная с startDir
        for (int i = 0; i < 8; i++) {
            // Вычисляем текущее направление (циклически по модулю 8)
            int dir = (startDir + i) % 8;

            // Вычисляем координаты соседней точки в этом направлении
            int nx = curr.x + dirs[dir][0]; // новая X координата
            int ny = curr.y + dirs[dir][1]; // новая Y координата

            // Проверяем: точка в пределах изображения И является красной (границей)
            if (nx >= 0 && nx < size && ny >= 0 && ny < size && isRed(image.getRGB(nx, ny)))
                return new Point(nx, ny); // Нашли следующую точку границы
        }

        // Если не нашли следующую точку (не должно происходить в корректном изображении)
        return curr;
    }

    // Определение направления между двумя точками
    private int getDir(Point from, Point to) {
        // Вычисляем разницу координат
        int dx = to.x - from.x; // разница по X
        int dy = to.y - from.y; // разница по Y

        // Сопоставляем разницу координат с направлениями:
        if (dx == 1 && dy == 0) return 0;   // движение вправо
        if (dx == 1 && dy == 1) return 1;   // движение вправо-вниз
        if (dx == 0 && dy == 1) return 2;   // движение вниз
        if (dx == -1 && dy == 1) return 3;  // движение влево-вниз
        if (dx == -1 && dy == 0) return 4;  // движение влево
        if (dx == -1 && dy == -1) return 5; // движение влево-вверх
        if (dx == 0 && dy == -1) return 6;  // движение вверх
        return 7; // движение вправо-вверх (dx == 1 && dy == -1)
    }

    // Рисуем найденную границу поверх исходного изображения
    private void drawBoundary() {
        Graphics g = image.getGraphics();
        g.setColor(Color.GREEN); // Зеленый цвет для отображения найденной границы
        ((Graphics2D)g).setStroke(new BasicStroke(2));

        // Рисуем линии между последовательными точками границы
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i); // Текущая точка
            // Если есть следующая точка - рисуем линию к ней
            if (i < points.size() - 1) {
                Point next = points.get(i + 1);
                g.drawLine(p.x, p.y, next.x, next.y);
            }
        }

        // Замыкаем контур: рисуем линию от последней точки к первой
        if (!points.isEmpty()) {
            Point first = points.get(0);     // Первая точка границы
            Point last = points.get(points.size() - 1); // Последняя точка границы
            g.drawLine(last.x, last.y, first.x, first.y);
        }

        g.dispose(); // Освобождаем ресурсы
    }

    // Метод отрисовки компонента - вызывается автоматически когда нужно перерисовать окно
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, null);

        // Выводим информацию о количестве найденных точек
        g.setColor(Color.BLACK);
        g.drawString("Точек границы: " + points.size(), 10, 20);
    }
}
