cmake_minimum_required(VERSION 3.4.1)

file(GLOB lame lame/*.c)
include_directories(lame)

add_library(
        lame_util
        SHARED
        ${lame}
        lame_util.cpp)