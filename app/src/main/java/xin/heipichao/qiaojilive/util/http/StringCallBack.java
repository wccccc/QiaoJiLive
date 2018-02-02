package xin.heipichao.qiaojilive.util.http;

import android.os.Handler;
import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by Chaochao.Wen on 2017/9/27.
 */

/* package */ class StringCallBack extends AbsOkHttpCallBack{

    public StringCallBack(@NonNull Handler handler, @NonNull ReqCallBack<String> callBack) {
        super(handler, callBack);
    }

    @Override
    protected void onSuccessfulResponse(Call call, Response response) {
        try {
            if(response.body()==null){
                throw new IOException("the body is null!");
            }else{
                String result=response.body().string();
                successCallBack(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
            failedCallBack(e);
        }
    }
}
