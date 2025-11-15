// новый класс для создания фигур вращения
package ru.usernamedrew.model;

import java.util.ArrayList;
import java.util.List;

// строит 3D модель вращением образующей вокруг оси Y
public class RevolutionSurfaceFactory {

    public enum Axis {
        X, Y, Z
    }

    // Метод создает полную 3D модель фигуры вращения, готовую для:
    //
    //Отображения в GraphicsPanel
    //Применения аффинных преобразований
    //Сохранения в файл формата OBJ
    //Дальнейшей обработки в приложении
    public static Polyhedron createRevolutionSurface(List<Point3D> generatrix, Axis axis, int divisions) {
        // Алгоритм работы:
        // 1. Валидация входных данных (образующая >= 2 точек, разбиения >= 3)
        // 2. Вычисление шага угла: 360 / divisions
        // 3. Создание сетки вершин:
        //    - Для каждого угла от 0 до 360 с шагом angleStep
        //    - Для каждой точки образующей применяется вращение вокруг выбранной оси
        //    - Все точки добавляются в Polyhedron
        // 4. Создание граней:
        //    - Для каждого "пояса" между соседними углами вращения
        //    - Для каждой пары соседних точек образующей
        //    - Создается четырехугольная грань между текущим и следующим поясом
        // 5. Возврат готовой модели
        if (generatrix == null || generatrix.size() < 2) {
            throw new IllegalArgumentException("Generatrix must contain at least 2 points");
        }
        if (divisions < 3) {
            throw new IllegalArgumentException("Divisions must be at least 3");
        }

        Polyhedron surface = new Polyhedron();
        List<List<Point3D>> grid = new ArrayList<>();

        double angleStep = 2 * Math.PI / divisions;

        // Создаем вершины
        for (int i = 0; i < divisions; i++) {
            double angle = i * angleStep;
            List<Point3D> circlePoints = new ArrayList<>();

            for (Point3D point : generatrix) {
                Point3D rotatedPoint = rotatePointAroundAxis(point, axis, angle);
                circlePoints.add(rotatedPoint);
                surface.addVertex(rotatedPoint);
            }
            grid.add(circlePoints);
        }

        // Добавляем первую окружность в конец для замыкания поверхности
        grid.add(grid.get(0));

        // Создаем грани
        for (int i = 0; i < divisions; i++) {
            List<Point3D> currentCircle = grid.get(i);
            List<Point3D> nextCircle = grid.get(i + 1);

            for (int j = 0; j < generatrix.size() - 1; j++) {
                Point3D v1 = currentCircle.get(j);
                Point3D v2 = currentCircle.get(j + 1);
                Point3D v3 = nextCircle.get(j + 1);
                Point3D v4 = nextCircle.get(j);

                Face face = new Face(List.of(v1, v2, v3, v4));
                surface.addFace(face);
            }
        }

        return surface;
    }

    private static Point3D rotatePointAroundAxis(Point3D point, Axis axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        return switch (axis) {
            case X -> rotateAroundX(point, cos, sin);
            case Y -> rotateAroundY(point, cos, sin);
            case Z -> rotateAroundZ(point, cos, sin);
        };
    }

    private static Point3D rotateAroundX(Point3D point, double cos, double sin) {
        return new Point3D(
                point.x(),
                point.y() * cos - point.z() * sin,
                point.y() * sin + point.z() * cos
        );
    }

    private static Point3D rotateAroundY(Point3D point, double cos, double sin) {
        return new Point3D(
                point.x() * cos + point.z() * sin,
                point.y(),
                -point.x() * sin + point.z() * cos
        );
    }

    private static Point3D rotateAroundZ(Point3D point, double cos, double sin) {
        return new Point3D(
                point.x() * cos - point.y() * sin,
                point.x() * sin + point.y() * cos,
                point.z()
        );
    }

    // Предопределенные образующие для быстрого создания фигур
    public static List<Point3D> createCylinderGeneratrix(double radius, double height, int points) {
        List<Point3D> generatrix = new ArrayList<>();
        for (int i = 0; i <= points; i++) {
            double y = (double) i / points * height;
            generatrix.add(new Point3D(radius, y, 0));
        }
        return generatrix;
    }

    public static List<Point3D> createConeGeneratrix(double radius, double height, int points) {
        List<Point3D> generatrix = new ArrayList<>();
        for (int i = 0; i <= points; i++) {
            double ratio = (double) i / points;
            double currentRadius = radius * (1 - ratio);
            double y = height * ratio;
            generatrix.add(new Point3D(currentRadius, y, 0));
        }
        return generatrix;
    }

    public static List<Point3D> createSphereGeneratrix(double radius, int points) {
        List<Point3D> generatrix = new ArrayList<>();
        for (int i = 0; i <= points; i++) {
            double theta = (double) i / points * Math.PI;
            double x = radius * Math.sin(theta);
            double y = radius * Math.cos(theta);
            generatrix.add(new Point3D(x, y, 0));
        }
        return generatrix;
    }
}