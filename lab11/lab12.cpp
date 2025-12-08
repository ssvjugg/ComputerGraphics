#include <GL/glew.h>
#include <SFML/Window.hpp>
#include <SFML/Graphics.hpp>
#include <iostream>
#include <vector>
#include <optional>

#define GL_SILENCE_DEPRECATION
const GLdouble PI = std::acos(-1.0);

const GLuint WINDOW_WIDTH = 800;
const GLuint WINDOW_HEIGHT = 600;
const GLfloat MOVE_SPEED = 0.05f;
const GLfloat COLOR_CHANGE_SPEED = 0.01f;

enum class ActiveShape {
	TETRAHEDRON,
	CUBE
};

struct Shader {
	GLuint programID;
};

struct Vec3 {
	GLfloat x, y, z;
};

struct Cube {
	GLuint VAO, VBO, EBO;
	GLuint textureID;
	GLint indexCount = 36;
	GLfloat colorMixFactor = 0.0f;
};

struct Tetrahedron {
	GLuint VAO, VBO, EBO;
	GLint indexCount = 12;
	Vec3 position = { 0.0f, 0.0f, 0.0f };
};

struct Matrix4 {
	GLfloat m[16];
};

Matrix4 Identity() {
	Matrix4 result = { 0 };
	result.m[0] = result.m[5] = result.m[10] = result.m[15] = 1.0f;
	return result;
}

Matrix4 Multiply(const Matrix4& A, const Matrix4& B) {
	Matrix4 C = { 0 };
	for (int i = 0; i < 4; ++i) {
		for (int j = 0; j < 4; ++j) {
			for (int k = 0; k < 4; ++k) {
				C.m[i + j * 4] += A.m[i + k * 4] * B.m[k + j * 4];
			}
		}
	}
	return C;
}

Matrix4 Translate(const Matrix4& M, const Vec3& v) {
	Matrix4 T = Identity();
	T.m[12] = v.x;
	T.m[13] = v.y;
	T.m[14] = v.z;
	return Multiply(M, T);
}

Matrix4 Rotate(const Matrix4& M, GLfloat angleRad, const Vec3& axis) {
	Matrix4 R = Identity();
	GLfloat c = cosf(angleRad);
	GLfloat s = sinf(angleRad);
	GLfloat omc = 1.0f - c;

	R.m[0] = axis.x * axis.x * omc + c;
	R.m[1] = axis.y * axis.x * omc + axis.z * s;
	R.m[2] = axis.z * axis.x * omc - axis.y * s;

	R.m[4] = axis.x * axis.y * omc - axis.z * s;
	R.m[5] = axis.y * axis.y * omc + c;
	R.m[6] = axis.z * axis.y * omc + axis.x * s;

	R.m[8] = axis.x * axis.z * omc + axis.y * s;
	R.m[9] = axis.y * axis.z * omc - axis.x * s;
	R.m[10] = axis.z * axis.z * omc + c;

	return Multiply(M, R);
}

Matrix4 Perspective(GLfloat fovDeg, GLfloat aspectRatio, GLfloat nearPlane, GLfloat farPlane) {
	Matrix4 P = { 0 };
	GLfloat fovRad = fovDeg * PI / 180.0f;
	GLfloat tanHalfFov = tanf(fovRad / 2.0f);
	GLfloat f = 1.0f / tanHalfFov;
	GLfloat depth = farPlane - nearPlane;

	P.m[0] = f / aspectRatio;
	P.m[5] = f;
	P.m[10] = -(farPlane + nearPlane) / depth;
	P.m[11] = -1.0f;
	P.m[14] = -(2.0f * farPlane * nearPlane) / depth;

	return P;
}

