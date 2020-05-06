package com.wl.function;

import android.media.MediaFormat;

import java.nio.ByteBuffer;

public class AudioExtractor implements IExtractor {

    private AVExtractor mAudioExtractor;
    public AudioExtractor(String path) {
        mAudioExtractor = new AVExtractor(path);
    }

    @Override
    public MediaFormat getFormat() {
        return mAudioExtractor.getAudioFormat();
    }

    @Override
    public int readBuffer(ByteBuffer buffer) {
        return mAudioExtractor.readBuffer(buffer);
    }

    @Override
    public long getCurrentTimestamp() {
        return mAudioExtractor.getCurrentTimestamp();
    }

    @Override
    public long seek(long pos) {
        return mAudioExtractor.seek(pos);
    }

    @Override
    public void setStartPos(long pos) {
        mAudioExtractor.setStartPos(pos);
    }

    @Override
    public void stop() {
        mAudioExtractor.stop();
    }

    @Override
    public int getTrack() {
        return mAudioExtractor.getAudioTrack();
    }
}
