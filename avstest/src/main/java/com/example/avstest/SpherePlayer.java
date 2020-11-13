package com.example.avstest;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaSync;
import android.media.PlaybackParams;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SpherePlayer extends MediaCodec.Callback implements SurfaceHolder.Callback  {
    private MainActivity mActivity;

    public SpherePlayer(GLSurfaceView surfaceView, String fmt, MainActivity activity) {
        mSurfaceView = surfaceView;
        mActivity = activity;
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

        mSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    if (mTimeStamp != 0) {
                        final double dT = (event.timestamp - mTimeStamp) * NS2S;
                        mAngle[0] += event.values[0] * dT;
                        mAngle[1] += event.values[1] * dT;
                        float angleX = (float) Math.toDegrees(mAngle[0]);
                        float angleY = (float) Math.toDegrees(mAngle[1]);
                        float dx = angleX - mPreviousXs;
                        float dy = angleY - mPreviousYs;
                        mRenderer.rotateX(-dy);
                        mRenderer.rotateY(-dx);
                        mSurfaceView.requestRender();

                        mPreviousYs = angleY;
                        mPreviousXs = angleX;
                    }
                    mTimeStamp = event.timestamp;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        mSensorMgr = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
        mGyroscopeSensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorMgr.registerListener(mSensorListener, mGyroscopeSensor, 0, 0);


    }

    private SensorManager mSensorMgr;
    private Sensor mGyroscopeSensor;
    private float mPreviousXs, mPreviousYs;
    private float mTimeStamp;
    private float mAngle[] = new float[3];
    private static final double NS2S = 1.0 / 1000000000.0;
    private SensorEventListener mSensorListener;


    private GLSurfaceView mSurfaceView;
    private SphereRenderer mRenderer;
    private MediaCodec mVideoDecoder;
    private MediaCodec mAudioDecoder;
    private String mVideoFormat;
//    private ArrayBlockingQueue mVideoFrameQueue = new ArrayBlockingQueue(20);
//    private ArrayBlockingQueue mAudioFrameQueue = new ArrayBlockingQueue(20);
/*private LinkedBlockingQueue mVideoFrameQueue=new LinkedBlockingQueue(50);
    private LinkedBlockingQueue mAudioFrameQueue=new LinkedBlockingQueue(50);*/
    private ArrayBlockingQueue<Frame> mVideoFrameQueue = new ArrayBlockingQueue<Frame>(20);
    private ArrayBlockingQueue<Frame> mAudioFrameQueue = new ArrayBlockingQueue<Frame>(20);
    private long mStartPts = -1;


    float[] mRotationMatrix = new float[16];
    private boolean mPressed = false;
    private float mStartX;
    private float mStartY;
    private float mLastDistance;


    private long mVideoPts;
    private long mAudioPts;

    class Frame{
        Frame(byte[] data, long pts){
            this.data = data;
            this.pts = pts;
        }
        public byte[] data;
        public long pts;
    }

    public boolean pushVideoFrame(byte[] frame,long videoPts) {
        mVideoPts=videoPts;
        if(mStartPts < 0) mStartPts = videoPts;
        if (mVideoFrameQueue.remainingCapacity() > 0) {
            System.out.println("video"+mVideoFrameQueue.remainingCapacity());
            return mVideoFrameQueue.offer(new Frame(frame, (videoPts-mStartPts)*1000));
        } else {
            return false;
        }
    }

    public boolean pushAudioFrame(byte[] frame,long audioPts) {
        mAudioPts=audioPts;
        if(mStartPts < 0) mStartPts = audioPts;
        if (mAudioFrameQueue.remainingCapacity() > 0) {
            System.out.println("Audio"+mAudioFrameQueue.remainingCapacity());
            return mAudioFrameQueue.offer(new Frame(frame, (audioPts-mStartPts)*1000));
        } else {
            return false;
        }
    }


    private VideoBufferThread mVideoBufferThread;

    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
        if(codec==mVideoDecoder){
            ByteBuffer inputBuffer=codec.getInputBuffer(index);
            Frame frame = new Frame(new byte[0], 0);
            try {
                frame =  mVideoFrameQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mVideoFrameQueue.remove(frame);
            inputBuffer.put(frame.data, 0, frame.data.length);
            mVideoDecoder.queueInputBuffer(index, 0, frame.data.length, frame.pts, 0);
        }
        if(codec==mAudioDecoder){
            Frame frame = new Frame(new byte[0], 0);
            try {
                frame = mAudioFrameQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mAudioFrameQueue.remove(frame);
            ByteBuffer inputBuffer = mAudioDecoder.getInputBuffer(index);
            inputBuffer.put(frame.data, 0, frame.data.length);
            mAudioDecoder.queueInputBuffer(index, 0, frame.data.length, frame.pts, 0);
        }

    }

    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
        if(codec==mVideoDecoder){
            Log.d("Codec", "video decode done: "+ info.presentationTimeUs);
            codec.releaseOutputBuffer(index,0);
            mSurfaceView.requestRender();
            Log.d("Codec", "video");

        }
        if(codec==mAudioDecoder){
          /*  ByteBuffer byteBuffer = mAudioDecoder.getOutputBuffer(index);
            byte[] bytes = new byte[info.size];
            byteBuffer.get(bytes);
            byteBuffer.clear();
            mAudioTrack.write(bytes, 0, bytes.length);
            mAudioDecoder.releaseOutputBuffer(index,false);*/
            Log.d("Codec", "audio decode done: "+info.presentationTimeUs);
            ByteBuffer audioByteBuffer=codec.getOutputBuffer(index);
            mMediaSync.queueAudio(audioByteBuffer,index,0);
            Log.d("Codec", "audio");

        }


    }

    @Override
    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

    }

    @Override
    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

    }
