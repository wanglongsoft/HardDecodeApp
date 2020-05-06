package com.wl.function;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;

public abstract class BaseDecoder implements IDecoder {

    private final String TAG = "BaseDecoder";
    //-------------线程相关------------------------
    /**
     * 解码器是否在运行
     */
    private boolean mIsRunning = true;

    /**
     * 线程等待锁
     */
    private Object mLock = new Object();

    /**
     * 是否可以进入解码
     */
    private boolean mReadyForDecode = false;

    //---------------解码相关-----------------------
    /**
     * 音视频解码器
     */
    protected MediaCodec mCodec = null;

    /**
     * 音视频数据读取器
     */
    protected IExtractor mExtractor  = null;

    /**
     * 解码输入缓存区
     */
    protected ByteBuffer mInputBuffers = null;

    /**
     * 解码输出缓存区
     */
    protected ByteBuffer mOutputBuffers = null;

    /**
     * 解码输出缓存区
     */
    protected Image mOutputImage = null;

    /**
     * 解码数据信息
     */
    private MediaCodec.BufferInfo mBufferInfo;

    private DecodeState mState = DecodeState.STOP;

    /**
     * 解码文件路径
     *
     */
    private String mFilePath = null;

    protected long mDuration = 0;
    private long mEndPos = 0;
    /**
     * 流数据是否结束
     */
    private boolean mIsEOS = false;

    protected int mVideoWidth = 0;

    protected int mVideoHeight = 0;

    /**
     * 开始解码时间，用于音视频同步
     */
    private long mStartTimeForSync = -1;

    // 是否需要音视频渲染同步
    private boolean mSyncRender = true;

    // 是否需要音视频渲染同步
    private boolean isVideoDecoder = false;

    public BaseDecoder(String path, boolean isVideoDecoder) {
        mFilePath = path;
        this.isVideoDecoder = isVideoDecoder;
        mBufferInfo = new MediaCodec.BufferInfo();
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause: ");
        mState = DecodeState.PAUSE;
        notifyDecode();
    }

    @Override
    public void goOn() {
        Log.d(TAG, "goOn: ");
        mState = DecodeState.DECODING;
        notifyDecode();
    }

    @Override
    public void stop() {
        Log.d(TAG, "stop: ");
        mState = DecodeState.STOP;
        mIsRunning = false;
        notifyDecode();
    }

    @Override
    public Boolean isDecoding() {
        return mState == DecodeState.DECODING;
    }

    @Override
    public Boolean isSeeking() {
        return mState == DecodeState.SEEKING;
    }

    @Override
    public Boolean isStop() {
        return mState == DecodeState.STOP;
    }

    @Override
    public Boolean isPause() {
        return mState == DecodeState.PAUSE;
    }

    @Override
    public void setStateListener() {

    }

    @Override
    public int getWidth() {
        return this.mVideoWidth;
    }

    @Override
    public int getHeight() {
        return this.mVideoHeight;
    }

    @Override
    public long getDuration() {
        return this.mDuration;
    }

    @Override
    public int getRotationAngle() {
        return 0;
    }

    @Override
    public MediaFormat getMediaFormat() {
        return mExtractor.getFormat();
    }

    @Override
    public int getTrack() {
        return mExtractor.getTrack();
    }

    @Override
    public String getFilePath() {
        return mFilePath;
    }

