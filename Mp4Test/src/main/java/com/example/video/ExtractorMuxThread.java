package com.example.video;


import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ExtractorMuxThread extends Thread {
    @Override
    public void run() {
        super.run();
        mixer();
    }
    public void mixer() {
        MediaExtractor videoExtractor = null;
        MediaExtractor audioExtractor = null;
        MediaMuxer mixMediaMuxer = null;
        File mFile=new File("/storage/emulated/0/VideoSave/video4.mp4");
        //String outputVideoFilePath ="/storage/emulated/0/VedioSave/2020.09.30.09.49.53.h265";
       // String outputAudioFilePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/qishi.aac";
        String outputFilePath = "/storage/emulated/0/VideoSave/mixer.mp4";

        try {
            videoExtractor = new MediaExtractor();
            //System.out.println(mFile.getAbsolutePath());
            videoExtractor.setDataSource(mFile.getAbsolutePath());
            int videoIndex = -1;
            MediaFormat videoTrackFormat = null;
            int trackCount = videoExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                videoTrackFormat = videoExtractor.getTrackFormat(i);
                if (videoTrackFormat.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
                    videoIndex = i;
                    break;
                }
            }

     /*       audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(outputAudioFilePath);
            int audioIndex = -1;
            MediaFormat audioTrackFormat = null;
            trackCount = audioExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                audioTrackFormat = audioExtractor.getTrackFormat(i);
                if (audioTrackFormat.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                    audioIndex = i;
                    break;
                }
            }*/

            videoExtractor.selectTrack(videoIndex);
           // audioExtractor.selectTrack(audioIndex);

            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();

            mixMediaMuxer = new MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int videoTrackIndex = mixMediaMuxer.addTrack(videoTrackFormat);
            //int audioTrackIndex = mixMediaMuxer.addTrack(audioTrackFormat);

            mixMediaMuxer.start();

            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 1024);
            long videotime;
            long audiotime;

            {
                videoExtractor.readSampleData(byteBuffer, 0);
                if (videoExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                    videoExtractor.advance();
                }
                videoExtractor.readSampleData(byteBuffer, 0);
                long sampleTime = videoExtractor.getSampleTime();
                videoExtractor.advance();
                videoExtractor.readSampleData(byteBuffer, 0);
                long sampleTime1 = videoExtractor.getSampleTime();
                videoExtractor.advance();
                videotime = Math.abs(sampleTime - sampleTime1);
            }

   /*         {
                audioExtractor.readSampleData(byteBuffer, 0);
                if (audioExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                    audioExtractor.advance();
                }
                audioExtractor.readSampleData(byteBuffer, 0);
                long sampleTime = audioExtractor.getSampleTime();
                audioExtractor.advance();
                audioExtractor.readSampleData(byteBuffer, 0);
                long sampleTime1 = audioExtractor.getSampleTime();
                audioExtractor.advance();

                audiotime = Math.abs(sampleTime - sampleTime1);
            }*/

            videoExtractor.unselectTrack(videoIndex);
            videoExtractor.selectTrack(videoIndex);

            while (true) {
                int data = videoExtractor.readSampleData(byteBuffer, 0);
                if (data < 0) {
                    break;
                }
                videoBufferInfo.size = data;
                videoBufferInfo.presentationTimeUs += videotime;
                videoBufferInfo.offset = 0;
                videoBufferInfo.flags = videoExtractor.getSampleFlags();

                mixMediaMuxer.writeSampleData(videoTrackIndex, byteBuffer, videoBufferInfo);
                videoExtractor.advance();
            }

    /*       while (true) {
                int data = audioExtractor.readSampleData(byteBuffer, 0);
                if (data < 0) {
                    break;
                }
                audioBufferInfo.size = data;
                audioBufferInfo.presentationTimeUs += audiotime;
                audioBufferInfo.offset = 0;
                audioBufferInfo.flags = audioExtractor.getSampleFlags();

                mixMediaMuxer.writeSampleData(audioTrackIndex, byteBuffer, audioBufferInfo);
                audioExtractor.advance();
            }*/
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (mixMediaMuxer != null) {
                mixMediaMuxer.stop();
                mixMediaMuxer.release();
            }
            if (videoExtractor != null){
                videoExtractor.release();
            }
            if (audioExtractor != null){
                audioExtractor.release();
            }
        }
    }
}
