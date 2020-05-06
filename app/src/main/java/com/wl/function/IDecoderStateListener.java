package com.wl.function;

public interface IDecoderStateListener {
    void decoderPrepare(BaseDecoder decodeJob);
    void decoderReady(BaseDecoder decodeJob);
    void decoderRunning(BaseDecoder decodeJob);
    void decoderPause(BaseDecoder decodeJob);
    void decodeOneFrame(BaseDecoder decodeJob);
    void decoderFinish(BaseDecoder decodeJob);
    void decoderDestroy(BaseDecoder decodeJob);
    void decoderError(BaseDecoder decodeJob, String msg);
}
