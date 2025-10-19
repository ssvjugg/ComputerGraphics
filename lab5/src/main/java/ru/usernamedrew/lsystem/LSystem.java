package ru.usernamedrew.lsystem;

import ru.usernamedrew.models.Point;
import ru.usernamedrew.models.TurtleState;

import java.util.*;
import java.util.List;

//Как работает:
//Получает правила (например: "F" → "F-F++F-F")
//Применяет правила несколько раз (итерации)
//Превращает буквы в движения

public class LSystem {
    private String axiom;
    private final Map<Character, String> rules;
    private double angle;
    private double initialAngle;
    private final List<Point> points;
    private final Random random;

    public LSystem() {
        rules = new HashMap<>();
        points = new ArrayList<>();
        random = new Random();
    }

    public void initialize(String axiom, double angle, double initialAngle) {
        this.axiom = axiom;
        this.angle = Math.toRadians(angle);
        this.initialAngle = Math.toRadians(initialAngle);
        this.rules.clear();
    }

    public void addRule(char key, String value) {
        rules.put(key, value);
    }


    //    Интерпретация команд:
    //    F - шаг вперед с рисованием
    //    f - шаг вперед без рисования
    //    + - поворот налево
    //    - - поворот направо
    //    [ - запомнить положение
    //    ] - вернуться к сохраненному положению

    // Начало: "F"
    // 1 итерация: "F-F++F-F"
    // 2 итерация: "F-F++F-F - F-F++F-F ++ F-F++F-F - F-F++F-F"
    // и т.д.
    public void generate(int iterations, double step, boolean useRandomness) {
        points.clear();

        String result = axiom;
        for (int i = 0; i < iterations; i++)
            result = applyRules(result);

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
        TurtleState currentState = new TurtleState(new Point(0, 0), initialAngle, step);

        for (char c : lString.toCharArray()) {
            switch (c) {
                case 'F':
                case 'G':
                    Point newPoint = calculateNewPoint(currentState);
                    points.add(currentState.position.copy());
                    points.add(newPoint.copy());
                    currentState.position = newPoint;
                    break;

                case 'f':
                case 'g':
                    currentState.position = calculateNewPoint(currentState);
                    break;

                case '+':
                    currentState.angle += getAngleWithRandomness(useRandomness);
                    break;

                case '-':
                    currentState.angle -= getAngleWithRandomness(useRandomness);
                    break;

                case '[':
                    stateStack.push(currentState.copy());
                    break;

                case ']':
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
            return angle * (0.8 + 0.4 * random.nextDouble());
        }
        return angle;
    }

    private Point calculateNewPoint(TurtleState state) {
        double newX = state.position.x + Math.cos(state.angle) * state.step;
        double newY = state.position.y + Math.sin(state.angle) * state.step;
        return new Point(newX, newY);
    }

    public List<Point> getPoints() {
        return points;
    }

    public void clearPoints() {
        points.clear();
    }
}