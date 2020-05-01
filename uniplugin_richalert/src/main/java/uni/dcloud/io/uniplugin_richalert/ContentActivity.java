package uni.dcloud.io.uniplugin_richalert;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import uni.dcloud.io.uniplugin_richalert.floatwindow.BaseActivity;
import uni.dcloud.io.uniplugin_richalert.floatwindow.fw_permission.FloatWinPermissionCompat;
import uni.dcloud.io.uniplugin_richalert.service.RecordListener;
import uni.dcloud.io.uniplugin_richalert.service.RecordService;
import uni.dcloud.io.uniplugin_richalert.utils.CommonUtil;
import uni.dcloud.io.uniplugin_richalert.utils.PermissionUtils;
import uni.dcloud.io.uniplugin_richalert.utils.ToastUtil;

import static uni.dcloud.io.uniplugin_richalert.utils.PermissionUtils.PERMISSION_CODE;

public class ContentActivity extends BaseActivity implements View.OnClickListener {

    public static final int OVERLAY_CODE = 124;
    private static List<String> needPermissions =new ArrayList<>();
    private PermissionUtils permissionUtils= null;
    //静态块中初始化所需要的权限
    static {
        needPermissions.add(Manifest.permission.RECORD_AUDIO);
        needPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private RelativeLayout contentWrap;
    private TextView tvTime;
    private EditText etCaseNumber;
    private Button btnStart,btnPause,btnResume,btnEnd;
    private String caseNumber;//案件号

    //监听home键和任务列表键的广播
    private MyReceiver homeReceiver;
    //录屏的服务
    private RecordService recordService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RecordService.RecordBinder recordBinder = (RecordService.RecordBinder) service;
            recordService = recordBinder.getRecordService();
            recordService.addRecordListener(recordListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ToastUtil.showShort(mActivity,"录屏失败");
        }
    };
    //录屏的监听
    private RecordListener recordListener = new RecordListener() {
        @Override
        public void onStartRecord() {
            etCaseNumber.setEnabled(false);
            btnStart.setEnabled(false);
            btnEnd.setEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                btnPause.setEnabled(true);
                btnResume.setEnabled(false);
            }
        }

        @Override
        public void onPauseRecord() {
            btnPause.setEnabled(false);
            btnResume.setEnabled(true);
        }

        @Override
        public void onResumeRecord() {
            btnPause.setEnabled(true);
            btnResume.setEnabled(false);
        }

        @Override
        public void onStopRecord() {
            etCaseNumber.setEnabled(true);
            etCaseNumber.setText("");
            tvTime.setText("00:00");
            caseNumber="";
            btnStart.setEnabled(true);
            btnEnd.setEnabled(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                btnPause.setEnabled(false);
                btnResume.setEnabled(false);
            }
            ToastUtil.showShort(mActivity,"录屏成功，已保存");
        }

