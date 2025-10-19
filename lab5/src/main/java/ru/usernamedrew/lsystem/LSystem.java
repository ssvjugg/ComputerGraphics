package ru.usernamedrew.lsystem;

import ru.usernamedrew.models.LineSegment;
import ru.usernamedrew.models.Point;
import ru.usernamedrew.models.TurtleState;

import java.awt.*;
import java.util.*;
import java.util.List;

public class LSystem {
    private String axiom;
    private Map<Character, String> rules;
    private double angle;
    private double initialAngle;
    private List<LineSegment> segments;  // Теперь храним отрезки с атрибутами
    private Random random;
    private boolean useThicknessAndColor;  // Флаг для улучшенных деревьев

    public LSystem() {
        rules = new HashMap<>();
        segments = new ArrayList<>();
        random = new Random();
        useThicknessAndColor = false;
    }

    public void initialize(String axiom, double angle, double initialAngle) {
        this.axiom = axiom;
        this.angle = Math.toRadians(angle);
        this.initialAngle = Math.toRadians(initialAngle);
        this.rules.clear();
    }

    public void setUseThicknessAndColor(boolean useThicknessAndColor) {
        this.useThicknessAndColor = useThicknessAndColor;
    }

    public void addRule(char key, String value) {
        rules.put(key, value);
    }

    public void generate(int iterations, double step, boolean useRandomness) {
        segments.clear();

        String result = axiom;
        for (int i = 0; i < iterations; i++) {
            result = applyRules(result);
        }

        interpretString(result, step, useRandomness);
    }

    private String applyRules(String input) {
        StringBuilder output = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (rules.containsKey(c)) {
                output.append(rules.get(c));
            } else {
                output.append(c);
            }
        }
        return output.toString();
    }

    private void interpretString(String lString, double step, boolean useRandomness) {
        Stack<TurtleState> stateStack = new Stack<>();

        // Начальное состояние с толщиной и цветом для ствола
        double initialThickness = useThicknessAndColor ? 5.0 : 1.0;
        Color initialColor = useThicknessAndColor ? new Color(139, 69, 19) : Color.BLUE; // Коричневый

        TurtleState currentState = new TurtleState(
                new Point(0, 0), initialAngle, step, initialThickness, initialColor
        );

        for (char c : lString.toCharArray()) {
            switch (c) {
                case 'F':
                case 'G':
                    // Движение вперед с рисованием
                    Point newPoint = calculateNewPoint(currentState);

                    // Добавляем отрезок с текущими атрибутами
                    segments.add(new LineSegment(
                            currentState.position.copy(),
                            newPoint.copy(),
                            currentState.thickness,
                            currentState.color
                    ));

                    currentState.position = newPoint;

                    // Уменьшаем толщину для следующих сегментов (только для деревьев)
                    if (useThicknessAndColor) {
                        currentState.thickness = Math.max(0.5, currentState.thickness * 0.7);

                        // Плавный переход от коричневого к зеленому
                        if (currentState.thickness < 2.0) {
                            float ratio = (float) Math.max(0, Math.min(1, (2.0 - currentState.thickness) / 2.0));
                            currentState.color = interpolateColor(
                                    new Color(139, 69, 19), // Коричневый
                                    new Color(34, 139, 34),  // Зеленый
                                    ratio
                            );
                        }
                    }
                    break;

                case 'f':
                case 'g':
                    // Движение вперед без рисования
                    currentState.position = calculateNewPoint(currentState);
                    break;

                case '+':
                    // Поворот налево с возможной случайностью
                    double leftAngle = getAngleWithRandomness(useRandomness);
                    currentState.angle += leftAngle;
                    break;

                case '-':
                    // Поворот направо с возможной случайностью
                    double rightAngle = getAngleWithRandomness(useRandomness);
                    currentState.angle -= rightAngle;
                    break;

                case '[':
                    // Сохраняем состояние (начало ветки)
                    stateStack.push(currentState.copy());
                    break;

                case ']':
                    // Восстанавливаем состояние (конец ветки)
                    if (!stateStack.isEmpty()) {
                        currentState = stateStack.pop();
                    }
                    break;

                default:
                    break;
            }
        }
    }

    private double getAngleWithRandomness(boolean useRandomness) {
        if (useRandomness) {
            return angle * (0.7 + 0.6 * random.nextDouble()); // Больший разброс для деревьев
        }
        return angle;
    }

    private Point calculateNewPoint(TurtleState state) {
        double newX = state.position.x + Math.cos(state.angle) * state.step;
        double newY = state.position.y + Math.sin(state.angle) * state.step;
        return new Point(newX, newY);
    }

    private Color interpolateColor(Color start, Color end, float ratio) {
        int red = (int) (start.getRed() + (end.getRed() - start.getRed()) * ratio);
        int green = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * ratio);
        int blue = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * ratio);

        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        return new Color(red, green, blue);
    }

    public List<LineSegment> getSegments() {
        return segments;
    }

    public void clearSegments() {
        segments.clear();
    }
}