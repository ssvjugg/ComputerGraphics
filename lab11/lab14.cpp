#include <glad/glad.h>
#include <GLFW/glfw3.h>

#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>

#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"

#include <iostream>
#include <vector>
#include <string>
#include <fstream>
#include <sstream>

// --- Шейдеры ---
const char* vertexShaderSource = R"(
#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aTexCoords;
layout (location = 2) in vec3 aNormal;

out vec3 FragPos;
out vec3 Normal;
out vec2 TexCoords;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main()
{
    FragPos = vec3(model * vec4(aPos, 1.0));
    Normal = mat3(transpose(inverse(model))) * aNormal;
    TexCoords = aTexCoords;
    gl_Position = projection * view * vec4(FragPos, 1.0);
}
)";

const char* fragmentShaderSource = R"(
#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;

uniform sampler2D diffuseTexture;
uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;

void main()
{
    // Ambient
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // Diffuse
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // Specular
    float specularStrength = 0.5;
    vec3 viewDir = normalize(viewPos - FragPos);
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32);
    vec3 specular = specularStrength * spec * lightColor;

    vec3 result = (ambient + diffuse + specular);
    vec4 texColor = texture(diffuseTexture, TexCoords);

    FragColor = vec4(result, 1.0) * texColor;
}
)";

// --- Структуры ---
struct Vertex {
    glm::vec3 Position;
    glm::vec2 TexCoords;
    glm::vec3 Normal;
};

// --- Класс Shader ---
class Shader {
public:
    unsigned int ID;
    Shader(const char* vShaderCode, const char* fShaderCode) {
        unsigned int vertex, fragment;
        int success;
        char infoLog[512];

        vertex = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertex, 1, &vShaderCode, NULL);
        glCompileShader(vertex);
        glGetShaderiv(vertex, GL_COMPILE_STATUS, &success);
        if(!success) {
            glGetShaderInfoLog(vertex, 512, NULL, infoLog);
            std::cout << "ERROR::VERTEX::COMPILATION_FAILED\n" << infoLog << std::endl;
        }

        fragment = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragment, 1, &fShaderCode, NULL);
        glCompileShader(fragment);
        glGetShaderiv(fragment, GL_COMPILE_STATUS, &success);
        if(!success) {
            glGetShaderInfoLog(fragment, 512, NULL, infoLog);
            std::cout << "ERROR::FRAGMENT::COMPILATION_FAILED\n" << infoLog << std::endl;
        }

        ID = glCreateProgram();
        glAttachShader(ID, vertex);
        glAttachShader(ID, fragment);
        glLinkProgram(ID);
        glGetProgramiv(ID, GL_LINK_STATUS, &success);
        if(!success) {
            glGetProgramInfoLog(ID, 512, NULL, infoLog);
            std::cout << "ERROR::PROGRAM::LINKING_FAILED\n" << infoLog << std::endl;
        }
        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }

    void use() { glUseProgram(ID); }
    void setMat4(const std::string &name, const glm::mat4 &mat) const {
        glUniformMatrix4fv(glGetUniformLocation(ID, name.c_str()), 1, GL_FALSE, &mat[0][0]);
    }
    void setVec3(const std::string &name, const glm::vec3 &value) const {
        glUniform3fv(glGetUniformLocation(ID, name.c_str()), 1, &value[0]);
    }
};

// --- Model Loader ---
class Model {
public:
    unsigned int VAO, VBO;
    std::vector<Vertex> vertices;
    unsigned int textureID;

    Model(const char* objPath, const char* texPath) {
        loadOBJ(objPath);
        loadTexture(texPath);
        setupMesh();
    }

    void Draw(Shader &shader, glm::mat4 modelMatrix) {
        shader.setMat4("model", modelMatrix);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureID);
        glBindVertexArray(VAO);
        glDrawArrays(GL_TRIANGLES, 0, vertices.size());
        glBindVertexArray(0);
    }

