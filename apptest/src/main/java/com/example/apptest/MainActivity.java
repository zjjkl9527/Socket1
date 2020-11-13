package com.example.apptest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;
import android.media.MediaFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.zck.ffmpeg.Demuxer;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mGLSurfaceView=findViewById(R.id.gl_sv);
        mGLSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        mSpherePlayer=new SpherePlayer(mGLSurfaceView, MediaFormat.MIMETYPE_VIDEO_AVC,this);
        mPushRTMPFrameThread=new PushRTMPFrameThread();
        mPushRTMPFrameThread.start();


    }

    private SpherePlayer mSpherePlayer;

    private Demuxer mDemuxer=new Demuxer();
    private PushRTMPFrameThread mPushRTMPFrameThread;


    private long mVideoPts;
    private long mAudioPts;
    private boolean videoPtsFlag=true;
    private boolean audioPtsFlag=true;
    //rtmp://202.69.69.180:443/webcast/bshdlive-pc
    //rtmp://8.131.75.62/live
    public void pushRTMPFrame(){
        mDemuxer.open("rtmp://8.131.75.62/live", new Demuxer.Callback() {
            @Override
            public void onVideo(byte[] frame, long pts) {
                if(videoPtsFlag){
                    mVideoPts=pts;
                    videoPtsFlag=false;
                }
                mSpherePlayer.pushVideoFrame(frame,pts);
                // System.out.println("Video------------"+mVideoPts);
            }

            @Override
            public void onAudio(byte[] frame, long pts) {
                if(audioPtsFlag){
                    mAudioPts=pts;
                    audioPtsFlag=false;
                }

                mSpherePlayer.pushAudioFrame(frame,pts);
                // System.out.println("Audio------------"+mAudioPts);
            }
        });
    }

    private class PushRTMPFrameThread extends Thread{
        @Override
        public void run() {
            pushRTMPFrame();
            while(true){
                mDemuxer.flush();
            }
        }
    }

}
