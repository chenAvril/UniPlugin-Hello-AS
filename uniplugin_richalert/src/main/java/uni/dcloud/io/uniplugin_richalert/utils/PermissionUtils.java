package uni.dcloud.io.uniplugin_richalert.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;


/**
 * 动态申请权限
 * 可以在activity中增加如果权限被拒绝后的弹窗，在onRequestPermissionsResult中判断状态
 */
public class PermissionUtils {

    public static final int PERMISSION_CODE = 123;
    private Activity mActivity;
    private CallBack mCallBack;

    //定义一个回调接口
    public interface CallBack{
        //接受
        void grantAll();
        //拒绝
        //void denied();
    }

    //定义一个构造函数
    public PermissionUtils(Activity activity) {
        this.mActivity = activity;
    }

    //定义请求权限的方法
    public void request(List<String> needPermissions, CallBack callBack){
        mCallBack=callBack;

        //因为6.0之后才需要动态权限申请
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            //6.0之前是默认获取全部权限
            callBack.grantAll();
            return;
        }

        //判空，并抛出异常
        if (mActivity==null){
            throw  new IllegalArgumentException("activity is null.");
        }

        //将需要申请的权限，因为有些权限已经赋予
        List<String> reqPermission =new ArrayList<>();
        for (String permission:needPermissions){
            if (mActivity.checkSelfPermission(permission)!= PackageManager.PERMISSION_GRANTED){
                reqPermission.add(permission);
            }
        }

        //如果没有要授予的权限，则直接返回
        if (reqPermission.isEmpty()){
            callBack.grantAll();
            return;
        }

        //真正开始申请
        mActivity.requestPermissions(reqPermission.toArray(new String[]{}),PERMISSION_CODE);
    }

    //处理权限返回的回调
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode==PERMISSION_CODE){
            boolean grantAll = true;
            //遍历每一个授权结果
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    grantAll = false;
                    break;
                }
            }

            if (grantAll){
                mCallBack.grantAll();
            }else {
                AlertDialog dialog = new AlertDialog.Builder(mActivity)
                        .setCancelable(false)
                        .setTitle("申请权限")
                        .setMessage("这些权限很重要，否则应用无法正常运行，请立即前往设置")
                        .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + mActivity.getPackageName()));
                                mActivity.startActivityForResult(intent,PERMISSION_CODE);
                                dialog.dismiss();
                            }
                        })
                        .create();
                dialog.show();
            }
        }
    }
}
