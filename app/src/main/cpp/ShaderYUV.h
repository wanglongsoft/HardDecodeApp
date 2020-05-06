//
// Created by 王龙 on 2020-03-25.
//

#ifndef OPENGLNDK_SHADERYUV_H
#define OPENGLNDK_SHADERYUV_H

#include <GLES2/gl2.h>
#include <EGL/egl.h>
#include "GlobalContexts.h"
#include "LogUtils.h"

#define GET_STR(x) #x

class ShaderYUV {
public:
    ShaderYUV(GlobalContexts *global_context);
    ~ShaderYUV();
    GLuint LoadShader(GLenum type, const char *shaderSrc);
    GLuint LoadProgram(const char *vShaderStr, const char *fShaderStr);
    int CreateProgram();
    void Render(uint8_t *data[]);
    void setVideoSize(int width, int height);
    void setWindowSize(int width, int height);
    void initDefMatrix();
    void orthoM(float m[], int mOffset,
                float left, float right, float bottom, float top,
                float near, float far);
    float adjustRatio(float ratio, float worldRatio);
    
    const char *vertex_shader_texture_code = GET_STR(
            attribute vec4 aPosition;//输入的顶点坐标，会在程序指定将数据输入到该字段
            attribute vec2 aTextCoord;//输入的纹理坐标，会在程序指定将数据输入到该字段
            varying vec2 vTextCoord;//输出的纹理坐标
            uniform mat4 uMatrix;
            void main() {
                //
                vTextCoord = aTextCoord;
                //直接把传入的坐标值作为传入渲染管线。gl_Position是OpenGL内置的
                gl_Position = aPosition * uMatrix;
            }
    );

    const char *fragment_shader_texture_code = GET_STR(
            precision mediump float;
            varying vec2 vTextCoord;
            //输入的yuv三个纹理
            uniform sampler2D yTexture;//采样器
            uniform sampler2D uTexture;//采样器
            uniform sampler2D vTexture;//采样器
            void main() {
                vec3 yuv;
                vec3 rgb;
                //分别取yuv各个分量的采样纹理
                yuv.x = texture2D(yTexture, vTextCoord).g;
                yuv.y = texture2D(uTexture, vTextCoord).g - 0.5;
                yuv.z = texture2D(vTexture, vTextCoord).g - 0.5;
                rgb = mat3(
                        1.0, 1.0, 1.0,
                        0.0, -0.39465, 2.03211,
                        1.13983, -0.5806, 0.0
                ) * yuv;
                //gl_FragColor是OpenGL内置的
                gl_FragColor = vec4(rgb, 1.0);
            }
    );

    float vertex_coords[12] = {//世界坐标
            -1, -1, 0, // left bottom
            1, -1, 0, // right bottom
            -1, 1, 0,  // left top
            1, 1, 0,   // right top
    };

     float vertex_coords_rotation_90[12] = {//世界坐标，原图顺时针旋转90°
            -1, 1, 0, // left bottom
            -1, -1, 0,// right bottom
            1, 1, 0,  // left top
            1, -1, 0,   // right top
    };

    float fragment_coords[8] = {//纹理坐标
            0,  1,
            1, 1,
            0, 0,
            1, 0,
    };

    float matrix_scale[16];
    GLint gl_program;
    GLint gl_position;
    GLint gl_textCoord;
    GLint gl_uMatrix;
    GLint gl_video_width;
    GLint gl_video_height;
    GLint gl_window_width;
    GLint gl_window_height;
    GLuint gl_texture_id[3];
    bool isNeedRotation;
    GlobalContexts *context;
    uint8_t *y_data;
    uint8_t *u_data;
    uint8_t *v_data;
};


#endif //OPENGLNDK_SHADERYUV_H
