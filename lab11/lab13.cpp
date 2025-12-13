#include <GL/glew.h>
#include <SFML/Window.hpp>
#include <SFML/Graphics.hpp>
#include <SFML/System.hpp>

#include <iostream>
#include <vector>
#include <fstream>
#include <sstream>
#include <string>
#include <cmath>

// --- Шейдеры ---
const char* vertexShaderSource = R"(
    #version 330 core
    layout (location = 0) in vec3 aPos;
    layout (location = 1) in vec2 aTexCoord;

    out vec2 TexCoord;

    uniform mat4 model;
    uniform mat4 view;
    uniform mat4 projection;

    void main() {
        gl_Position = projection * view * model * vec4(aPos, 1.0);
        TexCoord = aTexCoord;
    }
)";

const char* fragmentShaderSource = R"(
    #version 330 core
    out vec4 FragColor;
    in vec2 TexCoord;

    uniform sampler2D texture1;

    void main() {
        FragColor = texture(texture1, TexCoord);
    }
)";

// --- Математика ---
struct Mat4 {
    float m[16];

    static Mat4 Identity() {
        Mat4 res = {0};
        res.m[0] = res.m[5] = res.m[10] = res.m[15] = 1.0f;
        return res;
    }

    static Mat4 Perspective(float fov, float aspect, float nearPlane, float farPlane) {
        Mat4 res = {0};
        float tanHalfFovy = tan(fov / 2.0f);
        res.m[0] = 1.0f / (aspect * tanHalfFovy);
        res.m[5] = 1.0f / (tanHalfFovy);
        res.m[10] = -(farPlane + nearPlane) / (farPlane - nearPlane);
        res.m[11] = -1.0f;
        res.m[14] = -(2.0f * farPlane * nearPlane) / (farPlane - nearPlane);
        return res;
    }

    static Mat4 Translate(float x, float y, float z) {
        Mat4 res = Identity();
        res.m[12] = x; res.m[13] = y; res.m[14] = z;
        return res;
    }

    static Mat4 Scale(float s) {
        Mat4 res = Identity();
        res.m[0] = s; res.m[5] = s; res.m[10] = s;
        return res;
    }

    static Mat4 RotateY(float angle) {
        Mat4 res = Identity();
        float c = cos(angle);
        float s = sin(angle);
        res.m[0] = c; res.m[2] = s;
        res.m[8] = -s; res.m[10] = c;
        return res;
    }

    static Mat4 RotateX(float angle) {
        Mat4 res = Identity();
        float c = cos(angle);
        float s = sin(angle);
        res.m[5] = c; res.m[6] = -s;
        res.m[9] = s; res.m[10] = c;
        return res;
    }

    Mat4 operator*(const Mat4& other) const {
        Mat4 res = {0};
        for(int row = 0; row < 4; ++row) {
            for(int col = 0; col < 4; ++col) {
                for(int k = 0; k < 4; ++k) {
                    res.m[col * 4 + row] += m[k * 4 + row] * other.m[col * 4 + k];
                }
            }
        }
        return res;
    }

    const float* data() const { return m; }
};

// --- ГЕНЕРАТОРЫ ФАЙЛОВ ---

// 1. Пончик
void generateDonutOBJ(const std::string& filename) {
    std::ofstream file(filename);
    if (!file.is_open()) return;
    file << "# Generated Donut\n";
    int segments = 40, rings = 20;
    float majorRadius = 1.0f, minorRadius = 0.4f;
    for (int i = 0; i <= segments; ++i) {
        float theta = i * 2.0f * 3.14159f / segments;
        for (int j = 0; j <= rings; ++j) {
            float phi = j * 2.0f * 3.14159f / rings;
            float x = (majorRadius + minorRadius * cos(phi)) * cos(theta);
            float y = (majorRadius + minorRadius * cos(phi)) * sin(theta);
            float z = minorRadius * sin(phi);
            float u = (float)i / segments; float v = (float)j / rings;
            file << "v " << x << " " << y << " " << z << "\n";
            file << "vt " << u << " " << v << "\n";
        }
    }
    for (int i = 0; i < segments; ++i) {
        for (int j = 0; j < rings; ++j) {
            int current = i * (rings + 1) + j + 1;
            int next = current + (rings + 1);
            file << "f " << current << "/" << current << " " << (current + 1) << "/" << (current + 1) << " " << next << "/" << next << "\n";
            file << "f " << next << "/" << next << " " << (current + 1) << "/" << (current + 1) << " " << (next + 1) << "/" << (next + 1) << "\n";
        }
    }
}

