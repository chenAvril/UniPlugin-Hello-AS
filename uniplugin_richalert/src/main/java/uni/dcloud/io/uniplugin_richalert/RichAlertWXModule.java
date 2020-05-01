package uni.dcloud.io.uniplugin_richalert;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.utils.WXLogUtils;
import com.taobao.weex.utils.WXResourceUtils;

public class RichAlertWXModule extends WXSDKEngine.DestroyableModule {

    @JSMethod(uiThread = true)
    public void jump() {
        if (mWXSDKInstance.getContext() instanceof Activity) {

            Intent intent = new Intent(mWXSDKInstance.getContext(),ContentActivity .class);
            mWXSDKInstance.getContext().startActivity(intent);
        }
    }

    @Override
    public void destroy() {
        Log.e("111","===>adf");
    }

}
