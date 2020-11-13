package com.example.trafficpolice;

import android.app.Activity;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class Player implements SurfaceHolder.Callback {

    public Player(SurfaceView surfaceView, String fmt) {
        mSurfaceView = surfaceView;
        mSurfaceView.getHolder().addCallback(this);

        mVideoFormat = fmt;

    }

    public void pushFrame(byte[] frame) {
            if(mFrameQueue.remainingCapacity()>0) {
                    mFrameQueue.offer(frame);
            }
    }


    private SurfaceView mSurfaceView;
    private MediaCodec mVideoDecoder;
    private String mVideoFormat;
    public ArrayBlockingQueue mFrameQueue = new ArrayBlockingQueue(20);
    private DecoderThread mDecoderThread;


  private class DecoderThread extends Thread {
      boolean flag;
        @Override
        public void run() {

            while (true) {
                if(flag){
                    try {
                        int inIndex = mVideoDecoder.dequeueInputBuffer(System.currentTimeMillis());
                        if (inIndex < 0) continue;
                        byte[] data = new byte[0];
                        try {
                            data = (byte[]) mFrameQueue.take();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mFrameQueue.remove(data);


                        ByteBuffer inputBuffer = mVideoDecoder.getInputBuffer(inIndex);

                        inputBuffer.put(data, 0, data.length);
                        mVideoDecoder.queueInputBuffer(inIndex, 0, data.length, 0, 0);

                        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                        int outIndex = mVideoDecoder.dequeueOutputBuffer(info, 0);
                        if (outIndex < 0) continue;
                        mVideoDecoder.releaseOutputBuffer(outIndex, true);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else {
                    return;
                }
            }

        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
            try {
                mVideoDecoder = MediaCodec.createDecoderByType(mVideoFormat);
                MediaFormat fmt = MediaFormat.createVideoFormat(mVideoFormat, 1920, 1080);
                mVideoDecoder.configure(fmt, holder.getSurface(), null, 0);
                mVideoDecoder.start();

                mDecoderThread=new DecoderThread();
                mDecoderThread.start();

                mDecoderThread.flag= mSurfaceView.getVisibility() == View.VISIBLE;

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mDecoderThread.flag=false;
        mFrameQueue.clear();
        mVideoDecoder.stop();
        mVideoDecoder.flush();
        mVideoDecoder.release();

    }
}
////////////////////////////////////////////////////////////////////////////////////////////////////////