//----------------------------------------------------------------------------------------------------



    private class VideoBufferThread extends Thread {
        private Boolean flag;

        @Override
        public void run() {
            if(mAudioPts>mVideoPts){
                try {
                    Thread.sleep(mAudioPts-mVideoPts);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
                while(true){
                    if (flag) {
                        try {
                            int inIndex = mVideoDecoder.dequeueInputBuffer(0);
                            if (inIndex < 0) continue;
                            Frame frame =  mVideoFrameQueue.take();
                            mVideoFrameQueue.remove(frame);
                            ByteBuffer inputBuffer = mVideoDecoder.getInputBuffer(inIndex);
                            inputBuffer.put(frame.data, 0, frame.data.length);
                            mVideoDecoder.queueInputBuffer(inIndex, 0, frame.data.length, 0, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        return;
                    }
                }

            }

    }

    private VideoDecoderThread mVideoDecoderThread;

    private class VideoDecoderThread extends Thread {
        private boolean flag;
        @Override
        public void run() {

            while (true) {
                if(flag){
                    try {
                        long startTime = System.nanoTime();
                        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                        int outIndex = mVideoDecoder.dequeueOutputBuffer(info, 0);
                        if (outIndex < 0) continue;
                        mVideoDecoder.releaseOutputBuffer(outIndex, mVideoPts);
                        mSurfaceView.requestRender();

                        long endTime = System.nanoTime();
                        long delta = (endTime - startTime)/1000000;

                        if (delta < 33) {
                            try {
                                Thread.sleep(33 - delta);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }else {
                    return;
                }
            }
        }
    }
        private MediaSync mMediaSync;

        private MediaSync.Callback mMediaSyncCallback=new MediaSync.Callback() {
            @Override
            public void onAudioBufferConsumed(@NonNull MediaSync sync, @NonNull ByteBuffer audioBuffer, int bufferId) {
                mAudioDecoder.releaseOutputBuffer(bufferId,false);
            }
        };


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        try {

            mMediaSync=new MediaSync();
            //mMediaSync.setSurface(mRenderer.getSurface());
            //Surface inputSurface=mMediaSync.createInputSurface();

            int min=AudioTrack.getMinBufferSize(48000,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);
            mAudioTrack=new AudioTrack(AudioManager.STREAM_MUSIC,48000, AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT, min,AudioTrack.MODE_STREAM);
            mMediaSync.setAudioTrack(mAudioTrack);
            mAudioTrack.play();


            mVideoDecoder = MediaCodec.createDecoderByType(mVideoFormat);
            MediaFormat videoFormat = MediaFormat.createVideoFormat(mVideoFormat, 1920, 1080);
           //mVideoDecoder.configure(videoFormat, mRenderer.getSurface(), null, 0);
            mVideoDecoder.configure(videoFormat,mRenderer.getSurface(),null,0);
            mVideoDecoder.setCallback(this);
            mVideoDecoder.start();


            mAudioDecoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 48000, AudioFormat.CHANNEL_IN_STEREO);
            audioFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            //audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS);
            //audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            byte[] data = {(byte) 0x11, (byte) 0x90};
            ByteBuffer csd_0 = ByteBuffer.wrap(data);
            audioFormat.setByteBuffer("csd-0", csd_0);
            mAudioDecoder.configure(audioFormat, null, null, 0);
            mAudioDecoder.setCallback(this);
            mAudioDecoder.start();


           // mVideoBufferThread = new VideoBufferThread();
           // mVideoBufferThread.flag = true;
           // mVideoBufferThread.start();

           // mVideoDecoderThread = new VideoDecoderThread();
           // mVideoDecoderThread.flag=true;
           // mVideoDecoderThread.start();

           // mAudioBufferThread = new AudioBufferThread();
           // mAudioBufferThread.flag = true;
           // mAudioBufferThread.start();

          //  mAudioDecoderThread = new AudioDecoderThread();
          //  mAudioDecoderThread.flag=true;
           // mAudioDecoderThread.start();

            mMediaSync.setCallback(mMediaSyncCallback,null);
            mMediaSync.setPlaybackParams(new PlaybackParams().setSpeed(1.0f));


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mVideoBufferThread.flag = false;
        mVideoDecoderThread.flag=false;
        mVideoFrameQueue.clear();
        mVideoDecoder.stop();
        mVideoDecoder.release();

        mAudioBufferThread.flag = false;
        mAudioDecoderThread.flag=false;
        mAudioFrameQueue.clear();
        mAudioDecoder.stop();
        mAudioDecoder.release();

    }


    private AudioTrack mAudioTrack;
    private AudioDecoderThread mAudioDecoderThread;
    private AudioBufferThread mAudioBufferThread;


    private class AudioBufferThread extends Thread {
        private boolean flag;

        @Override
        public void run() {

               if(mAudioPts<mVideoPts){
                try {
                    Thread.sleep(mVideoPts-mAudioPts);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (true) {
                if (flag) {
                    try {
                        int inIndex = mAudioDecoder.dequeueInputBuffer(0);
                        if (inIndex < 0) continue;

                        Frame frame =  mAudioFrameQueue.take();
                        mAudioFrameQueue.remove(frame);
                        byte [] data = frame.data;
                        ByteBuffer inputBuffer = mAudioDecoder.getInputBuffer(inIndex);
                        inputBuffer.put(data, 0, data.length);
                        mAudioDecoder.queueInputBuffer(inIndex, 0, data.length, 0, 0);
                        // Thread.sleep(21);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    return;
                }
            }
        }
    }



    private class AudioDecoderThread extends Thread {
        private boolean flag;

        @Override
        public void run() {

            while (true) {
                if (flag) {
                    try {
                        long startTime = System.nanoTime();
                        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                        int outIndex = mAudioDecoder.dequeueOutputBuffer(info, 0);
                        if (outIndex < 0) continue;
                        ByteBuffer byteBuffer = mAudioDecoder.getOutputBuffer(outIndex);
                        byte[] bytes = new byte[info.size];
                        byteBuffer.get(bytes);
                        byteBuffer.clear();
                        mAudioTrack.write(bytes, 0, bytes.length);
                        mAudioDecoder.releaseOutputBuffer(outIndex, false);
                        long endTime = System.nanoTime();
                        long delta = (endTime - startTime)/1000000;
                        System.out.println(delta);
                        if(delta<21){
                            Thread.sleep(21-delta);
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                } else {
                    return;
                }
            }
        }
    }
}
