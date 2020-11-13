package com.example.trafficpolice;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;


public class DisplayActivity extends AppCompatActivity {


    private Chronometer mChronometer;
    private GLSurfaceView mGLSurfaceView;
    private SurfaceView mSurfaceView;
    private ImageView iv_redPoint;
    private AnimationDrawable drawable;
    private SpherePlayer mSpherePlayer;
    private Player mPlayer;
    private Button mBtnModeChange;
    private CheckBox cb_send;

    private MessageClient mMessageClient=new MessageClient();


    private ParametersClient mParametersClient=new ParametersClient(new ParametersClient.Callback() {
        @Override
        public void parserJson(String jsonStr) throws JSONException {
            JSONObject mJsonObject = new JSONObject(jsonStr);
            JSONObject mode=mJsonObject.getJSONObject("Mode");
            double blend=mode.getDouble("Blend");
            if(blend==-1.0){
                mHandler.sendEmptyMessageDelayed(1,0);
            }else if (blend==1.0){
                mHandler.sendEmptyMessageDelayed(2,0);
            }
        }
    });


    private MediaClient mMediaClient=new MediaClient(new MediaClient.Callback() {
        @Override
        public void onReceive(byte[] data) {
            if(mSpherePlayer != null&&mGLSurfaceView.getVisibility()==View.VISIBLE){
                mSpherePlayer.pushFrame(data);
            }
            if(mPlayer!=null&&mGLSurfaceView.getVisibility()==View.GONE){
                mPlayer.pushFrame(data);
            }
            mFileWriter.pushData(data);
        }
    },mMessageClient);

    private FileWriter mFileWriter = new FileWriter();
    private void record(Boolean mStartFlag){
        File[] files;
        String filePath;
        files = getExternalFilesDirs(Environment.MEDIA_MOUNTED);
        if(files.length>1){
            filePath =files[1]+"/VideoSave";
        }else {
            filePath="/storage/emulated/0/VideoSave";
        }
        if(mStartFlag){
            mFileWriter.startWrite(filePath);
        }else{
            mFileWriter.stopWrite();
        }
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dis_play);

        mChronometer = findViewById(R.id.mChronometer);
        iv_redPoint = findViewById(R.id.iv_redPoint);
        drawable = (AnimationDrawable) (iv_redPoint).getDrawable();
        cb_send = findViewById(R.id.cb_send);

        mBtnModeChange = findViewById(R.id.btn_modeChange);


        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        final int REQUEST_EXTERNAL_STORAGE = 1;
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        int permission = ActivityCompat.checkSelfPermission(DisplayActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DisplayActivity.this, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }


        cb_send.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    record(true);
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                    drawable.start();
                } else {
                    record(false);
                    mChronometer.stop();
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    drawable.stop();
                    drawable.selectDrawable(0);

                }
            }
        });


        mGLSurfaceView = findViewById(R.id.GLsv);
        mSpherePlayer = new SpherePlayer(mGLSurfaceView, MediaFormat.MIMETYPE_VIDEO_HEVC);

        mSurfaceView = findViewById(R.id.sv);
        mPlayer = new Player(mSurfaceView, MediaFormat.MIMETYPE_VIDEO_HEVC);


        mBtnModeChange.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (mGLSurfaceView.getVisibility() == View.VISIBLE) {
                    mMessageClient.sendMessage("ModeChange", -1.0);
                    mGLSurfaceView.setVisibility(View.GONE);
                    mSurfaceView.setVisibility(View.VISIBLE);
                } else {
                    mMessageClient.sendMessage("ModeChange", 1.0);
                    mSurfaceView.setVisibility(View.GONE);
                    mGLSurfaceView.setVisibility(View.VISIBLE);
                }
            }
        });

        mGLSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });



        startConnect();
        mMediaClient.monitor();

    }

    public void startConnect(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                    mMediaClient.connect();
                    mParametersClient.connect();
            }
        }).start();

    }



    public  Handler mHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {

            if(msg.what==1&&mGLSurfaceView!=null){
                mGLSurfaceView.setVisibility(View.GONE);
            }
            if(msg.what==2&&mSurfaceView!=null){
                mSurfaceView.setVisibility(View.GONE);
            }
            return false;
        }
    });

        @Override
        protected void onRestart() {
            super.onRestart();

    }

        @Override
        protected void onPause() {
            super.onPause();
        }

        @Override
        protected void onResume() {
            super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaClient.close();
    }
}