Matrix4 LookAt(const Vec3& position, const Vec3& target, const Vec3& up) {
	Vec3 zAxis = { target.x - position.x, target.y - position.y, target.z - position.z };
	GLfloat lenZ = sqrtf(zAxis.x * zAxis.x + zAxis.y * zAxis.y + zAxis.z * zAxis.z);
	zAxis.x /= -lenZ;
	zAxis.y /= -lenZ;
	zAxis.z /= -lenZ;


	Vec3 xAxis = { up.y * zAxis.z - up.z * zAxis.y,
				  up.z * zAxis.x - up.x * zAxis.z,
				  up.x * zAxis.y - up.y * zAxis.x };
	GLfloat lenX = sqrtf(xAxis.x * xAxis.x + xAxis.y * xAxis.y + xAxis.z * xAxis.z);
	xAxis.x /= lenX;
	xAxis.y /= lenX;
	xAxis.z /= lenX;

	Vec3 yAxis = { zAxis.y * xAxis.z - zAxis.z * xAxis.y,
				  zAxis.z * xAxis.x - zAxis.x * xAxis.z,
				  zAxis.x * xAxis.y - zAxis.y * xAxis.x };

	Matrix4 view = Identity();
	view.m[0] = xAxis.x; view.m[4] = xAxis.y; view.m[8] = xAxis.z; view.m[12] = -(xAxis.x * position.x + xAxis.y * position.y + xAxis.z * position.z);
	view.m[1] = yAxis.x; view.m[5] = yAxis.y; view.m[9] = yAxis.z; view.m[13] = -(yAxis.x * position.x + yAxis.y * position.y + yAxis.z * position.z);
	view.m[2] = zAxis.x; view.m[6] = zAxis.y; view.m[10] = zAxis.z; view.m[14] = -(zAxis.x * position.x + zAxis.y * position.y + zAxis.z * position.z);
	view.m[3] = 0.0f; view.m[7] = 0.0f; view.m[11] = 0.0f; view.m[15] = 1.0f;

	return view;
}

const char* tetraVertexShaderSource = R"(
#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aColor;

out vec3 vColor;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main()
{
    vColor = aColor;
    gl_Position = projection * view * model * vec4(aPos, 1.0);
}
)";

const char* tetraFragmentShaderSource = R"(
#version 330 core
in vec3 vColor;
out vec4 FragColor;

void main()
{
    FragColor = vec4(vColor, 1.0);
}
)";

const char* cubeVertexShaderSource = R"(
#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aColor;
layout (location = 2) in vec2 aTexCoord;

out vec2 vTexCoord;
out vec3 vColor;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main()
{
    vTexCoord = aTexCoord;
	vColor = aColor;
    gl_Position = projection * view * model * vec4(aPos, 1.0);
}
)";

const char* cubeFragmentShaderSource = R"(
#version 330 core
in vec2 vTexCoord;
in vec3 vColor;
out vec4 FragColor;

uniform sampler2D ourTexture;
uniform float colorMixFactor;

void main()
{
    vec4 textureColor = texture(ourTexture, vTexCoord);    
    FragColor = mix(textureColor, vec4(vColor, 1.0), colorMixFactor);
}
)";

GLuint compileShaders(const char* vShaderSource, const char* fShaderSource)
{
	GLuint vertexShader = glCreateShader(GL_VERTEX_SHADER);
	glShaderSource(vertexShader, 1, &vShaderSource, NULL);
	glCompileShader(vertexShader);

	GLint success;
	glGetShaderiv(vertexShader, GL_COMPILE_STATUS, &success);
	if (!success) std::cerr << "Vertex Shader Error" << std::endl;

	GLuint fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
	glShaderSource(fragmentShader, 1, &fShaderSource, NULL);
	glCompileShader(fragmentShader);

	glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, &success);
	if (!success) std::cerr << "Vertex Shader Error" << std::endl;

	GLuint shaderProgram = glCreateProgram();
	glAttachShader(shaderProgram, vertexShader);
	glAttachShader(shaderProgram, fragmentShader);
	glLinkProgram(shaderProgram);

	glDeleteShader(vertexShader);
	glDeleteShader(fragmentShader);

	return shaderProgram;
}

