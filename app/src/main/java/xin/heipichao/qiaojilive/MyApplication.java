package xin.heipichao.qiaojilive;

import android.app.Application;
import android.os.Handler;

import xin.heipichao.qiaojilive.data.DataParser;
import xin.heipichao.qiaojilive.util.http.OkHttpUtil;

/**
 * Created by Chaochao.Wen on 2018/2/2.
 */

public class MyApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        Handler handler=new Handler();
        OkHttpUtil.init(handler);
        DataParser.init(handler);
    }
}
