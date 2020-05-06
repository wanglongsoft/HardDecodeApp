//
// Created by 王龙 on 2020-03-25.
//

#include "ShaderYUV.h"

// 视频宽高：720  1280

ShaderYUV::ShaderYUV(GlobalContexts *global_context) {
    gl_program = -1;
    gl_uMatrix = -1;
    gl_texture_id[0] = 0;
    gl_texture_id[1] = 0;
    gl_texture_id[2] = 0;
    gl_video_width = -1;
    gl_video_height = -1;
    gl_window_width = -1;
    gl_window_height = -1;
    context = global_context;
    y_data = NULL;
    u_data = NULL;
    v_data = NULL;
    isNeedRotation = false;
    setVideoSize(context->gl_video_width, context->gl_video_height);
    setWindowSize(context->gl_window_width, context->gl_window_height);
}

ShaderYUV::~ShaderYUV() {

}

GLuint ShaderYUV::LoadShader(GLenum type, const char *shaderSrc) {
    GLuint shader;
    shader = glCreateShader(type);
    if (shader == 0) {
        return 0;
    }
    glShaderSource(shader, 1, &shaderSrc, NULL);
    glCompileShader(shader);
    return shader;
}

GLuint ShaderYUV::LoadProgram(const char *vShaderStr, const char *fShaderStr) {
    initDefMatrix();
    LOGD("LoadProgram in width : %d  height: %d", context->gl_video_width, context->gl_video_height);
    GLuint vertexShader;
    GLuint fragmentShader;
    GLuint mProgram;

    //eglMakeCurrent()函数来将当前的上下文切换，这样opengl的函数才能启动作用
    if(EGL_TRUE != eglMakeCurrent(context->eglDisplay,
                   context->eglSurface, context->eglSurface,
                   context->eglContext)) {
        LOGD("eglMakeCurrent failed");
        return -1;
    }

    // Load the vertex/fragment shaders
    vertexShader = LoadShader(GL_VERTEX_SHADER, vShaderStr);
    fragmentShader = LoadShader(GL_FRAGMENT_SHADER, fShaderStr);

    glClearColor(0, 0, 0, 1);
    glClear(GL_COLOR_BUFFER_BIT);
    glViewport(0, 0, context->gl_window_width, context->gl_window_height);
    // Create the program object
    mProgram = glCreateProgram();
    LOGD("glCreateProgram mProgram : %d", mProgram);
    context->mProgram = mProgram;
    gl_program = mProgram;

    // Attaches a shader object to a program object
    glAttachShader(mProgram, vertexShader);
    glAttachShader(mProgram, fragmentShader);

    // Link the program object
    glLinkProgram(mProgram);
    GLint status = 0;
    glGetProgramiv(mProgram, GL_LINK_STATUS, &status);
    if (status == 0) {
        LOGD("glLinkProgram failed");
        return -1;
    }
    LOGD("glLinkProgram success");

    glUseProgram(mProgram);

    // 获取顶点着色器的位置的句柄
    gl_position = glGetAttribLocation(mProgram, "aPosition");
    context->gl_position = gl_position;
    glEnableVertexAttribArray(gl_position);
    if(isNeedRotation) {
        glVertexAttribPointer(gl_position, 3, GL_FLOAT, GL_FALSE, 0, vertex_coords_rotation_90);
    } else {
        glVertexAttribPointer(gl_position, 3, GL_FLOAT, GL_FALSE, 0, vertex_coords);
    }

    gl_textCoord = glGetAttribLocation(mProgram, "aTextCoord");
    context->gl_textCoord = gl_textCoord;
    glEnableVertexAttribArray(gl_textCoord);
    glVertexAttribPointer(gl_textCoord, 2, GL_FLOAT, GL_FALSE, 0, fragment_coords);
    LOGD("LoadProgram out gl_position : %d gl_textCoord: %d", context->gl_position, context->gl_textCoord);

    gl_uMatrix = glGetUniformLocation(mProgram, "uMatrix");
    glUniform1i(glGetUniformLocation(gl_program, "yTexture"), 0);
    glUniform1i(glGetUniformLocation(gl_program, "uTexture"), 1);
    glUniform1i(glGetUniformLocation(gl_program, "vTexture"), 2);

    //创建若干个纹理对象，并且得到纹理ID
    glGenTextures(3, gl_texture_id);

    //将纹理目标和纹理绑定后，对纹理目标所进行的操作都反映到对纹理上
    glBindTexture(GL_TEXTURE_2D, gl_texture_id[0]);
    //缩小的过滤器
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    //放大的过滤器
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

    glTexImage2D(GL_TEXTURE_2D,
                 0,//细节基本 默认0
                 GL_LUMINANCE,//gpu内部格式 亮度，灰度图（这里就是只取一个亮度的颜色通道的意思）
                 context->gl_video_width,//加载的纹理宽度。最好为2的次幂(这里对y分量数据当做指定尺寸算，但显示尺寸会拉伸到全屏？)
                 context->gl_video_height,//加载的纹理高度。最好为2的次幂
                 0,//纹理边框
                 GL_LUMINANCE,//数据的像素格式 亮度，灰度图
                 GL_UNSIGNED_BYTE,//像素点存储的数据类型
                 NULL //纹理的数据（先不传）
    );
    //绑定纹理
    glBindTexture(GL_TEXTURE_2D, gl_texture_id[1]);
    //缩小的过滤器
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    //设置纹理的格式和大小
    glTexImage2D(GL_TEXTURE_2D,
                 0,//细节基本 默认0
                 GL_LUMINANCE,//gpu内部格式 亮度，灰度图（这里就是只取一个颜色通道的意思）
                 context->gl_video_width / 2,//u数据数量为屏幕的4分之1
                 context->gl_video_height / 2,
                 0,//边框
                 GL_LUMINANCE,//数据的像素格式 亮度，灰度图
                 GL_UNSIGNED_BYTE,//像素点存储的数据类型
                 NULL //纹理的数据（先不传）
    );

    //绑定纹理
    glBindTexture(GL_TEXTURE_2D, gl_texture_id[2]);
    //缩小的过滤器
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    //设置纹理的格式和大小
    glTexImage2D(GL_TEXTURE_2D,
                 0,//细节基本 默认0
                 GL_LUMINANCE,//gpu内部格式 亮度，灰度图（这里就是只取一个颜色通道的意思）
                 context->gl_video_width / 2,
                 context->gl_video_height / 2,//v数据数量为屏幕的4分之1
                 0,//边框
                 GL_LUMINANCE,//数据的像素格式 亮度，灰度图
                 GL_UNSIGNED_BYTE,//像素点存储的数据类型
                 NULL //纹理的数据（先不传）
    );

    context->gl_texture_id[0] = gl_texture_id[0];
    context->gl_texture_id[1] = gl_texture_id[1];
    context->gl_texture_id[2] = gl_texture_id[2];
    return mProgram;
}