GLuint loadTexture(const std::string& filename)
{
	sf::Image image;
	if (!image.loadFromFile(filename)) {
		std::cerr << "Ошибка: Не удалось загрузить текстуру " << filename << std::endl;
		return 0;
	}	
	GLuint textureID;
	glGenTextures(1, &textureID);
	glBindTexture(GL_TEXTURE_2D, textureID);

	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR_MIPMAP_LINEAR);

	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getSize().x, image.getSize().y,
		0, GL_RGBA, GL_UNSIGNED_BYTE, image.getPixelsPtr());

	glGenerateMipmap(GL_TEXTURE_2D);

	return textureID;
}

void setupTetrahedron(Tetrahedron& tetra)
{
	GLfloat tetraVertices[] = {
		 0.0f,  1.0f,  0.0f,   1.0f, 0.0f, 0.0f,
		-1.0f, -1.0f, -1.0f,   0.0f, 1.0f, 0.0f,
		 1.0f, -1.0f, -1.0f,   0.0f, 0.0f, 1.0f,
		 0.0f, -1.0f,  1.0f,   1.0f, 1.0f, 0.0f
	};
	GLint tetraIndices[] = {
		0, 1, 2, 0, 2, 3, 0, 3, 1, 1, 3, 2
	};

	glGenVertexArrays(1, &tetra.VAO);
	glGenBuffers(1, &tetra.VBO);
	glGenBuffers(1, &tetra.EBO);

	glBindVertexArray(tetra.VAO);

	glBindBuffer(GL_ARRAY_BUFFER, tetra.VBO);
	glBufferData(GL_ARRAY_BUFFER, sizeof(tetraVertices), tetraVertices, GL_STATIC_DRAW);

	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, tetra.EBO);
	glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(tetraIndices), tetraIndices, GL_STATIC_DRAW);

	glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 6 * sizeof(float), (void*)0);
	glEnableVertexAttribArray(0);

	glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 6 * sizeof(float), (void*)(3 * sizeof(float)));
	glEnableVertexAttribArray(1);

	glBindVertexArray(0);
}

void setupCube(Cube& cube)
{
	GLfloat cubeVertices[] = {				
		-0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 0.0f,  0.0f, 0.0f,
		 0.5f, -0.5f,  0.5f,  1.0f, 0.5f, 0.5f,  1.0f, 0.0f,
		 0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 1.0f,  1.0f, 1.0f,
		-0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 1.0f,  0.0f, 1.0f,
		
		-0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.0f,  0.0f, 0.0f,
		 0.5f, -0.5f, -0.5f,  0.5f, 1.0f, 0.5f,  1.0f, 0.0f,
		 0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 1.0f,  1.0f, 1.0f,
		-0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 1.0f,  0.0f, 1.0f,
		
		-0.5f,  0.5f,  0.5f,  0.0f, 0.0f, 1.0f,  0.0f, 1.0f,
		-0.5f,  0.5f, -0.5f,  0.5f, 0.5f, 1.0f,  1.0f, 1.0f,
		-0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 1.0f,  1.0f, 0.0f,
		-0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 0.0f,  0.0f, 0.0f,
		
		 0.5f,  0.5f,  0.5f,  0.0f, 1.0f, 0.0f,  0.0f, 1.0f,
		 0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 1.0f,  1.0f, 1.0f,
		 0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 0.0f,  1.0f, 0.0f,
		 0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 1.0f,  0.0f, 0.0f,
		 
		 -0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 0.0f,  0.0f, 0.0f,
		  0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.0f,  1.0f, 0.0f,
		  0.5f, -0.5f,  0.5f,  0.0f, 0.0f, 1.0f,  1.0f, 1.0f,
		 -0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 1.0f,  0.0f, 1.0f,
		 
		 -0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 1.0f,  0.0f, 0.0f,
		  0.5f,  0.5f, -0.5f,  0.0f, 1.0f, 0.0f,  1.0f, 0.0f,
		  0.5f,  0.5f,  0.5f,  1.0f, 1.0f, 1.0f,  1.0f, 1.0f,
		 -0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 0.0f,  0.0f, 1.0f
	};

	GLuint cubeIndices[] = {
		0, 1, 2, 2, 3, 0,		
		4, 5, 6, 6, 7, 4,		
		8, 9, 10, 10, 11, 8,		
		12, 13, 14, 14, 15, 12,		
		16, 17, 18, 18, 19, 16,		
		20, 21, 22, 22, 23, 20
	};

	glGenVertexArrays(1, &cube.VAO);
	glGenBuffers(1, &cube.VBO);
	glGenBuffers(1, &cube.EBO);

	glBindVertexArray(cube.VAO);

	glBindBuffer(GL_ARRAY_BUFFER, cube.VBO);
	glBufferData(GL_ARRAY_BUFFER, sizeof(cubeVertices), cubeVertices, GL_STATIC_DRAW);

	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cube.EBO);
	glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(cubeIndices), cubeIndices, GL_STATIC_DRAW);

	const GLsizei stride = 8 * sizeof(GLfloat);

	glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, stride, (void*)0);
	glEnableVertexAttribArray(0);

	glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, stride, (void*)(3 * sizeof(GLfloat)));
	glEnableVertexAttribArray(1);

	glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, stride, (void*)(6 * sizeof(GLfloat)));
	glEnableVertexAttribArray(2);

	glBindVertexArray(0);

	cube.textureID = loadTexture("texture.jpg");
}

