package uni.dcloud.io.uniplugin_richalert.floatwindow.float_view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import uni.dcloud.io.uniplugin_richalert.R;
import uni.dcloud.io.uniplugin_richalert.floatwindow.uitls.SystemUtils;

/**
 * FloatWindowView:悬浮窗控件V1-利用windowManger控制窗口
 */

public class FloatWindowView extends FrameLayout implements IFloatView {
    private float xInView;
    private float yInView;
    private float xInScreen;
    private float yInScreen;
    private float xDownInScreen;
    private float yDownInScreen;
    private Context mContext;
    private RelativeLayout content_wrap;

    private WindowManager mWindowManager = null;
    private WindowManager.LayoutParams mWindowParams = null;
    private FloatViewParams params = null;
    private FloatViewListener listener;
    private int statusBarHeight = 0;
    private int screenWidth;
    private int screenHeight;

    public FloatWindowView(Context context) {
        super(context);
        init();
    }

    public FloatWindowView(Context mContext, FloatViewParams floatViewParams, WindowManager.LayoutParams wmParams) {
        super(mContext);
        this.params = floatViewParams;
        this.mWindowParams = wmParams;
        init();
    }

    private void init() {
        try {
            initData();
            initView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View floatView = inflater.inflate(R.layout.float_view_layout, null);
        content_wrap = floatView.findViewById(R.id.content_wrap);
        content_wrap.setOnTouchListener(onMovingTouchListener);
        //ImageView imageView = floatView.findViewById(R.id.iv_float);
        addView(floatView);
    }


    private void initData() {
        mContext = getContext();
        mWindowManager = SystemUtils.getWindowManager(mContext);
        statusBarHeight = params.statusBarHeight;
        screenWidth = params.screenWidth;
        screenHeight = params.screenHeight - statusBarHeight;//要去掉状态栏高度
        //起点
        startX = params.x;
        startY = params.y;
    }

    private void updateViewLayoutParams(int width, int height) {
        if (content_wrap != null) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) content_wrap.getLayoutParams();
            layoutParams.height = height;
            layoutParams.width = width;
            content_wrap.setLayoutParams(layoutParams);
        }
    }

    private int startX = 0;//缩放前的x坐标
    private int startY = 0;//缩放前的y坐标

    /**
     * 更新WM的宽高大小
     */
    private synchronized void updateWindowWidthAndHeight(int width, int height) {
        if (mWindowManager != null) {
            mWindowParams.width = width;
            mWindowParams.height = height;
            mWindowManager.updateViewLayout(this, mWindowParams);
        }
    }

    private boolean isMoving = false;
    private final OnTouchListener onMovingTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return onTouchEvent2(event);
        }
    };

    private long firstClickTime;//第一次点击
    private int countClick = 0;
    private final Runnable clickRunnable = new Runnable() {
        @Override
        public void run() {
            if (countClick == 1 && canClick) {
                if (listener != null) {
                    listener.onClick(content_wrap);
                }
            }
            countClick = 0;
        }
    };
    private boolean canClick = true;//是否可以点击
    private final Runnable canClickRunnable = new Runnable() {
        @Override
        public void run() {
            canClick = true;
        }
    };

    private Handler handler = new Handler(Looper.getMainLooper());
    private int scaleCount = 1;//统计双击缩放的次数

    //@Override
    public boolean onTouchEvent2(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isMoving = false;
                xInView = event.getX();
                yInView = event.getY();
                xDownInScreen = event.getRawX();
                yDownInScreen = event.getRawY();
                xInScreen = xDownInScreen;
                yInScreen = yDownInScreen;
                break;
            case MotionEvent.ACTION_MOVE:
                // 手指移动的时候更新小悬浮窗的位置
                xInScreen = event.getRawX();
                yInScreen = event.getRawY();
                if (!isMoving) {
                    isMoving = !isClickedEvent();
                } else {
                    updateViewPosition();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isClickedEvent()) {
                    countClick++;
                    if (countClick == 1) {
                        firstClickTime = System.currentTimeMillis();
                        handler.removeCallbacks(clickRunnable);
                        handler.postDelayed(clickRunnable, 300);
                    } else if (countClick == 2) {
                        long secondClickTime = System.currentTimeMillis();
                        if (secondClickTime - firstClickTime < 300) {//双击
//                            if (listener != null) {
//                                listener.onDoubleClick();
//                            }
                            scaleCount++;
                            //handleScaleEvent();
                            countClick = 0;
                            //2秒后才允许再次点击
                            canClick = false;
                            handler.removeCallbacks(canClickRunnable);
                            handler.postDelayed(canClickRunnable, 1000);
                        }
                    }
                } else {
                    if (null != listener) {
                        //listener.onMoved();
                    }
                    countClick = 0;
                }
                isMoving = false;
                break;
            default:
                break;
        }
        return true;
    }

    private boolean isClickedEvent() {
        int scaledTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
        // 是点击事件
        return Math.abs(xDownInScreen - xInScreen) <= scaledTouchSlop
                && Math.abs(yDownInScreen - yInScreen) <= scaledTouchSlop;
    }

    /**
     * 更新悬浮窗位置
     */
    private void updateViewPosition() {
        int x = (int) (xInScreen - xInView);
        int y = (int) (yInScreen - yInView);
        //防止超出通知栏
        if (y < statusBarHeight) {
            y = statusBarHeight;
        }
        //更新起点
        startX = x;
        startY = y;
        updateWindowXYPosition(x, y);
    }

    /**
     * 更新窗体坐标位置
     *
     * @param x
     * @param y
     */
    private synchronized void updateWindowXYPosition(int x, int y) {
        if (mWindowManager != null) {
            mWindowParams.x = x;
            mWindowParams.y = y;
            mWindowManager.updateViewLayout(this, mWindowParams);
        }
    }

    @Override
    public FloatViewParams getParams() {
//        params.contentWidth = getContentViewWidth();
        params.x = mWindowParams.x;
        params.y = mWindowParams.y;
        params.width = mWindowParams.width;
        params.height = mWindowParams.height;
        return params;
    }

    @Override
    public void setFloatViewListener(FloatViewListener listener) {
        this.listener = listener;
    }

}
