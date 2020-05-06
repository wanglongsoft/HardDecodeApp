package com.wl.function;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;

import java.nio.ByteBuffer;

public class VideoDecoder extends BaseDecoder {

    private final String TAG = "VideoDecoder";

    private SurfaceView mSurfaceView;
    private String video_path;
    private Surface mSurface;
    private VideoDecoderUtils video_utils = null;
    public VideoDecoder(String path, SurfaceView sfv) {
        super(path, true);
        mSurfaceView = sfv;
        video_path = path;
        video_utils = new VideoDecoderUtils();
        video_utils.setOutDataType(VideoDecoderUtils.FILE_TypeI420);
    }

    @Override
    boolean check() {
        Log.d(TAG, "check: ");
        if(mSurfaceView != null) {
            return true;
        }
        return false;
    }

    @Override
    IExtractor initExtractor(String path) {
        Log.d(TAG, "initExtractor: ");
        return new VideoExtractor(video_path);
    }

    @Override
    void initSpecParams(MediaFormat format) {
        mVideoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
        mVideoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
        mDuration = format.getLong(MediaFormat.KEY_DURATION) / 1000000;
        Log.d(TAG, "initSpecParams mVideoWidth: " + mVideoWidth + " mVideoHeight: " + mVideoHeight + " mDuration : " + mDuration);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
    }

    @Override
    boolean initRender() {
        Log.d(TAG, "initRender: ");
        return true;
    }

    @Override
    boolean configCodec(MediaCodec codec, MediaFormat format) {
        Log.d(TAG, "configCodec:");
        if (mSurfaceView.getHolder().getSurface() != null) {//mSurfaceView 等待Surface创建后，可初始化成功
            Log.d(TAG, "configCodec: configure");
            mSurface = mSurfaceView.getHolder().getSurface();
            codec.configure(format, null , null, 0);
            notifyDecode();
            return true;
        } else {
            Log.d(TAG, "configCodec: surface == null");
            return false;
        }
    }

    @Override
    void render(ByteBuffer outputBuffers, MediaCodec.BufferInfo bufferInfo) {
        Log.d(TAG, "render bufferInfo : ");
    }

    @Override
    void renderImage(Image image, int width, int height) {
        Log.d(TAG, "renderImage width: " + width + " height : " + height);
        Log.d(TAG, "renderImage format: " + image.getFormat());// YUV_420_888
        if(image != null) {
            video_utils.decodeFramesToImage(image, width, height);
        } else {
            Log.d(TAG, "renderImage: image == null");
        }
    }

    @Override
    void doneDecode() {
        Log.d(TAG, "doneDecode: ");
    }

    public void setDataCallBack(VideoDecoderUtils.DataCallBack callBack) {
        video_utils.setDataCallBack(callBack);
    }
}
