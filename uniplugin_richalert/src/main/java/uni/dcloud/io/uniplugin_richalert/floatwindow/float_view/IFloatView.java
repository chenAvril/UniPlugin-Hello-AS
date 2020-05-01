package uni.dcloud.io.uniplugin_richalert.floatwindow.float_view;

/**
 * Description:悬浮窗抽象方法
 */
public interface IFloatView {
    FloatViewParams getParams();
    void setFloatViewListener(FloatViewListener listener);
}
