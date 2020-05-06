//
// Created by 王龙 on 2020-03-25.
//

#ifndef OPENGLNDK_EGLDISPLAYYUV_H
#define OPENGLNDK_EGLDISPLAYYUV_H

#include "LogUtils.h"
#include "GlobalContexts.h"
#include <EGL/egl.h>

class EGLDisplayYUV {
public:
    EGLDisplayYUV(ANativeWindow * window, GlobalContexts *context);
    ~EGLDisplayYUV();
    int eglOpen();
    int eglClose();
    ANativeWindow * nativeWindow;
    GlobalContexts *global_context;
};

#endif //OPENGLNDK_EGLDISPLAYYUV_H
