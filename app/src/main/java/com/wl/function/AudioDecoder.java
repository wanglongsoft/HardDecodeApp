package com.wl.function;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

public class AudioDecoder extends BaseDecoder {

    private final String TAG = "AudioDecoder";
    /**采样率**/
    private int mSampleRate = -1;

    /**解码器输出音频采样率**/
    private int OUT_SAMPLE_RATE = 44100;

    /**声音通道数量**/
    private int mChannels = -1;

    /**PCM采样位数**/
    private int mPCMEncodeBit = AudioFormat.ENCODING_PCM_16BIT;

    /**音频播放器*/
    private AudioTrack mAudioTrack = null;

    public AudioDecoder(String path) {
        super(path, false);
    }
    @Override
    boolean check() {
        Log.d(TAG, "check: ");
        return true;
    }

    @Override
    IExtractor initExtractor(String path) {
        Log.d(TAG, "initExtractor: ");
        return new AudioExtractor(path);
    }

    @Override
    void initSpecParams(MediaFormat format) {
        try {
            mChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
//            mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);//解析错误,原因未知,使用Codec的getOutputFormat获取采样率
           if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
               Log.d(TAG, "initSpecParams containsKey: KEY_PCM_ENCODING");
               mPCMEncodeBit = format.getInteger(MediaFormat.KEY_PCM_ENCODING);
            } else {
                //如果没有这个参数，默认为16位采样
               Log.d(TAG, "initSpecParams not containsKey: KEY_PCM_ENCODING");
               mPCMEncodeBit = AudioFormat.ENCODING_PCM_16BIT;
            }
        } catch (Exception e) {
            Log.d(TAG, "initSpecParams Exception : " + e.getMessage());
        }
        Log.d(TAG, "initSpecParams mChannels: " + mChannels + " mPCMEncodeBit: " + mPCMEncodeBit);
    }

    @Override
    boolean initRender() {
        Log.d(TAG, "initRender mChannels: " + mChannels);
        int channel;
        if (mChannels == 1) {
            //单声道
            channel = AudioFormat.CHANNEL_OUT_MONO;
        } else {
            //双声道
            channel = AudioFormat.CHANNEL_OUT_STEREO;
        }

        //获取最小缓冲区
        int minBufferSize = AudioTrack.getMinBufferSize(mSampleRate, channel, mPCMEncodeBit);
        Log.d(TAG, "initRender minBufferSize: " + minBufferSize + " mSampleRate: " + mSampleRate
                + " mPCMEncodeBit: " + mPCMEncodeBit + " mChannels: " + mChannels);
        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,  //播放类型：音乐
                OUT_SAMPLE_RATE,  //采样率
                channel, //通道
                mPCMEncodeBit,  //采样位数
                minBufferSize, //缓冲区大小
                AudioTrack.MODE_STREAM);  //播放模式：数据流动态写入
        mAudioTrack.play();
        return true;
    }

    @Override
    boolean configCodec(MediaCodec codec, MediaFormat format) {
        Log.d(TAG, "configCodec: ");
        codec.configure(format, null , null, 0);
        mSampleRate = codec.getOutputFormat().getInteger(MediaFormat.KEY_SAMPLE_RATE);//使用codec.getOutputFormat获取采样频率
        Log.d(TAG, "configCodec sample rate: " + mSampleRate);
        return true;
    }

    @Override
    void render(ByteBuffer outputBuffers, MediaCodec.BufferInfo bufferInfo) {
        mAudioTrack.write(outputBuffers, bufferInfo.size, AudioTrack.WRITE_BLOCKING);
    }

    @Override
    void renderImage(Image image, int width, int height) {

    }

    @Override
    void doneDecode() {
        Log.d(TAG, "doneDecode:");
        mAudioTrack.stop();
        mAudioTrack.release();
    }
}
