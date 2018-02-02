package xin.heipichao.qiaojilive.util.http;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by Chaochao.Wen on 2017/9/27.
 */

/* package */ class DownLoadFileCallBack extends AbsOkHttpCallBack{
    private static final String TAG="DownLoadFileCallBack";
    private File _File;
    public DownLoadFileCallBack(@NonNull Handler handler, File file , @NonNull ReqProgressCallBack<File> callBack) {
        super(handler, callBack);
        _File=file;
    }

    @Override
    protected void onSuccessfulResponse(Call call, Response response) {
        InputStream is = null;
        byte[] buf = new byte[2048];
        int len = 0;
        FileOutputStream fos = null;
        try {
            if(response.body()==null){
                failedCallBack(new IOException("response body is null!"));
            }else{
                long total = response.body().contentLength();
                Log.d(TAG, "total------>" + total);
                long current = 0;
                is = response.body().byteStream();
                fos = new FileOutputStream(_File);
                while ((len = is.read(buf)) != -1) {
                    current += len;
                    fos.write(buf, 0, len);
                    Log.d(TAG, "current------>" + current);
                    progressCallBack(total, current);
                }
                fos.flush();
                successCallBack(_File);
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            failedCallBack(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    protected void progressCallBack(final long total, final long current) {
        _MainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                ((ReqProgressCallBack)_CallBack).onProgress(total,current);
            }
        });
    }
}