// 2. Куб (Процедурная генерация для файла)
void generateCubeOBJ(const std::string& filename) {
    std::ofstream file(filename);
    if (!file.is_open()) return;
    file << "# Generated Cube\n";

    // 8 вершин куба
    file << "v -1 -1 -1\n v 1 -1 -1\n v 1 1 -1\n v -1 1 -1\n"; // Задняя грань (1-4)
    file << "v -1 -1 1\n v 1 -1 1\n v 1 1 1\n v -1 1 1\n";     // Передняя грань (5-8)

    // Текстурные координаты (простые)
    file << "vt 0 0\n vt 1 0\n vt 1 1\n vt 0 1\n";

    // Грани (Cube faces) - индексы v/vt
    // Front
    file << "f 5/1 6/2 7/3\n f 5/1 7/3 8/4\n";
    // Back
    file << "f 2/1 1/2 4/3\n f 2/1 4/3 3/4\n";
    // Left
    file << "f 1/1 5/2 8/3\n f 1/1 8/3 4/4\n";
    // Right
    file << "f 6/1 2/2 3/3\n f 6/1 3/3 7/4\n";
    // Top
    file << "f 4/1 8/2 7/3\n f 4/1 7/3 3/4\n";
    // Bottom
    file << "f 5/1 1/2 2/3\n f 5/1 2/3 6/4\n";
}

// 3. Пирамида
void generatePyramidOBJ(const std::string& filename) {
    std::ofstream file(filename);
    if (!file.is_open()) return;
    file << "# Generated Pyramid\n";

    // Вершина (1) и Основание (2,3,4,5)
    file << "v 0 1 0\n";
    file << "v -1 -1 1\n v 1 -1 1\n v 1 -1 -1\n v -1 -1 -1\n";

    file << "vt 0.5 1\n vt 0 0\n vt 1 0\n"; // Для боков
    file << "vt 0 0\n vt 1 0\n vt 1 1\n vt 0 1\n"; // Для дна

    // Sides
    file << "f 2/2 3/3 1/1\n"; // Front
    file << "f 3/2 4/3 1/1\n"; // Right
    file << "f 4/2 5/3 1/1\n"; // Back
    file << "f 5/2 2/3 1/1\n"; // Left

    // Base (Quad as 2 tris)
    file << "f 5/4 4/5 3/6\n f 5/4 3/6 2/7\n";
}

// --- Загрузчик ---
class OBJModel {
    GLuint VAO, VBO;
    size_t vertexCount;
public:
    OBJModel() : VAO(0), VBO(0), vertexCount(0) {}
    ~OBJModel() {
        if (VAO) glDeleteVertexArrays(1, &VAO);
        if (VBO) glDeleteBuffers(1, &VBO);
    }
    bool loadFromFile(const std::string& filename) {
        std::vector<float> positions, texCoords, finalVertices;
        std::ifstream file(filename);
        if (!file.is_open()) {
             std::cerr << "Error loading " << filename << "\n";
             return false;
        }

        std::string line;
        while (std::getline(file, line)) {
            std::stringstream ss(line);
            std::string prefix;
            ss >> prefix;
            if (prefix == "v") {
                float x, y, z; ss >> x >> y >> z;
                positions.insert(positions.end(), {x, y, z});
            } else if (prefix == "vt") {
                float u, v; ss >> u >> v;
                texCoords.insert(texCoords.end(), {u, v});
            } else if (prefix == "f") {
                std::string vertexData;
                while (ss >> vertexData) {
                    std::stringstream vss(vertexData);
                    std::string indexStr;
                    std::vector<int> indices;
                    while (std::getline(vss, indexStr, '/'))
                        indices.push_back(indexStr.empty() ? 0 : std::stoi(indexStr));

                    if (indices.size() >= 1) {
                        int p = indices[0] - 1;
                        if (p < positions.size()/3)
                            finalVertices.insert(finalVertices.end(), {positions[p*3], positions[p*3+1], positions[p*3+2]});
                        else finalVertices.insert(finalVertices.end(), {0,0,0});
                    }
                    if (indices.size() >= 2) {
                        int t = indices[1] - 1;
                        if (t < texCoords.size()/2)
                            finalVertices.insert(finalVertices.end(), {texCoords[t*2], texCoords[t*2+1]});
                        else finalVertices.insert(finalVertices.end(), {0,0});
                    } else finalVertices.insert(finalVertices.end(), {0.0f, 0.0f});
                }
            }
        }
        vertexCount = finalVertices.size() / 5;
        glGenVertexArrays(1, &VAO);
        glGenBuffers(1, &VBO);
        glBindVertexArray(VAO);
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, finalVertices.size() * sizeof(float), finalVertices.data(), GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 5 * sizeof(float), (void*)0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 5 * sizeof(float), (void*)(3 * sizeof(float)));
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
        return true;
    }
    void draw() {
        if (VAO) { glBindVertexArray(VAO); glDrawArrays(GL_TRIANGLES, 0, vertexCount); }
    }
};

GLuint createShaderProgram() {
    GLuint v = glCreateShader(GL_VERTEX_SHADER); glShaderSource(v, 1, &vertexShaderSource, NULL); glCompileShader(v);
    GLuint f = glCreateShader(GL_FRAGMENT_SHADER); glShaderSource(f, 1, &fragmentShaderSource, NULL); glCompileShader(f);
    GLuint p = glCreateProgram(); glAttachShader(p, v); glAttachShader(p, f); glLinkProgram(p);
    glDeleteShader(v); glDeleteShader(f);
    return p;
}

// --- Структура для планет ---
struct Planet {
    OBJModel* model;    // Указатель на модель (какую фигуру рисовать)
    float distance;     // Расстояние
    float orbitSpeed;   // Скорость орбиты
    float scale;        // Размер
    float selfRotation; // Вращение вокруг оси
    float phase;        // Сдвиг фазы
};

