package com.example.trafficpolice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class SpherePlayer  implements SurfaceHolder.Callback {

    public SpherePlayer(GLSurfaceView surfaceView, String fmt) {
        mSurfaceView = surfaceView;
        mRenderer = new SphereRenderer();
        mSurfaceView = surfaceView;
        mSurfaceView.setEGLContextClientVersion(3);
        mSurfaceView.setRenderer(mRenderer);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mSurfaceView.getHolder().addCallback(this);
        mVideoFormat = fmt;
        Matrix.setIdentityM(mRotationMatrix, 0);




        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
               // mSensorMgr.unregisterListener(mSensorListener);
                switch (event.getPointerCount()) {
                    case 1:
                        switch (event.getAction() & MotionEvent.ACTION_MASK) {
                            case MotionEvent.ACTION_DOWN:
                                mStartX = event.getX();
                                mStartY = event.getY();
                                mPressed = true;
                                break;

                            case MotionEvent.ACTION_MOVE:
                                if (!mPressed) break;

                                float x = event.getX();
                                float y = event.getY();
                                float deltaX = mStartX - x;
                                float deltaY = mStartY - y;

                                mStartX = x;
                                mStartY = y;

                                mRenderer.rotateY(0.05f * deltaX);
                                mRenderer.rotateX(-0.05f * deltaY);
                                mSurfaceView.requestRender();
                                break;
                            case MotionEvent.ACTION_UP:

                                //mSensorMgr.registerListener(mSensorListener,mGyroscopeSensor,SensorManager.SENSOR_DELAY_FASTEST);
                                break;
                        }

                        break;

                    case 2:
                        switch (event.getAction() & MotionEvent.ACTION_MASK) {
                            case MotionEvent.ACTION_POINTER_DOWN: {
                                mPressed = false;

                                float deltaX = event.getX(0) - event.getX(1);
                                float deltaY = event.getY(0) - event.getY(1);
                                mLastDistance = (float) Math.pow((deltaX * deltaX + deltaY * deltaY), 0.5);
                            }
                            break;

                            case MotionEvent.ACTION_MOVE: {
                                float deltaX = event.getX(0) - event.getX(1);
                                float deltaY = event.getY(0) - event.getY(1);
                                float distance = (float) Math.pow((deltaX * deltaX + deltaY * deltaY), 0.5);

                                float deltaDistance = distance - mLastDistance;
                                mLastDistance = distance;

                                mRenderer.zoom(-0.05f * deltaDistance);
                                mSurfaceView.requestRender();
                            }
                            break;
                            case MotionEvent.ACTION_UP:
                                //mSensorMgr.registerListener(mSensorListener,mGyroscopeSensor,SensorManager.SENSOR_DELAY_FASTEST);
                                break;
                        }
                        break;

                    default:
                        break;
                }
                return false;
            }
        });

    }






    private GLSurfaceView mSurfaceView;
    private SphereRenderer mRenderer;
    private MediaCodec mVideoDecoder;
    private String mVideoFormat;
    public ArrayBlockingQueue mFrameQueue = new ArrayBlockingQueue(20);


    float[] mRotationMatrix = new float[16];
    private boolean mPressed = false;
    private float mStartX;
    private float mStartY;
    private float mLastDistance;

    public void pushFrame(byte[] frame){
        if(mFrameQueue.remainingCapacity()>0) {
                mFrameQueue.offer(frame);
        }
    }


    private DecoderThread mGLDecoderThread;



    private class DecoderThread extends Thread{
        boolean flag;
        @Override
        public void run() {
            while (true) {
                if(flag){
                    try {
                        int inIndex = mVideoDecoder.dequeueInputBuffer(System.currentTimeMillis());
                        if (inIndex < 0) continue;
                        byte[] data = (byte[]) mFrameQueue.take();
                        mFrameQueue.remove(data);


                        ByteBuffer inputBuffer = mVideoDecoder.getInputBuffer(inIndex);

                        inputBuffer.put(data, 0, data.length);
                        mVideoDecoder.queueInputBuffer(inIndex, 0, data.length, 0, 0);

                        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                        int outIndex = mVideoDecoder.dequeueOutputBuffer(info, 0);
                        if (outIndex < 0) continue;
                        mVideoDecoder.releaseOutputBuffer(outIndex, true);
                        mSurfaceView.requestRender();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else {
                    return;
                }

            }
        }
    }


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        try {
            mVideoDecoder = MediaCodec.createDecoderByType(mVideoFormat);
            MediaFormat fmt = MediaFormat.createVideoFormat(mVideoFormat, 1920, 1080);
            mVideoDecoder.configure(fmt, mRenderer.getSurface(), null, 0);
            mVideoDecoder.start();

            mGLDecoderThread=new DecoderThread();
            mGLDecoderThread.start();
            mGLDecoderThread.flag= mSurfaceView.getVisibility() == View.VISIBLE;
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mGLDecoderThread.flag=false;
        mFrameQueue.clear();
        mVideoDecoder.stop();
        mVideoDecoder.flush();
        mVideoDecoder.release();

    }
}
