package xin.heipichao.qiaojilive.util.http;

/**
 * Created by Chaochao.Wen on 2017/9/27.
 */

public interface ReqCallBack<T> {
    void onSuccess(T result);
    void onFail(Exception e);
}