int main() {
    sf::ContextSettings settings;
    settings.depthBits = 24; settings.majorVersion = 3; settings.minorVersion = 3;
    sf::Window window(sf::VideoMode({800, 600}), "Lab 13: Shapes System", sf::State::Windowed, settings);
    window.setVerticalSyncEnabled(true);
    window.setActive(true);

    glewExperimental = GL_TRUE; glewInit();
    glEnable(GL_DEPTH_TEST);

    // 1. Генерируем ФАЙЛЫ
    generateDonutOBJ("donut.obj");
    generateCubeOBJ("cube.obj");
    generatePyramidOBJ("pyramid.obj");

    // 2. Загружаем модели в память
    OBJModel donutModel;
    donutModel.loadFromFile("donut.obj");

    OBJModel cubeModel;
    cubeModel.loadFromFile("cube.obj");

    OBJModel pyramidModel;
    pyramidModel.loadFromFile("pyramid.obj");

    sf::Texture sfTexture;
    if (!sfTexture.loadFromFile("papich.jpg")) {
        sf::Image white; white.resize(sf::Vector2u(100,100), sf::Color::White);
        sfTexture.loadFromImage(white);
    }
    GLuint textureID = sfTexture.getNativeHandle();

    GLuint shaderProgram = createShaderProgram();
    glUseProgram(shaderProgram);
    GLint modelLoc = glGetUniformLocation(shaderProgram, "model");
    GLint viewLoc = glGetUniformLocation(shaderProgram, "view");
    GLint projLoc = glGetUniformLocation(shaderProgram, "projection");

    // --- НАСТРОЙКА СИСТЕМЫ ---
    // Теперь указываем разные модели для разных планет
    std::vector<Planet> planets = {
        {&pyramidModel, 3.0f,  1.2f, 0.4f, 2.0f, 0.0f}, // "Меркурий" - Пирамида
        {&cubeModel,    5.0f,  0.8f, 0.5f, 1.5f, 2.0f}, // "Венера" - Куб
        {&donutModel,   7.5f,  0.5f, 0.6f, 3.0f, 4.0f}, // "Земля" - Пончик
        {&cubeModel,    10.0f, 0.3f, 0.4f, 2.5f, 1.0f}  // "Марс" - Снова Куб
    };

    sf::Clock clock;
    float cameraZ = -15.0f;
    float currentWidth = 800, currentHeight = 600;

    while (window.isOpen()) {
        while (const auto event = window.pollEvent()) {
            if (event->is<sf::Event::Closed>()) window.close();
            if (const auto* k = event->getIf<sf::Event::KeyPressed>())
                if (k->scancode == sf::Keyboard::Scancode::Escape) window.close();
            if (const auto* r = event->getIf<sf::Event::Resized>()) {
                currentWidth = r->size.x; currentHeight = r->size.y;
                glViewport(0, 0, r->size.x, r->size.y);
            }
            if (const auto* s = event->getIf<sf::Event::MouseWheelScrolled>())
                cameraZ += s->delta;
        }

        glClearColor(0.05f, 0.05f, 0.1f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glUseProgram(shaderProgram);
        glBindTexture(GL_TEXTURE_2D, textureID);

        float time = clock.getElapsedTime().asSeconds();
        Mat4 projection = Mat4::Perspective(1.0f, currentWidth/currentHeight, 0.1f, 100.0f);
        Mat4 view = Mat4::Translate(0.0f, 0.0f, cameraZ);
        Mat4 systemTilt = Mat4::RotateX(0.5f);
        view = view * systemTilt;

        glUniformMatrix4fv(projLoc, 1, GL_FALSE, projection.data());
        glUniformMatrix4fv(viewLoc, 1, GL_FALSE, view.data());

        // 1. СОЛНЦЕ (Пусть будет большим пончиком)
        {
            Mat4 model = Mat4::RotateY(time * 0.2f);
            model = model * Mat4::Scale(1.5f);
            glUniformMatrix4fv(modelLoc, 1, GL_FALSE, model.data());
            donutModel.draw();
        }

        // 2. ПЛАНЕТЫ
        for (const auto& planet : planets) {
            float angle = time * planet.orbitSpeed + planet.phase;
            float x = cos(angle) * planet.distance;
            float z = sin(angle) * planet.distance;

            Mat4 scaleMat = Mat4::Scale(planet.scale);
            Mat4 rotMat = Mat4::RotateX(time * planet.selfRotation);
            // Добавим вращение и по Y, чтобы интереснее выглядело для кубов
            rotMat = rotMat * Mat4::RotateY(time * planet.selfRotation);

            Mat4 transMat = Mat4::Translate(x, 0.0f, z);
            Mat4 model = transMat * rotMat * scaleMat;

            glUniformMatrix4fv(modelLoc, 1, GL_FALSE, model.data());

            // Самое важное: вызываем draw() у той модели, которая записана в планете
            if (planet.model) {
                planet.model->draw();
            }
        }

        window.display();
    }
    return 0;
}