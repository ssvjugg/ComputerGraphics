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
#include <memory>
#include <map>
#include <algorithm>

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
    uniform bool hasTexture;
    uniform vec3 objectColor;

    void main() {
        // Для отладки - всегда показываем красный цвет
        // FragColor = vec4(1.0, 0.0, 0.0, 1.0);

        if (hasTexture) {
            vec4 texColor = texture(texture1, TexCoord);
            if (texColor.a < 0.5) discard; // отбрасываем прозрачные пиксели
            FragColor = texColor;
        } else {
            FragColor = vec4(objectColor, 1.0);
        }
    }
)";

struct Vec3 {
    float x, y, z;
    Vec3 operator+(const Vec3& other) const { return { x + other.x, y + other.y, z + other.z }; }
    Vec3 operator-(const Vec3& other) const { return { x - other.x, y - other.y, z - other.z }; }
    Vec3 operator*(float s) const { return { x * s, y * s, z * s }; }
    Vec3 normalize() const {
        float len = std::sqrt(x * x + y * y + z * z);
        if (len == 0) return {0,0,0};
        return { x / len, y / len, z / len };
    }
    float dot(const Vec3& other) const { return x * other.x + y * other.y + z * other.z; }
    Vec3 cross(const Vec3& other) const {
        return { y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x };
    }
};

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

    static Mat4 Scale(float sx, float sy, float sz) {
        Mat4 res = Identity();
        res.m[0] = sx; res.m[5] = sy; res.m[10] = sz;
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

    static Mat4 RotateZ(float angle) {
        Mat4 res = Identity();
        float c = cos(angle);
        float s = sin(angle);
        res.m[0] = c; res.m[1] = -s;
        res.m[4] = s; res.m[5] = c;
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

// --- Класс для загрузки текстур ---
// --- Класс для загрузки текстур ---
class TextureLoader {
private:
    std::map<std::string, GLuint> textureCache;

    GLuint loadTextureDirectly(const std::string& path) {
        // Загружаем изображение через SFML
        sf::Image image;
        if (!image.loadFromFile(path)) {
            // Создаем белую текстуру
            image = sf::Image(sf::Vector2u(100, 100), sf::Color::White);
            std::cout << "[TEXTURE] Created default white texture for: " << path << std::endl;
        } else {
            std::cout << "[TEXTURE] Loaded: " << path << std::endl;
        }

        // Преобразуем в формат, понятный OpenGL
        image.flipVertically(); // OpenGL координаты текстуры идут снизу вверх

        GLuint textureID;
        glGenTextures(1, &textureID);
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Получаем данные изображения
        const std::uint8_t* pixels = image.getPixelsPtr();
        sf::Vector2u size = image.getSize();

        // Определяем формат пикселей
        GLenum format = GL_RGBA;
        GLenum internalFormat = GL_RGBA;

        // Загружаем текстуру в OpenGL
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, size.x, size.y, 0, format, GL_UNSIGNED_BYTE, pixels);

        // Устанавливаем параметры текстуры
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Генерируем мипмапы
        glGenerateMipmap(GL_TEXTURE_2D);

        glBindTexture(GL_TEXTURE_2D, 0);

        return textureID;
    }

public:
    ~TextureLoader() {
        for (auto& pair : textureCache) {
            glDeleteTextures(1, &pair.second);
        }
    }

    GLuint loadTexture(const std::string& path) {
        // Проверяем кэш
        auto it = textureCache.find(path);
        if (it != textureCache.end()) {
            return it->second;
        }

        // Загружаем текстуру напрямую через OpenGL
        GLuint textureID = loadTextureDirectly(path);
        textureCache[path] = textureID;
        return textureID;
    }

    void clearCache() {
        for (auto& pair : textureCache) {
            glDeleteTextures(1, &pair.second);
        }
        textureCache.clear();
    }
};

// --- Класс модели с текстурой ---
class TexturedModel {
private:
    GLuint VAO, VBO;
    size_t vertexCount;
    GLuint textureID;
    Vec3 color;
    bool hasTextureFlag;

public:
    TexturedModel() : VAO(0), VBO(0), vertexCount(0), textureID(0), color({1.0f, 1.0f, 1.0f}), hasTextureFlag(false) {}

    // Запрещаем копирование
    TexturedModel(const TexturedModel&) = delete;
    TexturedModel& operator=(const TexturedModel&) = delete;

    void clear() {
        if (VAO) glDeleteVertexArrays(1, &VAO);
        if (VBO) glDeleteBuffers(1, &VBO);
        VAO = 0; VBO = 0; vertexCount = 0;
    }

    ~TexturedModel() { clear(); }

    bool loadFromFile(const std::string& objPath, TextureLoader& textureLoader, const std::string& texturePath = "") {
        if (VAO != 0) clear();

        std::cout << "\n[Trying to load OBJ]: " << objPath << std::endl;

        if (!std::filesystem::exists(objPath)) {
            std::cerr << "[ERROR] File does not exist: " << objPath << std::endl;
            return false;
        }

        // Загружаем OBJ
        std::vector<Vec3> positions;
        std::vector<Vec3> texCoords;
        std::vector<Vec3> normals;
        std::vector<float> finalVertices;

        struct FaceIndex {
            int v, vt, vn;
        };
        std::vector<std::vector<FaceIndex>> faces;

        std::ifstream file(objPath);
        if (!file.is_open()) {
            std::cerr << "ERROR: Could not open model file: " << objPath << "\n";
            return false;
        }

        std::string line;
        while (std::getline(file, line)) {
            std::stringstream ss(line);
            std::string prefix;
            ss >> prefix;

            if (prefix == "v") { // вершины
                float x, y, z;
                ss >> x >> y >> z;
                positions.push_back({x, y, z});
            }
            else if (prefix == "vt") { // текстурные координаты
                float u, v;
                ss >> u >> v;
                texCoords.push_back({u, 1.0f - v}); // инвертируем V
            }
            else if (prefix == "vn") { // нормали
                float x, y, z;
                ss >> x >> y >> z;
                normals.push_back({x, y, z});
            }
            else if (prefix == "f") { // грани
                std::vector<FaceIndex> faceIndices;
                std::string vertexStr;

                while (ss >> vertexStr) {
                    FaceIndex fi = {-1, -1, -1};
                    std::replace(vertexStr.begin(), vertexStr.end(), '/', ' ');
                    std::stringstream vss(vertexStr);

                    std::string v_str, vt_str, vn_str;
                    vss >> v_str;
                    if (!v_str.empty()) fi.v = std::stoi(v_str) - 1;

                    if (vss >> vt_str && !vt_str.empty()) fi.vt = std::stoi(vt_str) - 1;
                    if (vss >> vn_str && !vn_str.empty()) fi.vn = std::stoi(vn_str) - 1;

                    faceIndices.push_back(fi);
                }

                if (faceIndices.size() >= 3) {
                    faces.push_back(faceIndices);
                }
            }
        }
        file.close();

        // Теперь разбиваем полигоны на треугольники
        for (const auto& face : faces) {
            if (face.size() == 3) {
                // Уже треугольник
                for (int i = 0; i < 3; i++) {
                    const auto& fi = face[i];

                    // Позиция
                    if (fi.v >= 0 && fi.v < positions.size()) {
                        finalVertices.push_back(positions[fi.v].x);
                        finalVertices.push_back(positions[fi.v].y);
                        finalVertices.push_back(positions[fi.v].z);
                    } else {
                        finalVertices.insert(finalVertices.end(), {0.0f, 0.0f, 0.0f});
                    }

                    // Текстурные координаты
                    if (fi.vt >= 0 && fi.vt < texCoords.size()) {
                        finalVertices.push_back(texCoords[fi.vt].x);
                        finalVertices.push_back(texCoords[fi.vt].y);
                    } else {
                        finalVertices.insert(finalVertices.end(), {0.0f, 0.0f});
                    }
                }
            }
            else if (face.size() > 3) {
                // Разбиваем полигон на треугольники (триангуляция веером)
                for (int i = 1; i < face.size() - 1; i++) {
                    // Треугольник: 0, i, i+1
                    const FaceIndex* tri[3] = { &face[0], &face[i], &face[i+1] };

                    for (int j = 0; j < 3; j++) {
                        const auto& fi = *tri[j];

                        // Позиция
                        if (fi.v >= 0 && fi.v < positions.size()) {
                            finalVertices.push_back(positions[fi.v].x);
                            finalVertices.push_back(positions[fi.v].y);
                            finalVertices.push_back(positions[fi.v].z);
                        } else {
                            finalVertices.insert(finalVertices.end(), {0.0f, 0.0f, 0.0f});
                        }

                        // Текстурные координаты
                        if (fi.vt >= 0 && fi.vt < texCoords.size()) {
                            finalVertices.push_back(texCoords[fi.vt].x);
                            finalVertices.push_back(texCoords[fi.vt].y);
                        } else {
                            finalVertices.insert(finalVertices.end(), {0.0f, 0.0f});
                        }
                    }
                }
            }
        }

        std::cout << "[DEBUG] Positions loaded: " << positions.size() << std::endl;
        std::cout << "[DEBUG] TexCoords loaded: " << texCoords.size() << std::endl;
        std::cout << "[DEBUG] Faces loaded: " << faces.size() << std::endl;
        std::cout << "[DEBUG] Final vertices count: " << finalVertices.size() << std::endl;

        if (finalVertices.empty()) {
            std::cerr << "[ERROR] No vertices loaded from: " << objPath << std::endl;
            return false;
        }

        vertexCount = finalVertices.size() / 5;
        std::cout << "[DEBUG] Vertex count: " << vertexCount << " (triangles: " << vertexCount / 3 << ")" << std::endl;

        // Создаем VAO и VBO
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
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        // Загружаем текстуру
        if (!texturePath.empty()) {
            std::cout << "[DEBUG] Trying to load texture: " << texturePath << std::endl;
            if (std::filesystem::exists(texturePath)) {
                textureID = textureLoader.loadTexture(texturePath);
                hasTextureFlag = true;
                std::cout << "[SUCCESS] Model loaded with texture: " << objPath << std::endl;
            } else {
                std::cerr << "[WARNING] Texture file not found: " << texturePath << std::endl;
                hasTextureFlag = false;
                color = {0.8f, 0.8f, 0.8f};
            }
        } else {
            hasTextureFlag = false;
            color = {0.8f, 0.8f, 0.8f};
            std::cout << "[INFO] Model loaded without texture: " << objPath << std::endl;
        }

        std::cout << "[LOADED] " << objPath << " (" << vertexCount << " vertices)\n";
        return true;
    }

    void draw(GLuint shaderProgram) {
        if (VAO == 0) return;

        // Устанавливаем uniform-переменные
        GLint hasTextureLoc = glGetUniformLocation(shaderProgram, "hasTexture");
        GLint objectColorLoc = glGetUniformLocation(shaderProgram, "objectColor");

        // Устанавливаем текстурный сэмплер
        GLint texture1Loc = glGetUniformLocation(shaderProgram, "texture1");

        glUniform1i(hasTextureLoc, hasTextureFlag ? 1 : 0);
        glUniform3f(objectColorLoc, color.x, color.y, color.z);

        // Привязываем текстуру, если она есть
        if (hasTextureFlag && textureID != 0) {
            // Активируем текстурный блок 0
            glActiveTexture(GL_TEXTURE0);
            // Привязываем текстуру
            glBindTexture(GL_TEXTURE_2D, textureID);
            // Устанавливаем uniform сэмплера на использование текстурного блока 0
            glUniform1i(texture1Loc, 0);
        } else {
            // Если текстуры нет, устанавливаем цвет
            glUniform1i(hasTextureLoc, 0);
        }

        // Отрисовываем
        glBindVertexArray(VAO);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }
};

// --- Класс камеры ---
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
    Camera(Vec3 pos = { 0.0f, 5.0f, 15.0f }, float initialYaw = -1.57f, float initialPitch = -0.2f) :
        position(pos), up({ 0.0f, 1.0f, 0.0f }), yaw(initialYaw), pitch(initialPitch) {
        updateCameraVectors();
    }

    Mat4 getViewMatrix() const {
        return Mat4::LookAt(position, position + front, up);
    }

    void processKeyboard(float direction, float velocity) {
        if (direction == 0) position = position + front * velocity;      // W - вперед
        if (direction == 1) position = position - front * velocity;      // S - назад
        if (direction == 2) position = position - right * velocity;      // A - влево
        if (direction == 3) position = position + right * velocity;      // D - вправо
        if (direction == 4) position.y += velocity;                      // Space - вверх
        if (direction == 5) position.y -= velocity;                      // C - вниз
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

    Vec3 getPosition() const { return position; }
};

// --- Создание шейдерной программы ---
GLuint createShaderProgram() {
    GLuint vertexShader = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vertexShader, 1, &vertexShaderSource, NULL);
    glCompileShader(vertexShader);

    // Проверка компиляции вершинного шейдера
    GLint success;
    glGetShaderiv(vertexShader, GL_COMPILE_STATUS, &success);
    if (!success) {
        char infoLog[512];
        glGetShaderInfoLog(vertexShader, 512, NULL, infoLog);
        std::cerr << "ERROR::SHADER::VERTEX::COMPILATION_FAILED\n" << infoLog << std::endl;
    }

    GLuint fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fragmentShader, 1, &fragmentShaderSource, NULL);
    glCompileShader(fragmentShader);

    // Проверка компиляции фрагментного шейдера
    glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, &success);
    if (!success) {
        char infoLog[512];
        glGetShaderInfoLog(fragmentShader, 512, NULL, infoLog);
        std::cerr << "ERROR::SHADER::FRAGMENT::COMPILATION_FAILED\n" << infoLog << std::endl;
    }

    // Создание шейдерной программы
    GLuint shaderProgram = glCreateProgram();
    glAttachShader(shaderProgram, vertexShader);
    glAttachShader(shaderProgram, fragmentShader);
    glLinkProgram(shaderProgram);

    // Проверка линковки
    glGetProgramiv(shaderProgram, GL_LINK_STATUS, &success);
    if (!success) {
        char infoLog[512];
        glGetProgramInfoLog(shaderProgram, 512, NULL, infoLog);
        std::cerr << "ERROR::SHADER::PROGRAM::LINKING_FAILED\n" << infoLog << std::endl;
    }

    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);

    return shaderProgram;
}

