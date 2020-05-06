//
// Created by 24909 on 2020/4/7.
//

#ifndef HARDDECODEAPP_LOGUTILS_H
#define HARDDECODEAPP_LOGUTILS_H

#include <android/log.h>

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "HardDecodeApp", __VA_ARGS__)

#endif //HARDDECODEAPP_LOGUTILS_H
