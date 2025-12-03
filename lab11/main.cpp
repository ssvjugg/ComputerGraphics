#include <GL/glew.h>
#include <SFML/Window.hpp>
#include <SFML/Graphics.hpp>
#include <iostream>
#include <vector>
#include <cmath>
#include <optional>

// Отключаем предупреждения macOS
#define GL_SILENCE_DEPRECATION

// --- Глобальные переменные ---
GLuint Program;
GLint Attrib_coord;   // Атрибут координат
GLint Attrib_color;   // Атрибут цвета (для градиента)
GLint Unif_flatColor; // Uniform для плоского цвета
GLint Unif_useGradient; // Переключатель режимов

GLuint VBO_quad, VBO_fan, VBO_pentagon;
int quad_vertex_count = 0;
int fan_vertex_count = 0;
int pentagon_vertex_count = 0;

struct Vertex {
    GLfloat x, y;       // Позиция
    GLfloat r, g, b;    // Цвет вершины
};

// --- ШЕЙДЕРЫ ---

// Вершинный шейдер
const char* VertexShaderSource = R"(
#version 120
attribute vec2 coord;   // Координаты вершины
attribute vec3 color;   // Цвет вершины

// 'varying' означает, что эта переменная будет ИНТЕРПОЛИРОВАТЬСЯ
// между вершинами при передаче во фрагментный шейдер.
varying vec3 vColor;

void main() {
   gl_Position = gl_ModelViewProjectionMatrix * vec4(coord, 0.0, 1.0);
   vColor = color;      // Передаем цвет для интерполяции
}
)";

// Фрагментный шейдер
const char* FragShaderSource = R"(
#version 120
varying vec3 vColor;       // Сюда приходит уже смешанный (интерполированный) цвет
uniform vec4 flatColor;    // Цвет для режима плоской заливки
uniform int useGradient;   // Переключатель

void main() {
    if (useGradient == 1) {
        // Градиент: используем интерполированный цвет вершин
        gl_FragColor = vec4(vColor, 1.0);
    } else {
        // Плоский: используем общий цвет
        gl_FragColor = flatColor;
    }
}
)";

void InitShader() {
    GLuint vShader = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vShader, 1, &VertexShaderSource, NULL);
    glCompileShader(vShader);

    // Проверка ошибок (упрощенная)
    GLint success;
    glGetShaderiv(vShader, GL_COMPILE_STATUS, &success);
    if(!success) std::cerr << "Vertex Shader Error" << std::endl;

    GLuint fShader = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fShader, 1, &FragShaderSource, NULL);
    glCompileShader(fShader);
    glGetShaderiv(fShader, GL_COMPILE_STATUS, &success);
    if(!success) std::cerr << "Fragment Shader Error" << std::endl;

    Program = glCreateProgram();
    glAttachShader(Program, vShader);
    glAttachShader(Program, fShader);
    glLinkProgram(Program);

    Attrib_coord = glGetAttribLocation(Program, "coord");
    Attrib_color = glGetAttribLocation(Program, "color");
    Unif_flatColor = glGetUniformLocation(Program, "flatColor");
    Unif_useGradient = glGetUniformLocation(Program, "useGradient");

    glDeleteShader(vShader);
    glDeleteShader(fShader);
}

// 1. КВАДРАТ: Каждая из 4 вершин имеет свой уникальный цвет
void InitQuadData() {
    std::vector<Vertex> vertices = {
        // Координаты (X, Y) | Цвет (R, G, B)
        { -0.5f,  0.5f,        1.0f, 0.0f, 0.0f }, // Верх-Лево: Красный
        { -0.5f, -0.5f,        0.0f, 1.0f, 0.0f }, // Низ-Лево:  Зеленый
        {  0.5f, -0.5f,        0.0f, 0.0f, 1.0f }, // Низ-Право: Синий
        {  0.5f,  0.5f,        1.0f, 1.0f, 0.0f }  // Верх-Право: Желтый
    };
    quad_vertex_count = vertices.size();

    glGenBuffers(1, &VBO_quad);
    glBindBuffer(GL_ARRAY_BUFFER, VBO_quad);
    glBufferData(GL_ARRAY_BUFFER, vertices.size() * sizeof(Vertex), vertices.data(), GL_STATIC_DRAW);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
}

