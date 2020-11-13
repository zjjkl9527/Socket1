package com.example.trafficpolice;

import android.provider.Settings;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

public class MessageClient {
    private final static String IP="192.168.0.122";
    private final static int PORT=8003;
    private SendThread mSender;
    private Socket mSocket;
    private ArrayBlockingQueue mSendQueue = new ArrayBlockingQueue(20);

    public boolean connect(){
        mSocket=new Socket();
        try {
            mSocket.connect(new InetSocketAddress(IP,PORT));
            mSender=new SendThread();
            mSender.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mSocket.isConnected();
    }

    public void close(){
        try {
            mSender.receiveFlag=false;
            mSocket.getOutputStream().flush();
            mSocket.getOutputStream().close();
            mSocket.close();
            mSendQueue.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void sendMessage(String key, double val){
        final long headLength = 8 * 2 + key.length();

        byte[] buffer = new byte[(int)headLength + 8];

        for(int i=0; i<8; ++i){
            buffer[i] = (byte)(headLength >> (7 - i) * 8);
        }

        final long keyLength = key.length();
        for(int i=0; i<8; ++i){
            buffer[8+i] = (byte)(keyLength >> (7 - i) * 8);
        }

        byte[] strByte = key.getBytes();
        System.arraycopy(strByte, 0, buffer, 16, strByte.length);

        long longVal = Double.doubleToRawLongBits(val);
        for(int i=0; i<8; ++i){
            buffer[16+strByte.length+i] = (byte)(longVal >> (7 - i) * 8);
        }
        mSendQueue.offer(buffer);

    }

    private class SendThread extends Thread {
        private OutputStream mOutputStream;
        private boolean receiveFlag=true;

        @Override
        public void run() {
            super.run();
            try {
                mOutputStream=mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            while(true){
                if(receiveFlag){
                    try {
                        byte[] data = (byte[]) mSendQueue.take();
                        mOutputStream.write(data);
                        mSendQueue.remove(data);
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
