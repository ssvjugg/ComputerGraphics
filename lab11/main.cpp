#include <GL/glew.h>
#include <SFML/Window.hpp>
#include <SFML/Graphics.hpp>
#include <iostream>
#include <vector>
#include <cmath>
#include <optional>

#define GL_SILENCE_DEPRECATION

// --- Глобальные переменные ---
GLuint Program;
GLint Attrib_vertex;
GLint Unif_colorType;

// Используем только VBO (буферы данных), VAO будем игнорировать для надежности
GLuint VBO_quad, VBO_fan, VBO_pentagon;

// Счетчики вершин
int quad_vertex_count = 0;
int fan_vertex_count = 0;
int pentagon_vertex_count = 0;

struct Vertex {
    GLfloat x;
    GLfloat y;
};

// --- ШЕЙДЕРЫ ---
const char* VertexShaderSource = R"(
#version 120
attribute vec2 coord;
void main() {
   // Применяем матрицу проекции и модели
   gl_Position = gl_ModelViewProjectionMatrix * vec4(coord, 0.0, 1.0);
}
)";

const char* FragShaderSource = R"(
#version 120
uniform int figureType;
void main() {
    if (figureType == 0) {
        gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0); // 0: Красный
    } else if (figureType == 1) {
        gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0); // 1: Зеленый
    } else {
        gl_FragColor = vec4(0.0, 0.0, 1.0, 1.0); // 2: Синий
    }
}
)";

void InitShader() {
    GLuint vShader = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vShader, 1, &VertexShaderSource, NULL);
    glCompileShader(vShader);

    GLuint fShader = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fShader, 1, &FragShaderSource, NULL);
    glCompileShader(fShader);

    Program = glCreateProgram();
    glAttachShader(Program, vShader);
    glAttachShader(Program, fShader);
    glLinkProgram(Program);

    Attrib_vertex = glGetAttribLocation(Program, "coord");
    Unif_colorType = glGetUniformLocation(Program, "figureType");

    glDeleteShader(vShader);
    glDeleteShader(fShader);
}

// 1. Создаем данные Квадрата
void InitQuadData() {
    std::vector<Vertex> vertices = {
        { -0.5f,  0.5f },
        { -0.5f, -0.5f },
        {  0.5f, -0.5f },
        {  0.5f,  0.5f }
    };
    quad_vertex_count = vertices.size();

    glGenBuffers(1, &VBO_quad);
    glBindBuffer(GL_ARRAY_BUFFER, VBO_quad);
    glBufferData(GL_ARRAY_BUFFER, vertices.size() * sizeof(Vertex), vertices.data(), GL_STATIC_DRAW);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
}

// 2. Создаем данные Веера (Полукруг)
void InitFanData() {
    std::vector<Vertex> vertices;
    vertices.push_back({0.0f, -0.5f}); // Центр внизу

    // Генерируем 30 сегментов (очень гладкий полукруг)
    int segments = 30;
    float radius = 0.8f;

    // От 0 до 180 градусов (PI)
    for (int i = 0; i <= segments; i++) {
        float angle = M_PI * i / segments;
        vertices.push_back({
            radius * cos(angle),
            radius * sin(angle) - 0.5f
        });
    }
    fan_vertex_count = vertices.size();

    glGenBuffers(1, &VBO_fan);
    glBindBuffer(GL_ARRAY_BUFFER, VBO_fan);
    glBufferData(GL_ARRAY_BUFFER, vertices.size() * sizeof(Vertex), vertices.data(), GL_STATIC_DRAW);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
}

// 3. Создаем данные Пятиугольника
void InitPentagonData() {
    std::vector<Vertex> vertices;
    vertices.push_back({0.0f, 0.0f}); // Центр

    int sides = 5;
    float radius = 0.5f;

    // Полный круг (2*PI), деленный на 5
    for (int i = 0; i <= sides; i++) {
        float angle = 2.0f * M_PI * i / sides + M_PI / 2.0f;
        vertices.push_back({
            radius * cos(angle),
            radius * sin(angle)
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
    std::cout << "--- ЗАПУСК ВЕРСИИ С ЯВНОЙ ПРИВЯЗКОЙ (FIX) ---" << std::endl;

    sf::ContextSettings settings;
    settings.depthBits = 24;
    settings.majorVersion = 2;
    settings.minorVersion = 1;

    sf::Window window(sf::VideoMode({800, 600}), "Fixed Shapes", sf::State::Windowed, settings);
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

    while (window.isOpen()) {
        while (std::optional<sf::Event> event = window.pollEvent()) {
            if (event->is<sf::Event::Closed>()) window.close();
            else if (const sf::Event::Resized* resized = event->getIf<sf::Event::Resized>()) {
                glViewport(0, 0, resized->size.x, resized->size.y);
            }
        }

        glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        glUseProgram(Program);
        glEnableVertexAttribArray(Attrib_vertex);

        // --- 1. КВАДРАТ (Красный) ---
        glLoadIdentity();
        glTranslatef(-1.5f, 0.0f, 0.0f);
        glUniform1i(Unif_colorType, 0);

        // ЯВНАЯ ПРИВЯЗКА: Подключаем буфер квадрата прямо перед рисованием
        glBindBuffer(GL_ARRAY_BUFFER, VBO_quad);
        glVertexAttribPointer(Attrib_vertex, 2, GL_FLOAT, GL_FALSE, sizeof(Vertex), (void*)0);
        glDrawArrays(GL_TRIANGLE_FAN, 0, quad_vertex_count);

        // --- 2. ВЕЕР (Зеленый) ---
        glLoadIdentity();
        glTranslatef(0.0f, 0.0f, 0.0f);
        glUniform1i(Unif_colorType, 1);

        // Подключаем буфер веера
        glBindBuffer(GL_ARRAY_BUFFER, VBO_fan);
        // Заново говорим OpenGL, где брать данные
        glVertexAttribPointer(Attrib_vertex, 2, GL_FLOAT, GL_FALSE, sizeof(Vertex), (void*)0);
        // Рисуем (теперь точно возьмет данные из VBO_fan)
        glDrawArrays(GL_TRIANGLE_FAN, 0, fan_vertex_count);

        // --- 3. ПЯТИУГОЛЬНИК (Синий) ---
        glLoadIdentity();
        glTranslatef(1.5f, 0.0f, 0.0f);
        glUniform1i(Unif_colorType, 2);

        // Подключаем буфер пятиугольника
        glBindBuffer(GL_ARRAY_BUFFER, VBO_pentagon);
        glVertexAttribPointer(Attrib_vertex, 2, GL_FLOAT, GL_FALSE, sizeof(Vertex), (void*)0);
        glDrawArrays(GL_TRIANGLE_FAN, 0, pentagon_vertex_count);

        // Отключаем
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glUseProgram(0);

        window.display();
    }

    Release();
    return 0;
}