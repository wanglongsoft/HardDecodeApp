package com.wl.function;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AVExtractor {
    private final String TAG = "AVExtractor";
    /**音视频分离器*/
    private MediaExtractor mExtractor;

    /**音频通道索引*/
    private int mAudioTrack = -1;

    /**视频通道索引*/
    private int mVideoTrack = -1;

    /**当前帧时间戳*/
    private long mCurSampleTime = 0;

    /**当前帧标志*/
    private int mCurSampleFlag = 0;

    /**开始解码时间点*/
    private long mStartPos = 0;

    public AVExtractor(String path) {
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(path);
        } catch (IOException e) {
            Log.e(TAG, "AVExtractor setDataSource error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取视频格式参数
     */
    public MediaFormat getVideoFormat() {
        Log.d(TAG, "getVideoFormat TrackCount: " + mExtractor.getTrackCount());
        for (int i = 0; i < mExtractor.getTrackCount(); i++) {
            String key_mime = mExtractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
            if(key_mime.startsWith("video/")) {
                mVideoTrack = i;
                break;
            }
        }

        if(mVideoTrack >= 0) {
            return mExtractor.getTrackFormat(mVideoTrack);
        } else {
            return null;
        }
    }

    /**
     * 获取音频格式参数
     */
    public MediaFormat getAudioFormat() {
        Log.d(TAG, "getAudioFormat TrackCount: " + mExtractor.getTrackCount());
        for (int i = 0; i < mExtractor.getTrackCount(); i++) {
            String key_mime = mExtractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
            if(key_mime.startsWith("audio/")) {
                mAudioTrack = i;
                break;
            }
        }

        if(mAudioTrack >= 0) {
            return mExtractor.getTrackFormat(mAudioTrack);
        } else {
            return null;
        }
    }

    /**
     * 读取视频数据
     */
    int readBuffer(ByteBuffer byteBuffer) {
        byteBuffer.clear();
        selectSourceTrack();
        int readSampleCount = mExtractor.readSampleData(byteBuffer, 0);
        if (readSampleCount < 0) {
            return -1;
        }
        //记录当前帧的时间戳
        mCurSampleTime = mExtractor.getSampleTime();
        mCurSampleFlag = mExtractor.getSampleFlags();
        //进入下一帧
        mExtractor.advance();
        return readSampleCount;
    }

    /**
     * 选择通道
     */
    private void selectSourceTrack() {
        Log.d(TAG, "selectSourceTrack mVideoTrack : " + mVideoTrack + " mAudioTrack : " + mAudioTrack);
        if (mVideoTrack >= 0) {
            mExtractor.selectTrack(mVideoTrack);
        } else if (mAudioTrack >= 0) {
            mExtractor.selectTrack(mAudioTrack);
        }
    }

    /**
     * Seek到指定位置，并返回实际帧的时间戳
     */
    public long seek(long pos) {
        mExtractor.seekTo(pos, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        return mExtractor.getSampleTime();
    }

    /**
     * 停止读取数据
     */
    public void stop() {
        mExtractor.release();
        mExtractor = null;
    }

    public int getVideoTrack() {
        return mVideoTrack;
    }

    public int getAudioTrack() {
        return mAudioTrack;
    }

    public void setStartPos(long pos) {
        mStartPos = pos;
    }

    /**
     * 获取当前帧时间
     */
    public long getCurrentTimestamp() {
        return mCurSampleTime;
    }

    public long getSampleFlag() {
        return mCurSampleFlag;
    }
}
