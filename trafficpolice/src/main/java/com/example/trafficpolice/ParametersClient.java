package com.example.trafficpolice;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ParametersClient {


private Callback mCallback;
private BufferedReader mBufferReader;
private static final int PORT=8001;
private static final String IP="192.168.0.122";
private Socket mSocket;
private receiveJsonThread mReceiveJsonThread;


 public ParametersClient(Callback callback){
      mCallback=callback;
 }

        public boolean connect(){
            mSocket=new Socket();
            try {
                mSocket.connect(new InetSocketAddress(IP, PORT));
                mReceiveJsonThread=new receiveJsonThread();
                mReceiveJsonThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return mSocket.isConnected();

 }


   private class receiveJsonThread extends Thread{
     private InputStream mInputStream;

       @Override
       public void run() {
           super.run();
           try {
               mInputStream=mSocket.getInputStream();
           } catch (IOException e) {
               e.printStackTrace();
           }
           try {
                   mBufferReader=new BufferedReader(new InputStreamReader(mInputStream));
                   StringBuilder info=new StringBuilder();
                   String temp;
                   while((temp=mBufferReader.readLine())!=null){
                        info.append(temp);
                   }
                   mCallback.parserJson(String.valueOf(info));
               } catch (IOException | JSONException e) {
                   e.printStackTrace();
               }finally {
                   try {
                       if(mBufferReader!=null){
                        mBufferReader.close();
                        mSocket.close();
                       }
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }
           }
        }


   public   interface Callback{
        void parserJson(String jsonStr) throws JSONException;
    }


}

