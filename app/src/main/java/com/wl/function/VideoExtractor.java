package com.wl.function;

import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

public class VideoExtractor implements IExtractor {

    private final String TAG = "VideoExtractor";
    private AVExtractor mVideoExtractor;
    public VideoExtractor(String path) {
        Log.d(TAG, "VideoExtractor: ");
        mVideoExtractor = new AVExtractor(path);
    }
    @Override
    public MediaFormat getFormat() {
        Log.d(TAG, "getFormat: ");
        return mVideoExtractor.getVideoFormat();
    }

    @Override
    public int readBuffer(ByteBuffer buffer) {
        Log.d(TAG, "readBuffer: ");
        return mVideoExtractor.readBuffer(buffer);
    }

    @Override
    public long getCurrentTimestamp() {
        return mVideoExtractor.getCurrentTimestamp();
    }

    @Override
    public long seek(long pos) {
        return mVideoExtractor.seek(pos);
    }

    @Override
    public void setStartPos(long pos) {
        mVideoExtractor.setStartPos(pos);
    }

    @Override
    public void stop() {
        mVideoExtractor.stop();
    }

    @Override
    public int getTrack() {
        return mVideoExtractor.getVideoTrack();
    }
}
