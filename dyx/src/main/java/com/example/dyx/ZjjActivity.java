package com.example.dyx;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.util.Objects;

public class ZjjActivity extends AppCompatActivity implements InterfaceTest.message{
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zjj);
        textView=findViewById(R.id.tv_tv);
    }

    @Override
    public void sendMessage(String s) {
        textView.setText(s);
    }
}
