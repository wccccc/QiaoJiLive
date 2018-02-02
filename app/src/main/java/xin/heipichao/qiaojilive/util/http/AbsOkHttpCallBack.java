package xin.heipichao.qiaojilive.util.http;

import android.os.Handler;
import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Chaochao.Wen on 2017/9/27.
 */

/* package */ abstract class AbsOkHttpCallBack implements Callback{
    Handler _MainThreadHandler;
    ReqCallBack _CallBack;
    public AbsOkHttpCallBack(@NonNull Handler handler, @NonNull ReqCallBack callBack){
        _MainThreadHandler=handler;
        _CallBack=callBack;
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        if(response.isSuccessful()){
            onSuccessfulResponse(call,response);
        }else{
            failedCallBack(new IOException("response code:"+response.code()));
        }
    }

    @Override
    public void onFailure(Call call, IOException e) {
        failedCallBack(e);
    }

    protected abstract void onSuccessfulResponse(Call call, Response response);

    protected void failedCallBack(final IOException e) {
        _MainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                _CallBack.onFail(e);
            }
        });
    }

    protected <T> void successCallBack(final T result) {
        _MainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                _CallBack.onSuccess(result);
            }
        });
    }
}
