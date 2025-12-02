cmake_minimum_required(VERSION 3.26)
project(lab11 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(SFML 3.0 COMPONENTS Graphics Window System REQUIRED)

find_package(GLEW REQUIRED)

add_executable(${PROJECT_NAME} main.cpp)

target_link_libraries(${PROJECT_NAME}
    PRIVATE
        SFML::Graphics
        SFML::Window
        SFML::System
        GLEW::GLEW
)

if(APPLE)
    find_library(OPENGL_FRAMEWORK OpenGL)
    if(OPENGL_FRAMEWORK)
        target_link_libraries(${PROJECT_NAME} PRIVATE ${OPENGL_FRAMEWORK})
    endif()

elseif(WIN32)    
    if(VCPKG_TARGET_TRIPLET)
        set(SFML_DLL_DIR "${VCPKG_INSTALLED_DIR}/${VCPKG_TARGET_TRIPLET}/bin")
        set(GLEW_DLL_DIR "${VCPKG_INSTALLED_DIR}/${VCPKG_TARGET_TRIPLET}/bin")

        foreach(dll IN ITEMS
            sfml-graphics-3.dll
            sfml-window-3.dll
            sfml-system-3.dll
            glew32.dll
        )
            add_custom_command(TARGET ${PROJECT_NAME} POST_BUILD
                COMMAND ${CMAKE_COMMAND} -E copy_if_different
                    "${SFML_DLL_DIR}/${dll}"
                    "$<TARGET_FILE_DIR:${PROJECT_NAME}>/${dll}"
                COMMAND ${CMAKE_COMMAND} -E copy_if_different
                    "${GLEW_DLL_DIR}/${dll}"
                    "$<TARGET_FILE_DIR:${PROJECT_NAME}>/${dll}"
                COMMENT "Copying ${dll}"
            )
        endforeach()
    endif()
endif()