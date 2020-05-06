package com.wl.function;

import android.media.MediaFormat;

public interface IDecoder extends Runnable {
    /**
     * 暂停解码
     */
    void pause();

    /**
     * 继续解码
     */
    void goOn();

    /**
     * 停止解码
     */
    void stop();

    /**
     * 是否正在解码
     */
    Boolean isDecoding();

    /**
     * 是否正在快进
     */
    Boolean isSeeking();

    /**
     * 是否停止解码
     */
    Boolean isStop();

    /**
     * 是否正在暂停
     */
    Boolean isPause();

    /**
     * 设置状态监听器
     */
    void setStateListener();

    /**
     * 获取视频宽
     */
    int getWidth();

    /**
     * 获取视频高
     */
    int getHeight();

    /**
     * 获取视频长度
     */
    long getDuration();

    /**
     * 获取视频旋转角度
     */
    int getRotationAngle();

    /**
     * 获取音视频对应的格式参数
     */
    MediaFormat getMediaFormat();

    /**
     * 获取音视频对应的媒体轨道
     */
    int getTrack();

    /**
     * 获取解码的文件路径
     */
    String getFilePath();
}
