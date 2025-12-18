cmake_minimum_required(VERSION 3.10)
project(lab11)

set(CMAKE_CXX_STANDARD 17)

# 1. Находим GLFW
find_package(glfw3 REQUIRED)

# 2. Подключаем папку include (чтобы работало #include <glad/glad.h>)
include_directories(include)

# 3. Создаем исполняемый файл "main"
# Мы собираем вместе твой код (lab14.cpp) и код загрузчика (glad.c)
add_executable(main lab14.cpp glad.c)

# 4. Линкуем библиотеки к цели "main"
if (APPLE)
    target_link_libraries(main glfw "-framework OpenGL" "-framework Cocoa" "-framework IOKit" "-framework CoreVideo")
else()
    target_link_libraries(main glfw dl)
endif()