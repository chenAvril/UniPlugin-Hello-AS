package com.chen.recordscreen;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;

import com.chen.recordscreen.service.RecordListener;
import com.chen.recordscreen.service.RecordService;
import com.chen.recordscreen.utils.CommonUtil;
import com.chen.recordscreen.utils.PermissionUtils;
import com.chen.recordscreen.utils.ToastUtil;
import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.BIND_AUTO_CREATE;
import static com.chen.recordscreen.utils.PermissionUtils.PERMISSION_CODE;

public class RecordScreenModule extends WXSDKEngine.DestroyableModule {

    private final int OVERLAY_CODE = 124;
    private List<String> needPermissions = new ArrayList<>();
    private PermissionUtils permissionUtils = null;

    //录屏的服务
    private RecordService recordService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RecordService.RecordBinder recordBinder = (RecordService.RecordBinder) service;
            recordService = recordBinder.getRecordService();
            recordService.addRecordListener(recordListener);
            startScreenRecord();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (recordService != null && recordService.isReady()) {
                recordService.stopRecord();
            }
            ToastUtil.showShort(mActivity, "录屏失败");
        }
    };

    //录屏的监听
    private RecordListener recordListener = new RecordListener() {
        @Override
        public void onStartRecord() {
            ToastUtil.showShort(mActivity, "开始录屏");
        }

        @Override
        public void onStopRecord(String path) {
            try {
                if (mWXSDKInstance.getContext() instanceof Activity) {
                    JSONObject result = new JSONObject();
                    result.put("type", "filePath");
                    result.put("value", path);
                    jsCallback.invokeAndKeepAlive(result);
                    ToastUtil.showShort(mActivity, "录屏成功，已保存");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    };

    public boolean isAudio = true;//是否开启音频
    public double quality = 1;//录屏质量高低

    @JSMethod(uiThread = true)
    public void init(JSONObject options) {
        try {
            if (mWXSDKInstance.getContext() instanceof Activity) {
                isAudio = options.getBoolean("audio");
                quality = options.getDouble("quality");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Activity mActivity;

    @JSMethod(uiThread = true)
    public void startRecord() {
        if (mWXSDKInstance.getContext() instanceof Activity) {
            mActivity = (Activity) mWXSDKInstance.getContext();
//            Log.e("startRecord","isAudio===>"+isAudio);
//            isAudio=!isAudio;
            needPermissions.add(Manifest.permission.RECORD_AUDIO);
            needPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            //获取屏幕宽高
            CommonUtil.init(mActivity);
            //6.0以上的权限申请
            permissionUtils = new PermissionUtils(mActivity);
            permissionUtils.request(needPermissions, new PermissionUtils.CallBack() {
                @Override
                public void grantAll() {
                    if (!isServiceRunning()) {
                        Intent intent = new Intent(mActivity, RecordService.class);
                        mActivity.bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
                    }
                    startScreenRecord();
                }
            });
//            if (timeCompare()) {
//            } else {
//                ToastUtil.showShort(mActivity, "试用期结束啦");
//            }
        }
    }

    private JSCallback jsCallback;

    @JSMethod(uiThread = true)
    public void stopRecord(JSCallback callback) {
        jsCallback = callback;
        if (recordService != null && recordService.isReady()) {
            recordService.stopRecord();
        }
        mActivity.unbindService(mServiceConnection);
    }

    private void startScreenRecord() {
        if (recordService != null && !recordService.ismIsRunning()) {
            if (!recordService.isReady()) {
                MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                        mActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                if (mediaProjectionManager != null) {
                    Intent intent = mediaProjectionManager.createScreenCaptureIntent();
                    PackageManager packageManager = mActivity.getPackageManager();
                    if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                        //存在录屏授权的Activity
                        mActivity.startActivityForResult(intent, OVERLAY_CODE);
                    } else {
                        ToastUtil.showShort(mActivity, "暂时无法录制");
                    }
                }
            } else {
                recordService.startRecord(isAudio, quality);
            }
        }
    }

    //获取用户允许录屏后，设置必要的数据
    private void setUpData(int resultCode, Intent resultData) {
        if (recordService != null && !recordService.ismIsRunning()) {
            recordService.setResultData(resultCode, resultData);
            recordService.startRecord(isAudio, quality);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_CODE) {
            permissionUtils.request(needPermissions, new PermissionUtils.CallBack() {
                @Override
                public void grantAll() {
                    if (!isServiceRunning()) {
                        Intent intent = new Intent(mActivity, RecordService.class);
                        mActivity.bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
                    }
                    startScreenRecord();
                }
            });
        } else if (requestCode == OVERLAY_CODE && resultCode == Activity.RESULT_OK) {
            setUpData(resultCode, data);
        } else {
            ToastUtil.showShort(mActivity, "拒绝录屏");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            permissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void destroy() {
        if (isServiceRunning()) {
//            Log.e("111","===>destroy");
            if (recordService != null && recordService.isReady()) {
                recordService.stopRecord();
            }
            mActivity.unbindService(mServiceConnection);
            permissionUtils = null;
            needPermissions = null;
        }
    }

    private boolean isServiceRunning() {
        ActivityManager am = (ActivityManager) mActivity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> info = am.getRunningServices(0x7FFFFFFF);
        if (info == null || info.size() == 0) return false;
        for (ActivityManager.RunningServiceInfo aInfo : info) {
            if ("com.chen.recordscreen.service.RecordService".equals(aInfo.service.getClassName()))
                return true;
        }
        return false;
    }

    @SuppressLint("SimpleDateFormat")
    private boolean timeCompare() {
        boolean b = false;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            long startLong = System.currentTimeMillis();//开始时间
            Date date2 = dateFormat.parse("2020-05-29 00:00:00");//结束时间
            // 1 结束时间小于开始时间  3 结束时间大于开始时间
            b = startLong < date2.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return b;
    }


}
