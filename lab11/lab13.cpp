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
#include <filesystem>
#include <memory> // Для std::shared_ptr

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

struct Vec3 {
    float x, y, z;
    Vec3 operator+(const Vec3& other) const { return { x + other.x, y + other.y, z + other.z }; }
    Vec3 operator-(const Vec3& other) const { return { x - other.x, y - other.y, z - other.z }; }
    Vec3 operator*(float s) const { return { x * s, y * s, z * s }; }
    Vec3 normalize() const {
        float len = std::sqrt(x * x + y * y + z * z);
        return { x / len, y / len, z / len };
    }
    float dot(const Vec3& other) const { return x * other.x + y * other.y + z * other.z; }
    Vec3 cross(const Vec3& other) const {
        return { y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x };
    }
};

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
        Mat4 res = { 0 };        
        for (int row = 0; row < 4; ++row) {
            for (int col = 0; col < 4; ++col) {
                for (int k = 0; k < 4; ++k) {
                    res.m[row + col * 4] += m[row + k * 4] * other.m[k + col * 4];
                }
            }
        }
        return res;
    }

    static Mat4 LookAt(const Vec3& eye, const Vec3& center, const Vec3& up) {
        Vec3 f = (center - eye).normalize();
        Vec3 s = f.cross(up).normalize();
        Vec3 u = s.cross(f);

        Mat4 res = Identity();

        res.m[0] = s.x;  res.m[4] = s.y;  res.m[8] = s.z;
        res.m[1] = u.x;  res.m[5] = u.y;  res.m[9] = u.z;
        res.m[2] = -f.x; res.m[6] = -f.y; res.m[10] = -f.z;
        
        res.m[12] = -(s.dot(eye));
        res.m[13] = -(u.dot(eye));
        res.m[14] = -(-f.dot(eye));

        return res;
    }

    const float* data() const { return m; }
};

// --- Загрузчик ---
class OBJModel {
    GLuint VAO, VBO;
    size_t vertexCount;
public:
    OBJModel() : VAO(0), VBO(0), vertexCount(0) {}

    // Запрещаем копирование, чтобы случайно не удалить VBO дважды
    OBJModel(const OBJModel&) = delete;
    OBJModel& operator=(const OBJModel&) = delete;

    void clear() {
        if (VAO) glDeleteVertexArrays(1, &VAO);
        if (VBO) glDeleteBuffers(1, &VBO);
        VAO = 0; VBO = 0; vertexCount = 0;
    }

    ~OBJModel() { clear(); }