// 2. ВЕЕР: Центр белый, крайние точки меняют цвет вдоль дуги
void InitFanData() {
    std::vector<Vertex> vertices;

    // ЦЕНТР веера - Белый (1, 1, 1)
    vertices.push_back({0.0f, -0.5f,  1.0f, 1.0f, 1.0f});

    int segments = 30;
    float radius = 0.8f;

    for (int i = 0; i <= segments; i++) {
        float angle = M_PI * i / segments;

        // Вычисляем цвет в зависимости от угла, чтобы каждая вершина была уникальной
        // t идет от 0.0 до 1.0
        float t = (float)i / segments;

        vertices.push_back({
            radius * cos(angle),        // X
            radius * sin(angle) - 0.5f, // Y
            t,              // Red компонент растет
            1.0f - t,       // Green компонент падает
            0.5f            // Blue фиксированный
        });
    }
    fan_vertex_count = vertices.size();

    glGenBuffers(1, &VBO_fan);
    glBindBuffer(GL_ARRAY_BUFFER, VBO_fan);
    glBufferData(GL_ARRAY_BUFFER, vertices.size() * sizeof(Vertex), vertices.data(), GL_STATIC_DRAW);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
}

// 3. ПЯТИУГОЛЬНИК: Центр серый, вершины разноцветные
void InitPentagonData() {
    std::vector<Vertex> vertices;

    // ЦЕНТР - Темно-серый
    vertices.push_back({0.0f, 0.0f, 0.3f, 0.3f, 0.3f});

    int sides = 5;
    float radius = 0.5f;

    // Цвета для 5 вершин + 1 замыкающая
    float colors[6][3] = {
        {1.0f, 0.0f, 0.0f}, // Красный
        {0.0f, 1.0f, 1.0f}, // Циан
        {1.0f, 0.0f, 1.0f}, // Пурпурный
        {1.0f, 1.0f, 0.0f}, // Желтый
        {0.0f, 1.0f, 0.0f}, // Зеленый
        {1.0f, 0.0f, 0.0f}  // Красный (чтобы замкнуть градиент плавно)
    };

    for (int i = 0; i <= sides; i++) {
        float angle = 2.0f * M_PI * i / sides + M_PI / 2.0f;
        vertices.push_back({
            radius * cos(angle),
            radius * sin(angle),
            colors[i][0], colors[i][1], colors[i][2]
        });
    }
    pentagon_vertex_count = vertices.size();

    glGenBuffers(1, &VBO_pentagon);
    glBindBuffer(GL_ARRAY_BUFFER, VBO_pentagon);
    glBufferData(GL_ARRAY_BUFFER, vertices.size() * sizeof(Vertex), vertices.data(), GL_STATIC_DRAW);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
}

void Init() {
    InitShader();
    InitQuadData();
    InitFanData();
    InitPentagonData();
}

void Release() {
    glDeleteBuffers(1, &VBO_quad);
    glDeleteBuffers(1, &VBO_fan);
    glDeleteBuffers(1, &VBO_pentagon);
    glDeleteProgram(Program);
}