        @Override
        public void onRecording(String time) {
            tvTime.setText(time);
        }
    };

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_content;
    }

    @Override
    protected void initView() {
        //将activity设置成透明的dialog
        Window window = getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
        window.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            int option = window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            window.getDecorView().setSystemUiVisibility(option);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        RelativeLayout rlClose = findViewById(R.id.rl_close);
        contentWrap = findViewById(R.id.content_wrap);
        tvTime = findViewById(R.id.tv_time);
        etCaseNumber = findViewById(R.id.et_case_number);
        btnStart = findViewById(R.id.btn_start);
        btnPause = findViewById(R.id.btn_pause);
        btnResume = findViewById(R.id.btn_resume);
        btnEnd = findViewById(R.id.btn_end);
        rlClose.setOnClickListener(this);
        contentWrap.setOnClickListener(this);
        btnStart.setOnClickListener(this);
        btnEnd.setOnClickListener(this);
        btnStart.setEnabled(true);
        btnEnd.setEnabled(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            btnPause.setVisibility(View.VISIBLE);
            btnResume.setVisibility(View.VISIBLE);
            btnPause.setEnabled(false);
            btnResume.setEnabled(false);
            btnPause.setOnClickListener(this);
            btnResume.setOnClickListener(this);
        }
    }

    //是否显示，配合广播使用
    private boolean isShow=false;
    @Override
    protected void onResume() {
        super.onResume();
        isShow = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isShow = false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        clearScreenOn();
        closeFloatWindow();
        contentWrap.setScaleX(1);
        contentWrap.setScaleY(1);
    }

    @Override
    protected void initData() {
        //获取屏幕宽高
        CommonUtil.init(mActivity);

        //6.0以上的权限申请
        permissionUtils=new PermissionUtils(this);
        permissionUtils.request(needPermissions,new PermissionUtils.CallBack() {
            @Override
            public void grantAll() {
                checkPermissionAndShow();
            }
        });

        //注册广播
        homeReceiver = new MyReceiver();
        IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(homeReceiver, homeFilter);
        //开启服务
        Intent intent = new Intent(this, RecordService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    //悬浮点击
    @Override
    protected void clickFloatView(View view) {
        Intent intent = new Intent(mActivity.getApplicationContext(), ContentActivity.class);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.rl_close) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(contentWrap, "scaleX", 1, 0);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(contentWrap, "scaleY", 1, 0);
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(scaleX, scaleY);
            animatorSet.setDuration(300);
            animatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {

                    moveTaskToBack(true);
                    showFloatWindowDelay();
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animatorSet.start();
        } else if (id == R.id.content_wrap) {
        } else if (id == R.id.btn_start) {//开始
            caseNumber = etCaseNumber.getText().toString().trim();
            if (TextUtils.isEmpty(caseNumber)) {
                ToastUtil.showShort(mActivity, "案件号不能为空");
                return;
            }
            startScreenRecord();
        } else if (id == R.id.btn_pause) {//暂停
            if (recordService != null && recordService.ismIsRunning()) {
                recordService.pauseRecord();
            }
        } else if (id == R.id.btn_resume) {//继续
            if (recordService != null && !recordService.ismIsRunning()) {
                recordService.resumeRecord();
            }
        } else if (id == R.id.btn_end) {//结束
            if (recordService != null && recordService.isReady()) {
                recordService.stopRecord();
            }
        }
    }

    private void startScreenRecord() {
        if (recordService != null && !recordService.ismIsRunning()){
            if (!recordService.isReady()){
                MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                        getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                if (mediaProjectionManager != null){
                    Intent intent = mediaProjectionManager.createScreenCaptureIntent();
                    PackageManager packageManager = getPackageManager();
                    if (packageManager.resolveActivity(intent,PackageManager.MATCH_DEFAULT_ONLY) != null){
                        //存在录屏授权的Activity
                        startActivityForResult(intent,OVERLAY_CODE);
                    }else {
                        ToastUtil.showShort(mActivity,"暂时无法录制");
                    }
                }
            } else {
                recordService.startRecord(caseNumber);
            }
        }
    }

    //获取用户允许录屏后，设置必要的数据
    private void setUpData(int resultCode,Intent resultData) {
        if (recordService != null && !recordService.ismIsRunning()){
            recordService.setResultData(resultCode,resultData);
            recordService.startRecord(caseNumber);
        }
    }

    protected void checkPermissionAndShow() {
        // 检查是否已经授权悬浮
        if (FloatWinPermissionCompat.getInstance().check(mActivity)) {

        } else {
            // 授权提示
            new AlertDialog.Builder(mActivity)
                    .setCancelable(false)
                    .setTitle("悬浮窗权限未开启")
                    .setMessage("你的手机没有授权" + getString(R.string.app_name) + "获得悬浮窗权限，悬浮窗功能将无法正常使用")
                    .setPositiveButton("开启", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 显示授权界面
                            try {
                                FloatWinPermissionCompat.getInstance().apply(mActivity);
                                dialog.dismiss();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .show();
        }
    }

//    private long exitTime = 0;
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
//            if ((System.currentTimeMillis() - exitTime) > 2000) {
//                ToastUtil.showShort(mActivity,"再次点击退出程序");
//                exitTime = System.currentTimeMillis();
//            } else {
//                finish();
//            }
//            return true;
//        }
//        return super.onKeyDown(keyCode, event);
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PERMISSION_CODE){
            permissionUtils.request(needPermissions,new PermissionUtils.CallBack() {
                @Override
                public void grantAll() {
                    checkPermissionAndShow();
                }
            });
        }else if (requestCode == OVERLAY_CODE && resultCode == Activity.RESULT_OK){
            setUpData(resultCode,data);
        } else {
            ToastUtil.showShort(this,"拒绝录屏");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_CODE){
            permissionUtils.onRequestPermissionsResult(requestCode,permissions,grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(homeReceiver);
        unbindService(mServiceConnection);
        super.onDestroy();
    }

    private class MyReceiver extends BroadcastReceiver {
        private final String SYSTEM_DIALOG_REASON_KEY = "reason";
        private final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        private final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (reason == null)
                    return;

                // Home键
                if (reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY) && isShow)
                    showFloatWindowDelay();

                // 最近任务列表键
                if (reason.equals(SYSTEM_DIALOG_REASON_RECENT_APPS) && isShow)
                    showFloatWindowDelay();
            }
        }
    }

}