cmake_minimum_required(VERSION 3.18.1)

project(test_app)

set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
#set(ANDROID_ABI arm64-v8a armeabi-v7a)
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}  -frtti -fopenmp -static-openmp")
#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -frtti -fopenmp -static-openmp")

find_library(log-lib log android)
find_library(jnigraphics-lib jnigraphics)
add_definitions(-DMNN_USE_LOGCAT)


set(LIB_DIR ${CMAKE_SOURCE_DIR}/src/main/Libs)
include_directories(src/main/include)

add_library(MNN SHARED IMPORTED)
add_library(MNN_CL SHARED IMPORTED)
add_library(MNN_GL SHARED IMPORTED)

set_target_properties(MNN PROPERTIES IMPORTED_LOCATION
        ${LIB_DIR}/${ANDROID_ABI}/libMNN.so)
set_target_properties(MNN_CL PROPERTIES IMPORTED_LOCATION
        ${LIB_DIR}/${ANDROID_ABI}/libMNN_CL.so)
set_target_properties(MNN_GL PROPERTIES IMPORTED_LOCATION
        ${LIB_DIR}/${ANDROID_ABI}/libMNN_GL.so)

file(GLOB SRC ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/*.cpp)

#message( "${SRC}")
add_library(cpp_test SHARED ${SRC})
target_link_libraries(cpp_test
        ${log-lib}
        ${log}
        ${android}
        ${jnigraphics}
        MNN
#        MNN_CL
#        MNN_GL
        )