int ShaderYUV::CreateProgram() {
    LOGD("CreateProgram in : ");
    GLuint mProgram;
    mProgram = LoadProgram(this->vertex_shader_texture_code, this->fragment_shader_texture_code);
    gl_program = mProgram;
    LOGD("CreateProgram : video_width: %d, video_height： %d, window_width：%d, window_height：%d",
         gl_video_width, gl_video_height, gl_window_width, gl_window_height);
    return 0;
}


void ShaderYUV::Render(uint8_t *data[]) {
    //激活第一层纹理，绑定到创建的纹理
    y_data = data[0];
    u_data = data[1];
    v_data = data[2];
    glActiveTexture(GL_TEXTURE0);
    //绑定y对应的纹理
    glBindTexture(GL_TEXTURE_2D, gl_texture_id[0]);
    //替换纹理，比重新使用glTexImage2D性能高多
    glTexSubImage2D(GL_TEXTURE_2D, 0,
                    0, 0,//相对原来的纹理的offset
                    gl_video_width, gl_video_height,//加载的纹理宽度、高度。最好为2的次幂
                    GL_LUMINANCE, GL_UNSIGNED_BYTE,
                    y_data);

    //激活第二层纹理，绑定到创建的纹理
    glActiveTexture(GL_TEXTURE1);
    //绑定u对应的纹理
    glBindTexture(GL_TEXTURE_2D, gl_texture_id[1]);
    //替换纹理，比重新使用glTexImage2D性能高
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, gl_video_width / 2, gl_video_height / 2, GL_LUMINANCE,
                    GL_UNSIGNED_BYTE,
                    u_data);

    //激活第三层纹理，绑定到创建的纹理
    glActiveTexture(GL_TEXTURE2);
    //绑定v对应的纹理
    glBindTexture(GL_TEXTURE_2D, gl_texture_id[2]);
    //替换纹理，比重新使用glTexImage2D性能高
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, gl_video_width / 2, gl_video_height / 2, GL_LUMINANCE,
                    GL_UNSIGNED_BYTE,
                    v_data);

    glEnableVertexAttribArray(gl_uMatrix);
    glUniformMatrix4fv(gl_uMatrix, 1, false, matrix_scale);

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    //窗口显示，交换双缓冲区
    eglSwapBuffers(context->eglDisplay, context->eglSurface);
}

