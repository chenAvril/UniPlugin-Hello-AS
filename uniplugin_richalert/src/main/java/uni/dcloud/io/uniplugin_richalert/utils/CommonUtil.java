package uni.dcloud.io.uniplugin_richalert.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by Administrator on 2018/3/8 0008.
 */

public class CommonUtil {

    private static int mScreenWidth;
    private static int mScreenHeight;

    private static int mScreenDpi;

    public static void init(Activity activity){

        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        if (isAllScreenDevice(activity)) {
            // 是全屏
            mScreenWidth = 1080;
            mScreenHeight = 1920;
        }else {
            mScreenWidth = metrics.widthPixels;
            mScreenHeight = metrics.heightPixels;
        }
//        int statusBarHeight1 = 0;
//        //获取status_bar_height资源的ID
//        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
//        if (resourceId > 0) {
//            //根据资源ID获取响应的尺寸值
//            statusBarHeight1 = activity.getResources().getDimensionPixelSize(resourceId);
//        }

        mScreenDpi = metrics.densityDpi;
    }

    public static int getScreenWidth(){
        return mScreenWidth;
    }

    public static int getScreenHeight(){
        return mScreenHeight;
    }

    public static int getScreenDpi(){
        return mScreenDpi;
    }

    /**
     * 判断是否是全面屏
     */
    private volatile static boolean mHasCheckAllScreen;
    private volatile static boolean mIsAllScreenDevice;

    private static boolean isAllScreenDevice(Context context) {
        if (mHasCheckAllScreen) {
            return mIsAllScreenDevice;
        }
        mHasCheckAllScreen = true;
        mIsAllScreenDevice = false;
        // 低于 API 21的，都不会是全面屏。。。
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//            return false;
//        }
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            Display display = windowManager.getDefaultDisplay();
            Point point = new Point();
            display.getRealSize(point);
            float width, height;
            if (point.x < point.y) {
                width = point.x;
                height = point.y;
            } else {
                width = point.y;
                height = point.x;
            }
            if (height / width >= 1.97f) {
                mIsAllScreenDevice = true;
            }
        }
        return mIsAllScreenDevice;
    }

}