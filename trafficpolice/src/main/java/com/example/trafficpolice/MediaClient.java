package com.example.trafficpolice;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MediaClient  {
    private static final String IP="192.168.0.122";
    private static final int PORT=8002;
    private MessageClient mMessageClient;



    public MediaClient(Callback callback,MessageClient messageClient){
        mCallback = callback;
        mMessageClient=messageClient;
    }


    public void monitor(){
        new Thread(new Runnable() {
            long preStream;
            long nextStream;
            @Override
            public void run() {
                while (true) {
                    if(mReceiver!=null){
                        preStream =mReceiver.streamLength;
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        nextStream=mReceiver.streamLength;

                        if (preStream == nextStream) {
                            close();
                            mMessageClient.close();
                            if(!connect()){
                                connect();
                            }
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
            }
        }).start();

    }

    public boolean connect() {
        try {
            mSocket=new Socket();
            mSocket.connect(new InetSocketAddress(IP,PORT));
            mMessageClient.connect();
            mReceiver=new ReceiveThread();
            mReceiver.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
         return mSocket.isConnected();
    }


    void close(){
        if(mSocket!= null){
            try {
                mReceiver.receiveFlag=false;
                mSocket.getInputStream().close();
                mSocket.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }



    public interface Callback{
        void onReceive(byte[] data);
    }



    private Socket mSocket;
    private ReceiveThread mReceiver;
    private Callback mCallback;



    private class ReceiveThread extends Thread{
        private InputStream mInputStream;
        boolean receiveFlag=true;
        long streamLength=0;


        @Override
        public void run() {
            super.run();
            try {
                mInputStream = mSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

                while(true){
                    if(receiveFlag){
                    try {
                        byte[] head = new byte[8];
                        int headOffset = 0;
                        while(headOffset < 8){
                            headOffset += mInputStream.read(head, headOffset, 8-headOffset);
                        }
                        long headStreamLength=0;
                        long endStreamLength=0;

                        for (int i = 0; i < 4; i++) {
                            endStreamLength = endStreamLength << 8;
                            endStreamLength = endStreamLength | (head[i + 4] & 0xff);
                            headStreamLength = headStreamLength << 8;
                            headStreamLength = headStreamLength | (head[i] & 0xff);
                        }

                        if (headStreamLength == endStreamLength&&headStreamLength!=0) {
                            streamLength = endStreamLength;



                            byte[] data = new byte[(int) streamLength];
                            int offset = 0;
                            while (offset < streamLength) {
                                offset += mInputStream.read(data, offset, (int) streamLength - offset);
                            }

                            mCallback.onReceive(data);
                            

                        } else {
                            close();
                            mMessageClient.close();
                            connect();
                            return;
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    }else {
                        return;
                    }
                }



        }
    }


}

