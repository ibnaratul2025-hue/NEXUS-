cmake_minimum_required(VERSION 3.22.1)
project("nexus-llama")

add_library(llama SHARED
    llama-jni.cpp
)

find_library(log-lib log)

target_link_libraries(llama
    ${log-lib}
)