int main() {
    sf::ContextSettings settings;
    settings.depthBits = 24;
    settings.majorVersion = 2;
    settings.minorVersion = 1;

    sf::Window window(sf::VideoMode({800, 600}), "Final Gradient Task", sf::State::Windowed, settings);
    window.setVerticalSyncEnabled(true);
    window.setActive(true);

    glewExperimental = GL_TRUE;
    glewInit();

    Init();

    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    float ratio = 800.0f / 600.0f;
    glOrtho(-2.0 * ratio, 2.0 * ratio, -2.0, 2.0, -1.0, 1.0);

    glMatrixMode(GL_MODELVIEW);

    bool useGradientMode = false; // По умолчанию плоское закрашивание
    std::cout << "Нажмите PROBEL (SPACE) для переключения режимов закраски." << std::endl;

    while (window.isOpen()) {
        while (std::optional<sf::Event> event = window.pollEvent()) {
            if (event->is<sf::Event::Closed>()) window.close();
            else if (const sf::Event::Resized* resized = event->getIf<sf::Event::Resized>()) {
                glViewport(0, 0, resized->size.x, resized->size.y);
            }
            else if (const sf::Event::KeyPressed* key = event->getIf<sf::Event::KeyPressed>()) {
                if (key->code == sf::Keyboard::Key::Space) {
                    useGradientMode = !useGradientMode;
                    std::cout << "Режим: " << (useGradientMode ? "ГРАДИЕНТ (Разные цвета вершин)" : "ПЛОСКИЙ (Uniform)") << std::endl;
                }
            }
        }

        glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        glUseProgram(Program);
        glEnableVertexAttribArray(Attrib_coord);
        glEnableVertexAttribArray(Attrib_color);

        // Передаем флаг режима в шейдер
        glUniform1i(Unif_useGradient, useGradientMode ? 1 : 0);

        // --- 1. КВАДРАТ ---
        glLoadIdentity();
        glTranslatef(-1.5f, 0.0f, 0.0f);
        // Цвет для плоского режима (Красный)
        glUniform4f(Unif_flatColor, 1.0f, 0.0f, 0.0f, 1.0f);

        glBindBuffer(GL_ARRAY_BUFFER, VBO_quad);
        // Смещение 0 для координат, смещение 8 байт (2 float) для цвета
        glVertexAttribPointer(Attrib_coord, 2, GL_FLOAT, GL_FALSE, sizeof(Vertex), (void*)0);
        glVertexAttribPointer(Attrib_color, 3, GL_FLOAT, GL_FALSE, sizeof(Vertex), (void*)(2 * sizeof(GLfloat)));
        glDrawArrays(GL_TRIANGLE_FAN, 0, quad_vertex_count);

        // --- 2. ВЕЕР ---
        glLoadIdentity();
        glTranslatef(0.0f, 0.0f, 0.0f);
        // Цвет для плоского режима (Зеленый)
        glUniform4f(Unif_flatColor, 0.0f, 1.0f, 0.0f, 1.0f);

        glBindBuffer(GL_ARRAY_BUFFER, VBO_fan);
        glVertexAttribPointer(Attrib_coord, 2, GL_FLOAT, GL_FALSE, sizeof(Vertex), (void*)0);
        glVertexAttribPointer(Attrib_color, 3, GL_FLOAT, GL_FALSE, sizeof(Vertex), (void*)(2 * sizeof(GLfloat)));
        glDrawArrays(GL_TRIANGLE_FAN, 0, fan_vertex_count);

        // --- 3. ПЯТИУГОЛЬНИК ---
        glLoadIdentity();
        glTranslatef(1.5f, 0.0f, 0.0f);
        // Цвет для плоского режима (Синий)
        glUniform4f(Unif_flatColor, 0.0f, 0.0f, 1.0f, 1.0f);

        glBindBuffer(GL_ARRAY_BUFFER, VBO_pentagon);
        glVertexAttribPointer(Attrib_coord, 2, GL_FLOAT, GL_FALSE, sizeof(Vertex), (void*)0);
        glVertexAttribPointer(Attrib_color, 3, GL_FLOAT, GL_FALSE, sizeof(Vertex), (void*)(2 * sizeof(GLfloat)));
        glDrawArrays(GL_TRIANGLE_FAN, 0, pentagon_vertex_count);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDisableVertexAttribArray(Attrib_coord);
        glDisableVertexAttribArray(Attrib_color);
        glUseProgram(0);

        window.display();
    }

    Release();
    return 0;
}