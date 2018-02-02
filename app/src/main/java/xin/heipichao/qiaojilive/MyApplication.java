package xin.heipichao.qiaojilive;

import android.app.Application;
import android.os.Handler;

import xin.heipichao.qiaojilive.util.http.OkHttpUtil;

/**
 * Created by Chaochao.Wen on 2018/2/2.
 */

public class MyApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        OkHttpUtil.init(new Handler());
    }
}
