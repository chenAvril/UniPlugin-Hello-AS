package uni.dcloud.io.uniplugin_richalert.floatwindow.float_view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import uni.dcloud.io.uniplugin_richalert.floatwindow.BaseActivity;
import uni.dcloud.io.uniplugin_richalert.floatwindow.uitls.SystemUtils;

/**
 * FloatWindowManager:管理悬浮窗视频播放
 * android.view.WindowManager$BadTokenException:
 * Unable to add window android.view.ViewRootImpl$W@123e0ab --
 * permission denied for this window 2003,type
 */

public class FloatWindowManager {
    private IFloatView floatView;
    private boolean isFloatWindowShowing = false;
    private FrameLayout contentView;
    private FloatViewParams floatViewParams;
    private WindowManager windowManager;
    private LastWindowInfo livePlayerWrapper;

    public FloatWindowManager() {
        livePlayerWrapper = LastWindowInfo.getInstance();
    }

    /**
     * 显示悬浮窗口
     */
    public synchronized void showFloatWindow(BaseActivity baseActivity) {
        if (baseActivity == null) {
            return;
        }
        Context mContext = baseActivity.getApplicationContext();
        showFloatWindow(mContext);
    }

    public synchronized void showFloatWindow(Context context) {
        if (context == null) {
            return;
        }
        try {
            isFloatWindowShowing = true;
            initFloatWindow(context);
        } catch (Exception e) {
            e.printStackTrace();
            isFloatWindowShowing = false;
        }
    }

    /**
     * 初始化悬浮窗
     */
    private void initFloatWindow(final Context mContext) {
        if (mContext == null) {
            return;
        }
        floatViewParams = initFloatViewParams(mContext);
        initSystemWindow(mContext);
        isFloatWindowShowing = true;
    }

    /**
     * 利用系统弹窗实现悬浮窗
     *
     * @param mContext
     */
    private void initSystemWindow(Context mContext) {
        windowManager = SystemUtils.getWindowManager(mContext);
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        wmParams.packageName = mContext.getPackageName();
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_SCALED
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;


        //需要权限
        if (Build.VERSION.SDK_INT >= 26) {
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.gravity = Gravity.START | Gravity.TOP;

        wmParams.width = floatViewParams.width;
        wmParams.height = floatViewParams.height;
        wmParams.x = floatViewParams.x;
        wmParams.y = floatViewParams.y;

        floatView = new FloatWindowView(mContext, floatViewParams, wmParams);

        //监听关闭悬浮窗
//        floatView.setFloatViewListener(new FloatViewListener() {
//            @Override
//            public void onClose() {
//                dismissFloatWindow();
//            }
//        });
        try {
            windowManager.addView((View) floatView, wmParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化窗口参数
     *
     * @param mContext
     * @return
     */
    private FloatViewParams initFloatViewParams(Context mContext) {
        FloatViewParams params = new FloatViewParams();
        int screenWidth = SystemUtils.getScreenWidth(mContext);
        int screenHeight = SystemUtils.getScreenHeight(mContext, false);
        int statusBarHeight = SystemUtils.getStatusBarHeight(mContext);
        Log.d("dq", "screenWidth=" + screenWidth + ",screenHeight=" + screenHeight + ",statusBarHeight=" + statusBarHeight);
        //根据实际宽高和设计稿尺寸比例适应。
        int width = SystemUtils.dip2px(mContext, 44);
//        if (winWidth <= winHeight) {
//            //竖屏比例
//            width = (int) (screenWidth * 1.0f * 1 / 3) + margin;
//        } else {//横屏比例
//            width = (int) (screenWidth * 1.0f / 2) + margin;
//        }
//        float ratio = 1.0f * winHeight / winWidth;
        int height = SystemUtils.dip2px(mContext, 44);

        //如果上次的位置不为null，则用上次的位置
        FloatViewParams lastParams = livePlayerWrapper.getLastParams();
        if (lastParams != null) {
            params.width = lastParams.width;
            params.height = lastParams.height;
            params.x = lastParams.x;
            params.y = lastParams.y;
        } else {
            params.width = width;
            params.height = height;
            params.x = (screenWidth - width);
            params.y = (screenHeight - height)/2;
        }

        params.screenWidth = screenWidth;
        params.screenHeight = screenHeight;
        params.statusBarHeight = statusBarHeight;
        return params;
    }

    public IFloatView getFloatView() {
        return floatView;
    }

    /**
     * 隐藏悬浮视频窗口
     */
    public synchronized void dismissFloatWindow() {
        if (!isFloatWindowShowing) {
            return;
        }
        try {
            isFloatWindowShowing = false;
            if (floatView != null) {
                FloatViewParams floatViewParams = floatView.getParams();
                livePlayerWrapper.setLastParams(floatViewParams);
            }
            removeWindow();

            if (contentView != null && floatView != null) {
                contentView.removeView((View) floatView);
            }
            floatView = null;
            windowManager = null;
            contentView = null;
        } catch (Exception e) {
        }
    }

    private void removeWindow() {
        if (windowManager != null && floatView != null) {
            windowManager.removeViewImmediate((View) floatView);
        }
    }
}