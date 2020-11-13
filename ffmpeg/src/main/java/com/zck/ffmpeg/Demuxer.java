package com.zck.ffmpeg;

import java.io.IOException;

public class Demuxer {

    public Demuxer(){
        mPointer = createDemuxer();
    }

    public void open(String url, Callback callback) throws RuntimeException {
        int ret =openInput(mPointer, url, callback);
        if(ret < 0) throw new RuntimeException(getErrorString(ret));
    }

    public void flush() throws RuntimeException{
        int ret = flush(mPointer);
        if(ret < 0) throw new RuntimeException(getErrorString(ret));
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        destroyDemuxer(mPointer);
    }

    private long mPointer;

    public interface Callback{
       void onVideo(byte[] frame, long pts);

       void onAudio(byte[] frame, long pts);
    }

    static {
        System.loadLibrary("native-lib");
    }

    private native long createDemuxer();

    private native void destroyDemuxer(long pointer);

    private native int openInput(long pointer, String url, Callback callback);

    private native int flush(long pointer);

    private native String getErrorString(int value);




}