    bool loadFromFile(const std::string& filename) {
        if (VAO != 0) clear(); // Чистим перед новой загрузкой

        std::vector<float> positions, texCoords, finalVertices;
        std::ifstream file(filename);

        if (!file.is_open()) {
             std::cerr << "ERROR: Could not open model file: " << filename << "\n";
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

        std::cout << "[LOADED] " << filename << " (" << vertexCount / 3 << " polygons)\n";
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

// --- Структура Планеты ---
struct Planet {
    std::shared_ptr<OBJModel> model; // У каждой планеты своя модель
    std::string name;                // Имя для UI
    float distance;
    float orbitSpeed;
    float scale;
    float selfRotation;
    float phase;
};

// Функция помощник для смены модели через консоль
void changeModelFromFile(std::shared_ptr<OBJModel> targetModel, const std::string& objectName) {
    std::cout << "\n----------------------------------------\n";
    std::cout << "[INPUT] Enter new .obj filename for " << objectName << ": ";
    std::string filename;
    std::cin >> filename;

    if (std::filesystem::exists(filename)) {
        if (targetModel->loadFromFile(filename)) {
            std::cout << "[SUCCESS] " << objectName << " updated!\n";
        } else {
            std::cout << "[ERROR] Failed to parse " << filename << "\n";
        }
    } else {
        std::cout << "[ERROR] File not found: " << filename << "\n";
    }
    std::cout << "----------------------------------------\n";
}

class Camera {
private:
    Vec3 position;
    Vec3 front;
    Vec3 up;
    Vec3 right;

    float yaw;
    float pitch;

    void updateCameraVectors() {        
        front.x = cos(yaw) * cos(pitch);
        front.y = sin(pitch);
        front.z = sin(yaw) * cos(pitch);
        front = front.normalize();
        
        right = front.cross({ 0.0f, 1.0f, 0.0f }).normalize();
        up = right.cross(front).normalize();
    }

public:
    Camera(Vec3 pos = { 0.0f, 0.0f, 15.0f }, float initialYaw = -1.57f, float initialPitch = 0.0f) :
        position(pos), up({ 0.0f, 1.0f, 0.0f }), yaw(initialYaw), pitch(initialPitch) {
        updateCameraVectors();
    }

    Mat4 getViewMatrix() const {        
        return Mat4::LookAt(position, position + front, up);
    }

    void processKeyboard(float direction, float velocity) {        
        if (direction == 0) position = position + front * velocity;
        if (direction == 1) position = position - front * velocity;
        if (direction == 2) position = position - right * velocity;
        if (direction == 3) position = position + right * velocity;
        if (direction == 4) position.y += velocity;
        if (direction == 5) position.y -= velocity;
    }

    void processMouseMovement(float xOffset, float yOffset, bool constrainPitch = true) {
        float sensitivity = 0.005f;
        yaw += xOffset * sensitivity;
        pitch += yOffset * sensitivity;
        
        if (constrainPitch) {
            if (pitch > 1.57f) pitch = 1.57f;
            if (pitch < -1.57f) pitch = -1.57f;
        }
        updateCameraVectors();
    }
};

int main() {
    sf::ContextSettings settings;
    settings.depthBits = 24; settings.majorVersion = 3; settings.minorVersion = 3;
    sf::Window window(sf::VideoMode({800, 600}), "Lab 13: Full Customization", sf::State::Windowed, settings);
    window.setVerticalSyncEnabled(true);
    window.setActive(true);

    glewExperimental = GL_TRUE; glewInit();
    glEnable(GL_DEPTH_TEST);

    // --- Инструкция ---
    std::cout << "========================================\n";
    std::cout << "      SOLAR SYSTEM EDITOR v1.0          \n";
    std::cout << "========================================\n";
    std::cout << "Press [0] -> Change SUN model\n";
    std::cout << "Press [1] -> Change MERCURY model\n";
    std::cout << "Press [2] -> Change VENUS model\n";
    std::cout << "Press [3] -> Change EARTH model\n";
    std::cout << "Press [4] -> Change MARS model\n";
    std::cout << "----------------------------------------\n";

    // --- Инициализация Моделей ---
    // Создаем "умные указатели", чтобы владеть моделями
    auto sunModel = std::make_shared<OBJModel>();
    sunModel->loadFromFile("donut.obj"); // Дефолт для солнца

    // Вектор планет. Каждая получает свой уникальный shared_ptr с начальной моделью
    std::vector<Planet> planets;

    // 1. Меркурий (Пирамида)
    auto m1 = std::make_shared<OBJModel>(); m1->loadFromFile("pyramid.obj");
    planets.push_back({m1, "Mercury", 3.0f, 1.2f, 0.4f, 2.0f, 0.0f});

    // 2. Венера (Куб)
    auto m2 = std::make_shared<OBJModel>(); m2->loadFromFile("cube.obj");
    planets.push_back({m2, "Venus", 5.0f, 0.8f, 0.5f, 1.5f, 2.0f});

    // 3. Земля (Пончик)
    auto m3 = std::make_shared<OBJModel>(); m3->loadFromFile("donut.obj");
    planets.push_back({m3, "Earth", 7.5f, 0.5f, 0.6f, 3.0f, 4.0f});

    // 4. Марс (Куб)
    auto m4 = std::make_shared<OBJModel>(); m4->loadFromFile("cube.obj");
    planets.push_back({m4, "Mars", 10.0f, 0.3f, 0.4f, 2.5f, 1.0f});


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

    sf::Clock clock;
    float cameraZ = -15.0f;
    float currentWidth = 800, currentHeight = 600;
    
    const float SUN_PLANET_SCALE_RATIO = 4.0f;
    Camera camera({ 0.0f, 0.0f, 15.0f });
    float lastX = currentWidth / 2.0f;
    float lastY = currentHeight / 2.0f;
    bool firstMouse = true;

    const float CAMERA_SPEED = 10.0f;
    bool mouseCaptured = true;

    window.setMouseCursorVisible(!mouseCaptured);
    sf::Mouse::setPosition(sf::Vector2i(lastX, lastY), window);
    float totalTime = 0.0f;

    while (window.isOpen()) {
        float deltaTime = clock.getElapsedTime().asSeconds();
        clock.restart();
        float cameraVelocity = CAMERA_SPEED * deltaTime;
        totalTime += deltaTime;

        while (const auto event = window.pollEvent()) {
            if (event->is<sf::Event::Closed>()) window.close();

            if (const auto* k = event->getIf<sf::Event::KeyPressed>()) {
                if (k->scancode == sf::Keyboard::Scancode::Escape) window.close();

                if (k->scancode == sf::Keyboard::Scancode::Tab) {
                    mouseCaptured = !mouseCaptured;
                    window.setMouseCursorVisible(!mouseCaptured);                    
                    if (mouseCaptured) {
                        sf::Mouse::setPosition(sf::Vector2i(currentWidth / 2, currentHeight / 2), window);
                        lastX = currentWidth / 2;
                        lastY = currentHeight / 2;
                        firstMouse = false;
                    }
                }

                // 0: Солнце
                if (k->scancode == sf::Keyboard::Scancode::Num0) {
                    changeModelFromFile(sunModel, "Sun");
                }

                // 1..4: Планеты
                if (k->scancode == sf::Keyboard::Scancode::Num1 && planets.size() > 0)
                    changeModelFromFile(planets[0].model, planets[0].name);

                if (k->scancode == sf::Keyboard::Scancode::Num2 && planets.size() > 1)
                    changeModelFromFile(planets[1].model, planets[1].name);

                if (k->scancode == sf::Keyboard::Scancode::Num3 && planets.size() > 2)
                    changeModelFromFile(planets[2].model, planets[2].name);

                if (k->scancode == sf::Keyboard::Scancode::Num4 && planets.size() > 3)
                    changeModelFromFile(planets[3].model, planets[3].name);

                if (k->scancode == sf::Keyboard::Scancode::W) camera.processKeyboard(0, cameraVelocity);
                if (k->scancode == sf::Keyboard::Scancode::S) camera.processKeyboard(1, cameraVelocity);
                if (k->scancode == sf::Keyboard::Scancode::A) camera.processKeyboard(2, cameraVelocity);
                if (k->scancode == sf::Keyboard::Scancode::D) camera.processKeyboard(3, cameraVelocity);
                if (k->scancode == sf::Keyboard::Scancode::Space) camera.processKeyboard(4, cameraVelocity);
                if (k->scancode == sf::Keyboard::Scancode::C) camera.processKeyboard(5, cameraVelocity);
            }

            if (const auto* m = event->getIf<sf::Event::MouseMoved>()) {
                if (mouseCaptured) {
                    if (firstMouse) { lastX = m->position.x; lastY = m->position.y; firstMouse = false; }

                    float xOffset = m->position.x - lastX;
                    float yOffset = lastY - m->position.y;

                    lastX = m->position.x;
                    lastY = m->position.y;

                    camera.processMouseMovement(xOffset, yOffset);
                    
                    sf::Mouse::setPosition(sf::Vector2i(currentWidth / 2, currentHeight / 2), window);
                    lastX = currentWidth / 2;
                    lastY = currentHeight / 2;
                }
            }

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

        float time = deltaTime;
        Mat4 projection = Mat4::Perspective(1.0f, currentWidth/currentHeight, 0.1f, 100.0f);
        Mat4 view = camera.getViewMatrix();

        glUniformMatrix4fv(projLoc, 1, GL_FALSE, projection.data());
        glUniformMatrix4fv(viewLoc, 1, GL_FALSE, view.data());

        float maxPlanetScale = 0.0f;
        for (const auto& planet : planets) {
            if (planet.scale > maxPlanetScale) {
                maxPlanetScale = planet.scale;
            }
        }

        // ОТРИСОВКА СОЛНЦА
        {
            float sunScale = maxPlanetScale * SUN_PLANET_SCALE_RATIO;
            Mat4 model = Mat4::RotateY(totalTime * 0.2f);
            model = model * Mat4::Scale(sunScale);
            glUniformMatrix4fv(modelLoc, 1, GL_FALSE, model.data());
            sunModel->draw();
        }

        // ОТРИСОВКА ПЛАНЕТ
        for (const auto& planet : planets) {
            float angle = totalTime * planet.orbitSpeed + planet.phase;
            float x = cos(angle) * planet.distance;
            float z = sin(angle) * planet.distance;

            Mat4 scaleMat = Mat4::Scale(planet.scale);
            Mat4 rotMat = Mat4::RotateX(totalTime * planet.selfRotation);
            rotMat = rotMat * Mat4::RotateY(totalTime * planet.selfRotation);
            Mat4 transMat = Mat4::Translate(x, 0.0f, z);
            Mat4 model = transMat * rotMat * scaleMat;

            glUniformMatrix4fv(modelLoc, 1, GL_FALSE, model.data());

            planet.model->draw();
        }

        window.display();
    }
    return 0;
}