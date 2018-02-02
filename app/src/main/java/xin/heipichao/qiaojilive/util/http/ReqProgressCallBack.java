package xin.heipichao.qiaojilive.util.http;

/**
 * Created by Chaochao.Wen on 2017/9/27.
 */

public interface ReqProgressCallBack<T>  extends ReqCallBack<T>{
    /**
     * 响应进度更新
     */
    void onProgress(long total, long current);
}