void renderTetrahedron(const Tetrahedron& tetra, const Shader& shader, const Matrix4& view, const Matrix4& projection)
{
	glUseProgram(shader.programID);

	Matrix4 model = Identity();
	model = Rotate(model, 50.0f * PI / 180.0f, { 1.0f, 0.0f, 0.0f });
	model = Rotate(model, 45.0f * PI / 180.0f, { 0.0f, 1.0f, 0.0f });

	model = Translate(model, tetra.position);
	
	glUniformMatrix4fv(glGetUniformLocation(shader.programID, "model"), 1, GL_FALSE, model.m);
	glUniformMatrix4fv(glGetUniformLocation(shader.programID, "view"), 1, GL_FALSE, view.m);
	glUniformMatrix4fv(glGetUniformLocation(shader.programID, "projection"), 1, GL_FALSE, projection.m);

	glBindVertexArray(tetra.VAO);
	glDrawElements(GL_TRIANGLES, tetra.indexCount, GL_UNSIGNED_INT, 0);
}

void renderCube(const Cube& cube, const Shader& shader, const Matrix4& view, const Matrix4& projection)
{
	glUseProgram(shader.programID);

	Matrix4 model = Identity();
	
	GLfloat rotAngle = (GLfloat)sf::Mouse::getPosition().x * 0.005f;
	model = Rotate(model, 25.0f * PI / 180.0f, { 1.0f, 0.0f, 0.0f });
	model = Rotate(model, rotAngle, { 0.0f, 1.0f, 0.0f });

	glUniformMatrix4fv(glGetUniformLocation(shader.programID, "model"), 1, GL_FALSE, model.m);
	glUniformMatrix4fv(glGetUniformLocation(shader.programID, "view"), 1, GL_FALSE, view.m);
	glUniformMatrix4fv(glGetUniformLocation(shader.programID, "projection"), 1, GL_FALSE, projection.m);

	glUniform1f(glGetUniformLocation(shader.programID, "colorMixFactor"), cube.colorMixFactor);

	glActiveTexture(GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_2D, cube.textureID);

	glBindVertexArray(cube.VAO);
	glDrawElements(GL_TRIANGLES, cube.indexCount, GL_UNSIGNED_INT, 0);
}

void cleanup(const Tetrahedron& tetra, const Cube& cube, const Shader& tetraShader, const Shader& cubeShader)
{
	glDeleteVertexArrays(1, &tetra.VAO);
	glDeleteBuffers(1, &tetra.VBO);
	glDeleteBuffers(1, &tetra.EBO);
	glDeleteProgram(tetraShader.programID);

	glDeleteVertexArrays(1, &cube.VAO);
	glDeleteBuffers(1, &cube.VBO);
	glDeleteBuffers(1, &cube.EBO);
	glDeleteProgram(cubeShader.programID);
	glDeleteTextures(1, &cube.textureID);
}

