#include <jni.h>
#include <string>
#include <pthread.h>
#include <android/native_window_jni.h>
#include "GlobalContexts.h"
#include "LogUtils.h"
#include "EGLDisplayYUV.h"
#include "ShaderYUV.h"


ANativeWindow * nativeWindow = NULL;
GlobalContexts *global_context = NULL;
pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
EGLDisplayYUV *eglDisplayYuv = NULL;
ShaderYUV * shaderYuv = NULL;
unsigned char* array_data = NULL;
char* ConvertJByteaArrayToChars(JNIEnv *env, jbyteArray bytearray);

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_harddecodeapp_MainActivity_setSurface(JNIEnv *env, jobject thiz, jobject surface) {
    // TODO: implement setSurface()
    LOGD("setPlayerSurface in");
    pthread_mutex_lock(&mutex);

    if (nativeWindow) {
        ANativeWindow_release(nativeWindow);
        nativeWindow = 0;
    }

    // 创建新的窗口用于视频显示
    nativeWindow = ANativeWindow_fromSurface(env, surface);
    if(NULL == global_context) {
        global_context = new GlobalContexts();
    }
    global_context->nativeWindow = nativeWindow;
    if(NULL != eglDisplayYuv) {
        eglDisplayYuv->eglClose();
        delete eglDisplayYuv;
        eglDisplayYuv = NULL;
    }
    if(NULL != shaderYuv) {
        delete shaderYuv;
        shaderYuv = NULL;
    }
    pthread_mutex_unlock(&mutex);
    LOGD("setPlayerSurface out");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_harddecodeapp_MainActivity_setSurfaceSize(JNIEnv *env, jobject thiz, jint width,
                                                      jint height) {
    LOGD("setSurfaceSize in");
    pthread_mutex_lock(&mutex);
    if(NULL == global_context) {
        global_context = new GlobalContexts();
    }
    global_context->gl_window_width = width;
    global_context->gl_window_height = height;
    pthread_mutex_unlock(&mutex);
    LOGD("setSurfaceSize out");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_harddecodeapp_MainActivity_rendSurface(JNIEnv *env, jobject thiz, jbyteArray data,
                                                   jint width, jint height) {
    pthread_mutex_lock(&mutex);
    if(NULL == global_context) {
        global_context = new GlobalContexts();
    }
    if(NULL == eglDisplayYuv) {
        eglDisplayYuv = new EGLDisplayYUV(global_context->nativeWindow, global_context);
        eglDisplayYuv->eglOpen();
        global_context->gl_video_width = width;
        global_context->gl_video_height = height;
    }
    if(NULL == shaderYuv) {
        shaderYuv = new ShaderYUV(global_context);
        shaderYuv->CreateProgram();
    }
    if(NULL != array_data) {
        delete array_data;
        array_data = NULL;
    }
    array_data = (unsigned char*) ConvertJByteaArrayToChars(env, data);
    unsigned char* frame_data[3];
    frame_data[0] = array_data;
    frame_data[1] = array_data + width * height;
    frame_data[2] = array_data + width * height +  width * height / 4;
    shaderYuv->Render(frame_data);
    pthread_mutex_unlock(&mutex);
}


char* ConvertJByteaArrayToChars(JNIEnv *env, jbyteArray bytearray)
{
    char *chars = NULL;
    jbyte *bytes;
    bytes = env->GetByteArrayElements(bytearray, 0);
    int chars_len = env->GetArrayLength(bytearray);
    chars = new char[chars_len + 1];//使用结束后, delete 该数组
    memset(chars,0,chars_len + 1);
    memcpy(chars, bytes, chars_len);
    chars[chars_len] = 0;
    env->ReleaseByteArrayElements(bytearray, bytes, 0);
    return chars;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wl_harddecodeapp_MainActivity_releaseSurface(JNIEnv *env, jobject thiz) {
    LOGD("releaseSurface in");
    pthread_mutex_lock(&mutex);
    if(NULL != shaderYuv) {
        delete shaderYuv;
        shaderYuv = NULL;
    }
    if(NULL != eglDisplayYuv) {
        delete eglDisplayYuv;
        eglDisplayYuv = NULL;
    }
    if(NULL != global_context) {
        delete global_context;
        global_context = NULL;
    }
    if(NULL != array_data) {
        delete array_data;
        array_data = NULL;
    }
    pthread_mutex_unlock(&mutex);
    LOGD("releaseSurface out");
}