private:
    void loadOBJ(const char* path) {
        std::vector<glm::vec3> temp_positions;
        std::vector<glm::vec2> temp_uvs;
        std::vector<glm::vec3> temp_normals;

        std::ifstream file(path);
        if (!file.is_open()) {
            std::cerr << "!!! FAILED TO OPEN OBJ: " << path << " !!!" << std::endl;
            return;
        }

        std::string line;
        while (std::getline(file, line)) {
            std::stringstream ss(line);
            std::string prefix;
            ss >> prefix;

            if (prefix == "v") {
                glm::vec3 temp;
                ss >> temp.x >> temp.y >> temp.z;
                temp_positions.push_back(temp);
            } else if (prefix == "vt") {
                glm::vec2 temp;
                ss >> temp.x >> temp.y;
                temp_uvs.push_back(temp);
            } else if (prefix == "vn") {
                glm::vec3 temp;
                ss >> temp.x >> temp.y >> temp.z;
                temp_normals.push_back(temp);
            } else if (prefix == "f") {
                std::string vertexStr;
                while (ss >> vertexStr) {
                    Vertex vertex;
                    std::stringstream vss(vertexStr);
                    std::string segment;
                    std::vector<std::string> indices;
                    while(std::getline(vss, segment, '/')) indices.push_back(segment);

                    if (!indices.empty() && !temp_positions.empty()) {
                        int posIdx = stoi(indices[0]) - 1;
                        if(posIdx >= 0 && posIdx < temp_positions.size())
                            vertex.Position = temp_positions[posIdx];

                        if (indices.size() > 1 && !indices[1].empty() && !temp_uvs.empty()) {
                            int uvIdx = stoi(indices[1]) - 1;
                            if(uvIdx >= 0 && uvIdx < temp_uvs.size())
                                vertex.TexCoords = temp_uvs[uvIdx];
                        } else vertex.TexCoords = glm::vec2(0.0f);

                        if (indices.size() > 2 && !indices[2].empty() && !temp_normals.empty()) {
                            int normIdx = stoi(indices[2]) - 1;
                            if(normIdx >= 0 && normIdx < temp_normals.size())
                                vertex.Normal = temp_normals[normIdx];
                        } else vertex.Normal = glm::vec3(0.0f, 1.0f, 0.0f);

                        vertices.push_back(vertex);
                    }
                }
            }
        }
        std::cout << "Loaded " << path << ": " << vertices.size() << " vertices." << std::endl;
    }

    void loadTexture(const char* path) {
        glGenTextures(1, &textureID);
        int width, height, nrComponents;
        stbi_set_flip_vertically_on_load(true);
        unsigned char *data = stbi_load(path, &width, &height, &nrComponents, 0);
        if (data) {
            GLenum format = (nrComponents == 4) ? GL_RGBA : GL_RGB;
            glBindTexture(GL_TEXTURE_2D, textureID);
            glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, format, GL_UNSIGNED_BYTE, data);
            glGenerateMipmap(GL_TEXTURE_2D);

            // Важные параметры для Mac
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            std::cout << "Texture failed to load: " << path << std::endl;
        }
        stbi_image_free(data);
    }

    void setupMesh() {
        glGenVertexArrays(1, &VAO);
        glGenBuffers(1, &VBO);
        glBindVertexArray(VAO);
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, vertices.size() * sizeof(Vertex), vertices.data(), GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, sizeof(Vertex), (void*)0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, sizeof(Vertex), (void*)offsetof(Vertex, TexCoords));
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 3, GL_FLOAT, GL_FALSE, sizeof(Vertex), (void*)offsetof(Vertex, Normal));
        glBindVertexArray(0);
    }
};

