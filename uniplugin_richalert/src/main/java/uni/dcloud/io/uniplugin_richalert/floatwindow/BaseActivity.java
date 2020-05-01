package uni.dcloud.io.uniplugin_richalert.floatwindow;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import uni.dcloud.io.uniplugin_richalert.floatwindow.float_view.FloatViewListener;
import uni.dcloud.io.uniplugin_richalert.floatwindow.float_view.FloatWindowManager;
import uni.dcloud.io.uniplugin_richalert.floatwindow.float_view.IFloatView;

/**
 * Description:Activity基类
 */
public abstract class BaseActivity extends Activity {
    protected Activity mActivity;
    protected View rootView;

    private FloatWindowManager floatWindowManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;

        int layoutId = getLayoutResId();
        if (layoutId > 0) {
            rootView = LayoutInflater.from(mActivity).inflate(layoutId, null);
            setContentView(rootView);
        }

        floatWindowManager = new FloatWindowManager();

        initData();
        initView();
    }

    protected abstract int getLayoutResId();

    protected abstract void initData();

    protected abstract void initView();

    @Override
    protected void onResume() {
        super.onResume();
        //showFloatWindowDelay();
    }

    /**
     * 必须等activity创建后，view展示了再addView，否则可能崩溃
     * BadTokenException: Unable to add window --token null is not valid; is your activity running?
     */
    protected void showFloatWindowDelay() {
        if (rootView != null ) {
            rootView.removeCallbacks(floatWindowRunnable);
            rootView.post(floatWindowRunnable);
        }
    }

    private final Runnable floatWindowRunnable = new Runnable() {
        @Override
        public void run() {
            showFloatWindow();
        }
    };

    /**
     * 显示悬浮窗
     */
    protected void showFloatWindow() {
        closeFloatWindow();//如果要显示多个悬浮窗，可以不关闭，这里只显示一个
        floatWindowManager.showFloatWindow(this);
        addFloatWindowClickListener();
    }

    /**
     * 关闭悬浮窗
     */
    protected void closeFloatWindow() {
        if (rootView != null) {
            rootView.removeCallbacks(floatWindowRunnable);
        }
        if (floatWindowManager != null) {
            floatWindowManager.dismissFloatWindow();
        }
    }

    /**
     * 监听悬浮窗关闭和点击事件
     */
    private void addFloatWindowClickListener() {
        IFloatView floatView = floatWindowManager.getFloatView();
        if (floatView == null) {
            return;
        }
        //说明悬浮窗view创建了，增加屏幕常亮
        keepScreenOn();
        floatView.setFloatViewListener(new FloatViewListener() {

            @Override
            public void onClick(View view) {
                clearScreenOn();
                closeFloatWindow();
                clickFloatView(view);
            }
        });
    }

    protected abstract void clickFloatView(View view);

    /**
     * 开启屏幕常量
     */
    private void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * 清除常量模式
     */
    protected void clearScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

}