void ShaderYUV::setVideoSize(int width, int height) {
    gl_video_width = width;
    gl_video_height = height;
}

void ShaderYUV::setWindowSize(int width, int height) {
    gl_window_width = width;
    gl_window_height = height;
}

void ShaderYUV::initDefMatrix() {

//    if((gl_video_width < gl_window_width) && (gl_video_height < gl_window_height)) {// 图片宽高小于视图宽高，原图显示
//        float widthRatio = (float) gl_window_width / gl_video_width;
//        float heightRatio = (float) gl_window_height / gl_video_height;
//        orthoM(
//                matrix_scale, 0,
//                -widthRatio, widthRatio,
//                -heightRatio, heightRatio,
//                -1, 3
//        );
//        return;
//    }

    float originRatio = (float) gl_video_width / gl_video_height;
    float worldRatio = (float) gl_window_width / gl_window_height;
//    originRatio = adjustRatio(originRatio, worldRatio);
    if (worldRatio > 1) {
        if (originRatio > worldRatio) {
            float actualRatio = originRatio / worldRatio;
            orthoM(
                    matrix_scale, 0,
                    -1, 1,
                    -actualRatio, actualRatio,
                    -1, 3
            );
        } else {// 原始比例小于窗口比例，缩放高度度会导致高度超出，因此，高度以窗口为准，缩放宽度
            float actualRatio = worldRatio / originRatio;
            orthoM(
                    matrix_scale, 0,
                    -actualRatio, actualRatio,
                    -1, 1,
                    -1, 3
            );
        }
    } else {
        if (originRatio > worldRatio) {
            float actualRatio = originRatio / worldRatio;
            orthoM(
                    matrix_scale, 0,
                    -1, 1,
                    -actualRatio, actualRatio,
                    -1, 3
            );
        } else {// 原始比例小于窗口比例，缩放高度会导致高度超出，因此，高度以窗口为准，缩放宽度
            float actualRatio = worldRatio / originRatio;
            orthoM(
                    matrix_scale, 0,
                    -actualRatio, actualRatio,
                    -1, 1,
                    -1, 3
            );
        }
    }
}

void ShaderYUV::orthoM(float *m, int mOffset, float left, float right, float bottom, float top,
                       float near, float far) {
    float r_width  = 1.0f / (right - left);
    float r_height = 1.0f / (top - bottom);
    float r_depth  = 1.0f / (far - near);
    float x =  2.0f * (r_width);
    float y =  2.0f * (r_height);
    float z = -2.0f * (r_depth);
    float tx = -(right + left) * r_width;
    float ty = -(top + bottom) * r_height;
    float tz = -(far + near) * r_depth;
    m[mOffset + 0] = x;
    m[mOffset + 5] = y;
    m[mOffset +10] = z;
    m[mOffset +12] = tx;
    m[mOffset +13] = ty;
    m[mOffset +14] = tz;
    m[mOffset +15] = 1.0f;
    m[mOffset + 1] = 0.0f;
    m[mOffset + 2] = 0.0f;
    m[mOffset + 3] = 0.0f;
    m[mOffset + 4] = 0.0f;
    m[mOffset + 6] = 0.0f;
    m[mOffset + 7] = 0.0f;
    m[mOffset + 8] = 0.0f;
    m[mOffset + 9] = 0.0f;
    m[mOffset + 11] = 0.0f;
}

float ShaderYUV::adjustRatio(float ratio, float worldRatio) {//调整视频宽高比，适应手机竖屏
    if((ratio > 1 && worldRatio < 1) ||
            (ratio < 1 && worldRatio > 1)) {
        isNeedRotation = true;
        return 1 / ratio;
    }
    isNeedRotation = false;
    return ratio;
}