int main() {
	sf::ContextSettings settings;
	settings.depthBits = 24;
	settings.majorVersion = 3;
	settings.minorVersion = 3;

	sf::RenderWindow window(sf::VideoMode({ WINDOW_WIDTH, WINDOW_HEIGHT }),
		"Modular OpenGL Program", sf::State::Windowed, settings);
	window.setFramerateLimit(60);

	if (glewInit() != GLEW_OK) {
		std::cerr << "Ошибка инициализации GLEW!" << std::endl;
		return -1;
	}
	glEnable(GL_DEPTH_TEST);
	glViewport(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

	Shader tetraShader;
	tetraShader.programID = compileShaders(tetraVertexShaderSource, tetraFragmentShaderSource);

	Shader cubeShader;
	cubeShader.programID = compileShaders(cubeVertexShaderSource, cubeFragmentShaderSource);

	Tetrahedron tetra;
	setupTetrahedron(tetra);

	Cube cube;
	setupCube(cube);

	ActiveShape currentShape = ActiveShape::TETRAHEDRON;

	while (window.isOpen())
	{
		while (std::optional<sf::Event> event = window.pollEvent())
		{
			if (event->is<sf::Event::Closed>()) window.close();
			else if (const sf::Event::Resized* resized = event->getIf<sf::Event::Resized>()) {
				glViewport(0, 0, resized->size.x, resized->size.y);
			}
			else if (const sf::Event::KeyPressed* key = event->getIf<sf::Event::KeyPressed>())
			{
				if (key->code == sf::Keyboard::Key::Num1) {
					currentShape = ActiveShape::TETRAHEDRON;
					std::cout << "Активная фигура: Тетраэдр" << std::endl;
				}
				if (key->code == sf::Keyboard::Key::Num2) {
					currentShape = ActiveShape::CUBE;
					std::cout << "Активная фигура: Куб" << std::endl;
				}
				
				if (currentShape == ActiveShape::TETRAHEDRON) {
					if (key->code == sf::Keyboard::Key::A)
						tetra.position.x -= MOVE_SPEED;
					if (key->code == sf::Keyboard::Key::D)
						tetra.position.x += MOVE_SPEED;
					if (key->code == sf::Keyboard::Key::W)
						tetra.position.y += MOVE_SPEED;
					if (key->code == sf::Keyboard::Key::S)
						tetra.position.y -= MOVE_SPEED;
					if (key->code == sf::Keyboard::Key::Q)
						tetra.position.z -= MOVE_SPEED;
					if (key->code == sf::Keyboard::Key::E)
						tetra.position.z += MOVE_SPEED;
				}

				else if (currentShape == ActiveShape::CUBE) {
					if (key->code == sf::Keyboard::Key::C)
					{
						cube.colorMixFactor += COLOR_CHANGE_SPEED;
						if (cube.colorMixFactor > 1.0f) cube.colorMixFactor = 1.0f;
					}
					if (key->code == sf::Keyboard::Key::V)
					{
						cube.colorMixFactor -= COLOR_CHANGE_SPEED;
						if (cube.colorMixFactor < 0.0f) cube.colorMixFactor = 0.0f;
					}
				}
			}
		}
		glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		Matrix4 projection = Perspective(45.0f, (GLfloat)WINDOW_WIDTH / (GLfloat)WINDOW_HEIGHT, 0.1f, 100.0f);
		Matrix4 view = LookAt({ 0.0f, 0.0f, 8.0f }, { 0.0f, 0.0f, 0.0f }, { 0.0f, 1.0f, 0.0f });
		if (currentShape == ActiveShape::TETRAHEDRON) {
			renderTetrahedron(tetra, tetraShader, view, projection);
		}
		else if (currentShape == ActiveShape::CUBE) {
			renderCube(cube, cubeShader, view, projection);
		}
		glBindVertexArray(0);
		glUseProgram(0);
		window.display();
	}
	cleanup(tetra, cube, tetraShader, cubeShader);
	return 0;
}
