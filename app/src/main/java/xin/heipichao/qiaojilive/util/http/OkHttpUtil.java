package xin.heipichao.qiaojilive.util.http;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by Chaochao.Wen on 2017/9/26.
 */

public class OkHttpUtil {
    private static Handler _MainThreadHandler;
    private static final long DEFAULT_TIMEOUT=-1;
    public static void init(@NonNull Handler mainThreadHandler){
        _MainThreadHandler=mainThreadHandler;
    }

    private static Call buildCall(@NonNull String url, @Nullable Map<String,String> herders, @Nullable RequestBody body, long connectTimeout, long readTimeout) throws IOException {
        try{
            OkHttpClient okClient=buildOkHttpClient(connectTimeout,readTimeout);
            Request.Builder builder=new Request.Builder().url(url);
            if(herders!=null){
                builder.headers(Headers.of(herders));
            }
            if(body!=null){
                builder.post(body);
            }
            Request request=builder.build();
            return okClient.newCall(request);
        }catch (Exception e){
            throw new IOException(e);
        }

    }

    private static OkHttpClient buildOkHttpClient(long connectTimeout,long readTimeout){
        OkHttpClient.Builder okClientBuilder=new OkHttpClient.Builder().readTimeout(5000, TimeUnit.MILLISECONDS);
        if(connectTimeout>0){
            okClientBuilder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
        }
        if(readTimeout>0){
            okClientBuilder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        }
        return okClientBuilder.build();
    }

    public static void doGetAsync(@NonNull String url, @NonNull Callback callback){
        doGetAsync(url,null,callback,DEFAULT_TIMEOUT,DEFAULT_TIMEOUT);
    }

    public static void doGetAsync(@NonNull String url, @NonNull Callback callback, long connectTimeout, long readTimeout){
        doGetAsync(url,null,callback,connectTimeout,readTimeout);
    }

    public static void doGetAsync(@NonNull String url, @Nullable Map<String,String> herders,@NonNull Callback callback){
        doGetAsync(url,herders,callback,DEFAULT_TIMEOUT,DEFAULT_TIMEOUT);
    }

    public static void doGetAsync(@NonNull String url, @Nullable Map<String,String> herders,@NonNull Callback callback,long connectTimeout, long readTimeout){
        try{
            buildCall(url,herders,null,connectTimeout,readTimeout).enqueue(callback);
        }catch (IOException e){
            callback.onFailure(null,e);
        }

    }

    public static Response doGetSync(@NonNull String url) throws IOException {
        return doGetSync(url,null,DEFAULT_TIMEOUT,DEFAULT_TIMEOUT);
    }

    public static Response doGetSync(@NonNull String url, @Nullable Map<String,String> herders, long connectTimeout, long readTimeout) throws IOException {
        return buildCall(url,herders,null,connectTimeout,readTimeout).execute();
    }

    public static void doPostAsync(@NonNull String url, @Nullable Map<String,String> herders, @NonNull RequestBody body, long connectTimeout, long readTimeout, @NonNull Callback callback){
        try {
            buildCall(url,herders,body,connectTimeout,readTimeout).enqueue(callback);
        } catch (IOException e) {
            callback.onFailure(null,e);
        }
    }

    public static void downloadFile(@NonNull String url, @Nullable Map<String,String> herders, @NonNull final File file, long connectTimeout, long readTimeout, @NonNull final ReqProgressCallBack<File> callBack){
        doGetAsync(url,herders,new DownLoadFileCallBack(_MainThreadHandler,file,callBack),connectTimeout,readTimeout);
    }

    public static void downLoadFile(String url, @Nullable Map<String,String> herders, String savePath, String fileName, long connectTimeout, long readTimeout, ReqProgressCallBack<File> callBack){
        File file=new File(savePath,fileName);
        downloadFile(url,herders,file,connectTimeout,readTimeout,callBack);
    }

    public static void downloadFile(@NonNull String url, @NonNull String savePath, @NonNull String fileName, @NonNull ReqProgressCallBack<File> callBack){
        downLoadFile(url,null,savePath,fileName,DEFAULT_TIMEOUT,DEFAULT_TIMEOUT,callBack);
    }

    public static void downloadFile(@NonNull String url, @NonNull File file, @NonNull ReqProgressCallBack<File> callBack){
        downloadFile(url,null,file,DEFAULT_TIMEOUT,DEFAULT_TIMEOUT,callBack);
    }

    public static void getString(@NonNull String url, @NonNull ReqCallBack<String> callBack){
        getString(url,null,DEFAULT_TIMEOUT,DEFAULT_TIMEOUT,callBack);
    }

    public static void getString(@NonNull String url, long connectTimeout, long readTimeout, @NonNull ReqCallBack<String> callBack){
        getString(url,null,connectTimeout,readTimeout,callBack);
    }

    public static void getString(@NonNull String url, @Nullable Map<String,String> headers, long connectTimeout, long readTimeout, @NonNull ReqCallBack<String> callBack){
        doGetAsync(url,headers,new StringCallBack(_MainThreadHandler,callBack),connectTimeout,readTimeout);
    }

    public static void postString(@NonNull String url, @Nullable Map<String,String> headers, @NonNull String content, @NonNull ReqCallBack<String> callBack){
        RequestBody body=RequestBody.create(MediaType.parse("application/json; charset=utf-8"),content);
        doPostAsync(url,headers,body,DEFAULT_TIMEOUT,DEFAULT_TIMEOUT,new StringCallBack(_MainThreadHandler,callBack));
    }

    public static void postString(@NonNull String url, @NonNull String content, @NonNull ReqCallBack<String> callBack){
        postString(url,null,content,callBack);
    }

    public static void postForm(@NonNull String url, @Nullable Map<String,String> headers, @NonNull Map<String,String> params, @NonNull ReqCallBack<String> callBack){
        Set<String> keys=params.keySet();
        StringBuilder content =new StringBuilder();
        for(String key:keys){
            content.append(key);
            content.append("=");
            content.append(params.get(key));
            content.append("&");
        }
        RequestBody body=RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=utf-8"),content.toString());
        doPostAsync(url,headers,body,DEFAULT_TIMEOUT,DEFAULT_TIMEOUT,new StringCallBack(_MainThreadHandler,callBack));
    }

    public static void postForm(@NonNull String url, @NonNull Map<String,String> params, @NonNull ReqCallBack<String> callBack){
        postForm(url,null,params,callBack);
    }

    public static @Nullable
    String getString(@NonNull String url) throws IOException {
        return getString(url,null,DEFAULT_TIMEOUT,DEFAULT_TIMEOUT);
    }

    public static @Nullable
    String getString(@NonNull String url, @Nullable Map<String,String> headers, long connectTimeout, long readTimeout) throws IOException {
        Response response=doGetSync(url,headers,connectTimeout,readTimeout);
        if(!response.isSuccessful()){
            throw new IOException("response code:"+response.code());
        }else if (response.body()==null){
            return null;
        }else{
            return response.body().string();
        }
    }

}