// --- Структура для объекта сцены ---
struct SceneObject {
    std::shared_ptr<TexturedModel> model;
    std::string name;
    Vec3 position;
    Vec3 rotation;
    Vec3 scale;
    float rotationSpeed;
};

int main() {
    // Настройки окна
    sf::ContextSettings settings;
    settings.depthBits = 24;
    settings.stencilBits = 8;
    settings.antiAliasingLevel = 4;
    settings.majorVersion = 3;
    settings.minorVersion = 3;
    settings.attributeFlags = sf::ContextSettings::Core;

    sf::Window window(sf::VideoMode({1200, 800}), "Lab 14: 3D Models Showcase", sf::State::Windowed, settings);
    window.setVerticalSyncEnabled(true);
    window.setActive(true);

    // Инициализация GLEW
    glewExperimental = GL_TRUE;
    GLenum err = glewInit();
    if (err != GLEW_OK) {
        std::cerr << "Failed to initialize GLEW: " << glewGetErrorString(err) << std::endl;
        return -1;
    }

    // Настройки OpenGL
    glEnable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);
    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);
    glFrontFace(GL_CCW);

    // Создаем шейдерную программу
    GLuint shaderProgram = createShaderProgram();
    glUseProgram(shaderProgram);

    // Получаем uniform-локации
    GLint modelLoc = glGetUniformLocation(shaderProgram, "model");
    GLint viewLoc = glGetUniformLocation(shaderProgram, "view");
    GLint projLoc = glGetUniformLocation(shaderProgram, "projection");

    // Загрузчик текстур
    TextureLoader textureLoader;

    // Создаем объекты сцены (5 моделей из папки 3d_models)
        // Создаем объекты сцены (5 моделей из папки 3d_models)
    std::vector<SceneObject> objects;

    // 1. Забор (Fence) - слева в глубине
    auto fenceModel = std::make_shared<TexturedModel>();
    if (fenceModel->loadFromFile("3d_models/Free_Fence9OBJ/objFence.obj",
                                 textureLoader,
                                 "3d_models/Free_Fence9OBJ/textures/germany010.jpg")) {
        objects.push_back({
            fenceModel,
            "Fence",
            { -10.0f, 0.0f, -8.0f },  // Левый дальний угол
            { 0.0f, 0.5f, 0.0f },     // Немного повернут
            { 5.3f, 5.3f, 5.3f },     // Масштаб
            0.2f                      // Очень медленное вращение
        });
    }

    // 2. Кофейный столик (Coffee Table) - центр слева
    auto coffeeTableModel = std::make_shared<TexturedModel>();
    if (coffeeTableModel->loadFromFile("3d_models/coffee_table/coffee_table.obj",
                                       textureLoader,
                                       "3d_models/coffee_table/textures/coffee_table_Albedo.png")) {
        objects.push_back({
            coffeeTableModel,
            "Coffee Table",
            { -6.0f, 0.0f, -2.0f },   // Центр-слева, ближе к камере
            { 0.0f, 0.8f, 0.0f },     // Повернут для лучшего обзора
            { 0.08f, 0.08f, 0.08f },  // Увеличенный масштаб
            0.0f                       // Не вращается
        });
    }

    // 3. Мяч для гольфа (Golf Ball) - в центре сцены на возвышении
    auto golfBallModel = std::make_shared<TexturedModel>();
    if (golfBallModel->loadFromFile("3d_models/golf_ball/golf_ball.obj",
                                    textureLoader,
                                    "3d_models/golf_ball/textures/golf_ball_Albedo.png")) {
        objects.push_back({
            golfBallModel,
            "Golf Ball",
            { 0.0f, 1.5f, 0.0f },     // Центр, на возвышении
            { 0.0f, 0.0f, 0.0f },
            { 0.08f, 0.08f, 0.08f },     // Увеличенный мяч
            1.5f                      // Быстрое вращение (как катящийся мяч)
        });
    }

    // 4. Старый табурет (Old Stool) - центр справа
    auto stoolModel = std::make_shared<TexturedModel>();
    if (stoolModel->loadFromFile("3d_models/old_stool/old_stool.obj",
                                 textureLoader,
                                 "3d_models/old_stool/textures/old_stool_Albedo.png")) {
        objects.push_back({
            stoolModel,
            "Old Stool",
            { 5.0f, 0.0f, -1.0f },    // Правее центра, ближе к камере
            { 0.0f, -0.5f, 0.0f },    // Повернут в другую сторону
            { 0.06f, 0.06f, 0.06f },  // Увеличенный масштаб
            0.0f                       // Не вращается
        });
    }

    // 5. Банка с газировкой (Soda Can) - справа в глубине на столе/возвышении
    auto sodaCanModel = std::make_shared<TexturedModel>();
    if (sodaCanModel->loadFromFile("3d_models/soda_can_obj/soda_can.obj",
                                   textureLoader,
                                   "3d_models/soda_can_obj/textures/soda_can_color.png")) {
        objects.push_back({
            sodaCanModel,
            "Soda Can",
            { 5.0f, 4.27f, -2.0f },    // Правый дальний угол, на высоте
            { 0.3f, 0.0f, 0.0f },     // Наклонена для реалистичности
            { 13.0f, 13.0f, 13.0f },     // Увеличенный масштаб (но не слишком)
            0.8f                      // Медленное вращение
        });
    }

    std::cout << "\n========================================\n";
    std::cout << "     LAB 14: 3D MODELS SHOWCASE         \n";
    std::cout << "========================================\n";
    std::cout << "Loaded " << objects.size() << " models\n";
    std::cout << "Controls:\n";
    std::cout << "  W/S/A/D - Move camera\n";
    std::cout << "  Space/C - Move up/down\n";
    std::cout << "  Mouse - Look around\n";
    std::cout << "  TAB - Toggle mouse capture\n";
    std::cout << "  ESC - Exit\n";
    std::cout << "========================================\n";

    // Камера
    Camera camera({ 0.0f, 5.0f, 15.0f });
    float lastX = 600.0f;
    float lastY = 400.0f;
    bool firstMouse = true;
    const float CAMERA_SPEED = 5.0f;
    bool mouseCaptured = true;
    window.setMouseCursorVisible(!mouseCaptured);

    sf::Clock clock;
    float totalTime = 0.0f;

    // Главный цикл
    while (window.isOpen()) {
        float deltaTime = clock.restart().asSeconds();
        totalTime += deltaTime;
        float cameraVelocity = CAMERA_SPEED * deltaTime;

        // Обработка событий
        while (const auto event = window.pollEvent()) {
            if (event->is<sf::Event::Closed>()) {
                window.close();
            }

            if (const auto* k = event->getIf<sf::Event::KeyPressed>()) {
                if (k->scancode == sf::Keyboard::Scancode::Escape) {
                    window.close();
                }

                if (k->scancode == sf::Keyboard::Scancode::Tab) {
                    mouseCaptured = !mouseCaptured;
                    window.setMouseCursorVisible(!mouseCaptured);
                    if (mouseCaptured) {
                        sf::Mouse::setPosition(sf::Vector2<int>(600, 400), window);
                        lastX = 600.0f;
                        lastY = 400.0f;
                        firstMouse = false;
                    }
                }

                // Управление камерой
                if (k->scancode == sf::Keyboard::Scancode::W) camera.processKeyboard(0, cameraVelocity);
                if (k->scancode == sf::Keyboard::Scancode::S) camera.processKeyboard(1, cameraVelocity);
                if (k->scancode == sf::Keyboard::Scancode::A) camera.processKeyboard(2, cameraVelocity);
                if (k->scancode == sf::Keyboard::Scancode::D) camera.processKeyboard(3, cameraVelocity);
                if (k->scancode == sf::Keyboard::Scancode::Space) camera.processKeyboard(4, cameraVelocity);
                if (k->scancode == sf::Keyboard::Scancode::C) camera.processKeyboard(5, cameraVelocity);
            }

            if (const auto* m = event->getIf<sf::Event::MouseMoved>()) {
                if (mouseCaptured) {
                    if (firstMouse) {
                        lastX = m->position.x;
                        lastY = m->position.y;
                        firstMouse = false;
                    }

                    float xOffset = m->position.x - lastX;
                    float yOffset = lastY - m->position.y;

                    lastX = m->position.x;
                    lastY = m->position.y;

                    camera.processMouseMovement(xOffset, yOffset);

                    // Центрируем курсор
                    if (mouseCaptured) {
                        sf::Mouse::setPosition(sf::Vector2i(600, 400), window);
                        lastX = 600.0f;
                        lastY = 400.0f;
                    }
                }
            }

            if (const auto* r = event->getIf<sf::Event::Resized>()) {
                glViewport(0, 0, r->size.x, r->size.y);
            }
        }

        // Очистка экрана
        glClearColor(0.1f, 0.1f, 0.12f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Используем шейдерную программу
        glUseProgram(shaderProgram);

        // Устанавливаем матрицы проекции и вида
        sf::Vector2u windowSize = window.getSize();
        Mat4 projection = Mat4::Perspective(1.0f,
                                           static_cast<float>(windowSize.x) / windowSize.y,
                                           0.1f, 100.0f);
        Mat4 view = camera.getViewMatrix();

        glUniformMatrix4fv(projLoc, 1, GL_FALSE, projection.data());
        glUniformMatrix4fv(viewLoc, 1, GL_FALSE, view.data());

        // Отрисовываем все объекты
        // Отрисовываем все объекты
        for (auto& obj : objects) {
            // Обновляем вращение - ЗАКОММЕНТИРУЙТЕ эту строку:
            // obj.rotation.y += obj.rotationSpeed * deltaTime;

            // Создаем матрицу модели БЕЗ вращения:
            Mat4 model = Mat4::Translate(obj.position.x, obj.position.y, obj.position.z);
            // УБЕРИТЕ все строки с Rotate, оставьте только Translate и Scale:
            model = model * Mat4::Scale(obj.scale.x, obj.scale.y, obj.scale.z);

            // Устанавливаем матрицу модели
            glUniformMatrix4fv(modelLoc, 1, GL_FALSE, model.data());

            // Отрисовываем модель
            obj.model->draw(shaderProgram);

            // Отладочный вывод для проверки видимости (однократно)
            static bool printed = false;
            if (!printed) {
                std::cout << "[DEBUG] Model: " << obj.name
                          << " Pos: (" << obj.position.x << ", " << obj.position.y << ", " << obj.position.z << ")"
                          << " Scale: (" << obj.scale.x << ", " << obj.scale.y << ", " << obj.scale.z << ")" << std::endl;
            }
        }

        // Отображаем результат
        window.display();
    }

    // Очистка
    glDeleteProgram(shaderProgram);

    return 0;
}