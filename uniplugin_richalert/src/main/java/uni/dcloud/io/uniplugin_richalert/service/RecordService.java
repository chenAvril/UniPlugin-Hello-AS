package uni.dcloud.io.uniplugin_richalert.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import uni.dcloud.io.uniplugin_richalert.utils.CommonUtil;
import uni.dcloud.io.uniplugin_richalert.utils.FileUtil;
import uni.dcloud.io.uniplugin_richalert.utils.ToastUtil;

public class RecordService extends Service implements Handler.Callback {

    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;
    private VirtualDisplay mVirtualDisplay;

    private boolean mIsRunning;
    private int mRecordWidth = CommonUtil.getScreenWidth();
    private int mRecordHeight = CommonUtil.getScreenHeight();
    private int mScreenDpi = CommonUtil.getScreenDpi();

    private RecordListener listener;

    private Intent mResultData;

    //录屏文件的保存地址
    private String mRecordFilePath;
    private Handler mHandler;
    //已经录制多少秒了
    private int mRecordSeconds = 0;
    private static final int MSG_TYPE_COUNT_DOWN = 110;

    public RecordService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        mIsRunning = false;
        mMediaRecorder = new MediaRecorder();
        mHandler = new Handler(Looper.getMainLooper(),this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public boolean isReady(){
        return  mMediaProjection != null && mResultData != null;
    }

    public boolean ismIsRunning() {
        return mIsRunning;
    }

    public void setResultData(int resultCode, Intent resultData){
        mResultData = resultData;

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mProjectionManager != null){
            if (mMediaProjection == null)
                mMediaProjection = mProjectionManager.getMediaProjection(resultCode,mResultData);
        }
    }

    public void addRecordListener(RecordListener listener){
        if (listener != null){
            this.listener=listener;
        }
    }

    /**
     * 开始录屏
     */
    public void startRecord(String caseNumber) {
        if (mIsRunning) {
            return;
        }

        setUpMediaRecorder(caseNumber);
        //createVirtualDisplay();
        mMediaRecorder.start();

        if (listener != null){
            listener.onStartRecord();
        }

        mHandler.sendEmptyMessageDelayed(MSG_TYPE_COUNT_DOWN,1000);
        mIsRunning = true;
    }

    /**
     * 停止录屏
     */
    public void stopRecord() {
//        if (!mIsRunning) {
//            return;
//        }
        mIsRunning = false;

        try {
            mMediaRecorder.stop();
            mVirtualDisplay.setSurface(mMediaRecorder.getSurface());
            mMediaRecorder.reset();
            mMediaRecorder = null;
            mVirtualDisplay.release();
            mMediaProjection.stop();

        }catch (Exception e){
            e.printStackTrace();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        mMediaProjection = null;
        mHandler.removeMessages(MSG_TYPE_COUNT_DOWN);
        if (listener != null){
            listener.onStopRecord();
        }

//        if (mRecordSeconds <= 3 ){
//            FileUtil.deleteSDFile(mRecordFilePath);
//        }else {
        //通知系统图库更新
        //FileUtil.fileScanVideo(this,mRecordFilePath,mRecordWidth,mRecordHeight,mRecordSeconds);
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(mRecordFilePath))));
//        }
        mRecordSeconds = 0;
    }

    public void pauseRecord(){
        if (mMediaRecorder != null ){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mMediaRecorder.pause();
                mIsRunning = false;
                mHandler.removeMessages(MSG_TYPE_COUNT_DOWN);
                if (listener != null){
                    listener.onPauseRecord();
                }
            }
        }
    }

    public void resumeRecord(){
        if (mMediaRecorder != null ){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mMediaRecorder.resume();
                mIsRunning = true;
                mHandler.sendEmptyMessageDelayed(MSG_TYPE_COUNT_DOWN,1000);
                if (listener != null){
                    listener.onResumeRecord();
                }
            }
        }
    }

    private void setUpMediaRecorder(String caseNumber) {
        if(getSaveDirectory() == null){
            ToastUtil.showShort(this,"无存储环境");
            return;
        }
        String dirPath = getSaveDirectory()  + File.separator+"录屏视频";

        if(FileUtil.createOrExistsDir(new File(dirPath))){
            mRecordFilePath = dirPath +File.separator + setFileName(caseNumber) + ".mp4";
            if (mMediaRecorder == null){
                mMediaRecorder = new MediaRecorder();
            }

            //设置音频采集方式
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //设置视频的采集方式
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            //设置文件的输出格式
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            //设置视频输出格式
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            //设置输出文件的路径
            mMediaRecorder.setOutputFile(mRecordFilePath);
            //根据屏幕分辨率设置录制尺寸
            mMediaRecorder.setVideoSize(mRecordWidth,mRecordHeight);
            //设置要捕获的视频帧速率,注意文档的说明:
            mMediaRecorder.setVideoFrameRate(30);
            //设置音频输出格式
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            //设置录制的视频编码比特率：比特率越高，每秒传送数据就越多，画质就越清晰
            mMediaRecorder.setVideoEncodingBitRate((mRecordWidth * mRecordHeight * 3));

            //Log.e("111","mRecordWidth==="+mRecordWidth);
            //Log.e("111","mRecordHeight==="+mRecordHeight);

            try {
                mMediaRecorder.prepare();


            } catch (IOException e) {
                e.printStackTrace();
            }

            mVirtualDisplay = mMediaProjection.createVirtualDisplay("MainScreen", mRecordWidth, mRecordHeight, mScreenDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
        }
    }

    public String getRecordFilePath(){
        return mRecordFilePath;
    }

    public String getSaveDirectory() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            return null;
        }
    }

    /**
     * 获取当前时间的完整显示字符串
     */
    @SuppressLint("SimpleDateFormat")
    private String setFileName(String caseNumber) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        return caseNumber+"_"+format.format(new Date(System.currentTimeMillis()));
    }

    @SuppressLint("DefaultLocale")
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){
            case MSG_TYPE_COUNT_DOWN:{
                boolean enough = FileUtil.getSDFreeMemory() / (1024* 1024) < 4;
                if (enough){
                    //空间不足，停止录屏
                    ToastUtil.showShort(this,"存储空间不足");
                    stopRecord();
                    mRecordSeconds = 0;
                    break;
                }

                mRecordSeconds++;

                String time;
                if(mRecordSeconds<3600){
                    time = String.format("%1$02d:%2$02d",mRecordSeconds/60,mRecordSeconds%60);
                }else {
                    time = String.format("%1$d:%2$02d:%3$02d",mRecordSeconds/3600,mRecordSeconds%3600/60,mRecordSeconds%60);
                }

                if(listener!=null){
                    listener.onRecording(time);
                }

                if (mRecordSeconds < 60 * 60 ){
                    mHandler.sendEmptyMessageDelayed(MSG_TYPE_COUNT_DOWN,1000);
                } else if (mRecordSeconds == 60 * 60 ){
                    ToastUtil.showShort(this,"录制已到限定时长");
                    stopRecord();
                    mRecordSeconds = 0;
                }

                break;
            }
        }
        return true;
    }

    public class RecordBinder extends Binder {
        public RecordService getRecordService() {
            return RecordService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mHandler.removeCallbacksAndMessages(null);
        return super.onUnbind(intent);
    }
}