    @Override
    public void run() {
        Log.d(TAG, "run: BaseDecoder");
        if (mState == DecodeState.STOP) {
            mState = DecodeState.START;
        }
        //【解码步骤：1. 初始化，并启动解码器】
        if (!init()) {
            Log.d(TAG, "run: coec init fail, return");
            return;
        }

        while (mIsRunning) {
            if (mState != DecodeState.START &&
                    mState != DecodeState.DECODING &&
                    mState != DecodeState.SEEKING) {
                Log.d(TAG, "run waitDecode mState : " + mState);
                waitDecode();
                mStartTimeForSync = System.currentTimeMillis() - getCurTimeStamp();
            }

            if (!mIsRunning || mState == DecodeState.STOP) {
                mIsRunning = false;
                break;
            }

            if (mStartTimeForSync == -1) {
                mStartTimeForSync = System.currentTimeMillis();
            }

            //如果数据没有解码完毕，将数据推入解码器解码
            if (!mIsEOS) {
                //【解码步骤：2. 将数据压入解码器输入缓冲】
                mIsEOS = pushBufferToDecoder();
            }

            //【解码步骤：3. 将解码好的数据从缓冲区拉取出来】
            int index = pullBufferFromDecoder();
            if (index >= 0) {
                // ---------【音视频同步】-------------
                if (mSyncRender && mState == DecodeState.DECODING) {
                    sleepRender();
                }
                //【解码步骤：4. 渲染】
                if(this.isVideoDecoder) {
                    renderImage(mOutputImage, mVideoWidth, mVideoHeight);
                } else {
                    render(mOutputBuffers, mBufferInfo);
                }
                //【解码步骤：5. 释放输出缓冲】
                mCodec.releaseOutputBuffer(index, true);
                if (mState == DecodeState.START) {
                    mState = DecodeState.PAUSE;
                }
            }
            //【解码步骤：6. 判断解码是否完成】
            if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                mState = DecodeState.FINISH;
            }
        }
        doneDecode();
    }

    public long getCurTimeStamp() {
        return mBufferInfo.presentationTimeUs / 1000;
    }

    private boolean init()  {
        //1.检查参数是否完整
        if (mFilePath.isEmpty() || (!new File(mFilePath).exists())) {
            Log.d(TAG, "init: file not exist : " + mFilePath);
            return false;
        }
        //调用虚函数，检查子类参数是否完整
        if (!check()) {
            Log.d(TAG, "init: check fail");
            return false;
        }

        //2.初始化数据提取器
        mExtractor = initExtractor(mFilePath);
        if (mExtractor == null || mExtractor.getFormat() == null) {
            Log.d(TAG, "init: check mExtractor fail");
            return false;
        }

        //3.初始化参数
        if (!initParams()) {
            Log.d(TAG, "init: initParams fail");
            return false;
        }

        //4.初始化解码器
        if (!initCodec()) {
            Log.d(TAG, "init: initCodec fail");
            return false;
        }

        //5.初始化渲染器
        if (!initRender()) {
            Log.d(TAG, "init: initRender fail");
            return false;
        }

        return true;
    }

    private boolean initParams() {
        try {
            MediaFormat format = mExtractor.getFormat();
            mDuration = format.getLong(MediaFormat.KEY_DURATION) / 1000;
            if (mEndPos == 0) {
                mEndPos = mDuration;
            }
            initSpecParams(format);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean initCodec() {
        Log.d(TAG, "initCodec: ");
        try {
            //1.根据音视频编码格式初始化解码器
            String type = mExtractor.getFormat().getString(MediaFormat.KEY_MIME);
            Log.d(TAG, "initCodec codec type: " + type);
            mCodec = MediaCodec.createDecoderByType(type);
            //2.配置解码器
            if (!configCodec(mCodec, mExtractor.getFormat())) {
                waitDecode();
            }
            //3.启动解码器
            mCodec.start();
        } catch (Exception e) {
            Log.d(TAG, "initCodec exception: " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean pushBufferToDecoder() {
        int inputBufferIndex = mCodec.dequeueInputBuffer(1000);
        Log.d(TAG, "pushBufferToDecoder inputBufferIndex: " + inputBufferIndex);
        boolean isEndOfStream = false;
        if (inputBufferIndex >= 0) {
            mInputBuffers = mCodec.getInputBuffer(inputBufferIndex);
            ByteBuffer inputBuffer = mInputBuffers;
            int sampleSize = mExtractor.readBuffer(inputBuffer);
            Log.d(TAG, "pushBufferToDecoder sampleSize: " + sampleSize);
            if (sampleSize < 0) {
                //如果数据已经取完，压入数据结束标志：BUFFER_FLAG_END_OF_STREAM
                mCodec.queueInputBuffer(inputBufferIndex, 0, 0,
                        0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                isEndOfStream = true;
            } else {
                mCodec.queueInputBuffer(inputBufferIndex, 0,
                        sampleSize, mExtractor.getCurrentTimestamp(), 0);
            }
        }
        return isEndOfStream;
    }

    private int pullBufferFromDecoder() {
        // 查询是否有解码完成的数据，index >=0 时，表示数据有效，并且index为缓冲区索引
        int index = mCodec.dequeueOutputBuffer(mBufferInfo, 1000);
        Log.d(TAG, "pullBufferFromDecoder index: " + index);
        if(index >= 0) {
            if(this.isVideoDecoder) {
                mOutputImage = mCodec.getOutputImage(index);
            } else {
                mOutputBuffers = mCodec.getOutputBuffer(index);
            }
        }
        return index;
    }

    /**
     * 解码线程进入等待
     */
    private void waitDecode() {
        Log.d(TAG, "waitDecode: ");
        try {
            synchronized(mLock) {
                mLock.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 通知解码线程继续运行
     */
    protected void notifyDecode() {
        synchronized(mLock) {
            mLock.notifyAll();
        }
    }

    private void sleepRender() {
        long passTime = System.currentTimeMillis() - mStartTimeForSync;
        long curTime = getCurTimeStamp();
        Log.d(TAG, "sleepRender curTime: " + curTime + " passTime: " + passTime);
        if (curTime > passTime) {
            try {
                Thread.sleep(curTime - passTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查子类参数
     */
    abstract boolean check();

    /**
     * 初始化数据提取器
     */
    abstract IExtractor initExtractor(String path);

    /**
     * 初始化子类自己特有的参数
     */
    abstract void initSpecParams(MediaFormat format);

    /**
     * 初始化渲染器
     */
    abstract boolean initRender();

    /**
     * 配置解码器
     */
    abstract boolean configCodec(MediaCodec codec, MediaFormat format);

    abstract void render(ByteBuffer outputBuffers, MediaCodec.BufferInfo bufferInfo);
    abstract void renderImage(Image image, int width, int height);
    abstract void doneDecode();
}