int main() {
    glfwInit();
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

    GLFWwindow* window = glfwCreateWindow(1024, 768, "OpenGL Scene", NULL, NULL);
    if (!window) { glfwTerminate(); return -1; }
    glfwMakeContextCurrent(window);

    if (!gladLoadGLLoader((GLADloadproc)glfwGetProcAddress)) {
        return -1;
    }
    glEnable(GL_DEPTH_TEST);

    Shader lightingShader(vertexShaderSource, fragmentShaderSource);

    std::cout << "--- LOADING STATUS ---" << std::endl;
    Model cube("cube.obj", "texture.jpg");
    Model donut("donut.obj", "texture2.jpg");

    // МЕРСЕДЕС
    Model mers("mers.obj", "texture2.jpg");
    if(mers.vertices.empty()) std::cout << "CRITICAL WARNING: Mercedes (mers.obj) failed to load!" << std::endl;

    // ЧАЙНИК
    Model chai("chai.obj", "texture.jpg");
    if(chai.vertices.empty()) std::cout << "CRITICAL WARNING: Teapot (chai.obj) failed to load!" << std::endl;

    Model pyramid("pyramid.obj", "texture2.jpg");
    std::cout << "----------------------" << std::endl;

    // Отодвинем камеру ПОДАЛЬШЕ (было z=8.0, стало z=12.0)
    glm::vec3 lightPos(2.0f, 4.0f, 2.0f);
    glm::vec3 cameraPos(0.0f, 5.0f, 12.0f);

    while (!glfwWindowShouldClose(window)) {
        if(glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS)
            glfwSetWindowShouldClose(window, true);

        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        lightingShader.use();
        lightingShader.setVec3("lightColor", glm::vec3(1.0f, 1.0f, 1.0f));
        lightingShader.setVec3("lightPos", lightPos);
        lightingShader.setVec3("viewPos", cameraPos);

        glm::mat4 projection = glm::perspective(glm::radians(45.0f), 1024.0f / 768.0f, 0.1f, 100.0f);
        glm::mat4 view = glm::lookAt(cameraPos, glm::vec3(0.0f, 0.0f, 0.0f), glm::vec3(0.0f, 1.0f, 0.0f));

        lightingShader.setMat4("projection", projection);
        lightingShader.setMat4("view", view);

        // --- 1. КУБ (СТОЛ) ---
        glm::mat4 model = glm::mat4(1.0f);
        model = glm::translate(model, glm::vec3(0.0f, -1.0f, 0.0f));
        model = glm::scale(model, glm::vec3(4.0f, 0.2f, 3.0f));
        cube.Draw(lightingShader, model);


            // Запасной вариант - куб (шейдер тот же, текстура другая)
             model = glm::mat4(1.0f);
             model = glm::translate(model, glm::vec3(2.3f, 0.0f, 0.0f));
             cube.Draw(lightingShader, model);

        // --- 3. ПОНЧИК (ЦЕНТР над столом) ---
        model = glm::mat4(1.0f);
        model = glm::translate(model, glm::vec3(0.0f, 2.0f, 0.0f));
        model = glm::rotate(model, (float)glfwGetTime(), glm::vec3(0.0f, 1.0f, 0.0f));
        model = glm::scale(model, glm::vec3(0.3f));
        donut.Draw(lightingShader, model);

        // --- 4. МЕРСЕДЕС (СПРАВА) ---
        // Если мерс есть
        if (!mers.vertices.empty()) {
            model = glm::mat4(1.0f);
            model = glm::translate(model, glm::vec3(2.5f, -1.0f, 1.0f));
            model = glm::rotate(model, glm::radians(-45.0f), glm::vec3(0.0f, 1.0f, 0.0f));

            // ВАЖНО: Увеличиваем масштаб. Попробуй 0.05f.
            model = glm::scale(model, glm::vec3(0.9f));
            mers.Draw(lightingShader, model);
        }

        // --- 5. ПИРАМИДА (СЗАДИ) ---
        model = glm::mat4(1.0f);
        model = glm::translate(model, glm::vec3(-2.0f, 0.0f, -2.0f)); // Подняли чуть выше
        model = glm::scale(model, glm::vec3(1.0f));
        pyramid.Draw(lightingShader, model);

        glfwSwapBuffers(window);
        glfwPollEvents();
    }

    glfwTerminate();
    return 0;
}