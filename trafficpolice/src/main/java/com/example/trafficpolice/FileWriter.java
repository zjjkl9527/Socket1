package com.example.trafficpolice;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileWriter {

    public Boolean startWrite(String url) {
        if (!mFinished.get()) return false;

        mRunnable.set(true);
        mThread = new WriteThread(url);
        mThread.start();
        return true;
    }

    public void stopWrite() {
        mRunnable.set(false);
    }

    public void pushData(byte[] data) {
        if (mQueue.remainingCapacity()>0&&mRunnable.get()) {
                mQueue.offer(data);

        }
    }


    private WriteThread mThread = null;
    private AtomicBoolean mRunnable = new AtomicBoolean(false);
    private AtomicBoolean mFinished = new AtomicBoolean(true);
    private ArrayBlockingQueue mQueue = new ArrayBlockingQueue(10);

    class WriteThread extends Thread{
        WriteThread(String url){
            mURL = url;
        }

        private String mURL;

        @Override
        public void run() {
            super.run();
            mFinished.set(false);

            String fileName ="/"+DateUtil.getNowTime()+".h265";
            File dir = new File(mURL);
            if (!dir.exists() && !dir.isDirectory()) {//判断文件目录是否存在
               boolean mkdir= dir.mkdirs();
                System.out.println(mkdir);
            }
            File file = new File(mURL + fileName);

            BufferedOutputStream bos;

            try {
                bos = new BufferedOutputStream(new FileOutputStream(file));

            } catch (IOException e) {
                e.printStackTrace();
                mFinished.set(true);
                return;
            }

            try {
                while(mRunnable.get()){
                    byte[] data= (byte[]) mQueue.take();
                    mQueue.remove(data);
                    bos.write(data);
                }
            }catch (IOException | InterruptedException e){
                e.printStackTrace();
            }


            try {
                bos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            mFinished.set(true);
        }
    }
}
