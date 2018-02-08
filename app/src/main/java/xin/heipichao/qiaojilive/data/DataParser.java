package xin.heipichao.qiaojilive.data;

import android.os.Handler;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import xin.heipichao.qiaojilive.util.http.OkHttpUtil;

/**
 * Created by Chaochao.Wen on 2018/2/6.
 */

public class DataParser {
    private static Handler mMainHandler;
    public static final int TYPE_HUYA=0;
    public static final int TYPE_DOUYU=1;
    public static void init(Handler handler){
        mMainHandler=handler;
    }
    public static void parseAllLive(int type,ParserCallback callback){
        switch (type){
            case TYPE_HUYA:
                huyaAllLive(callback);
                break;
            case TYPE_DOUYU:
                break;
        }
    }

    public static void parseOneLive(String url,ParserCallback callback){
        if(url.contains("huya.com")){
            parseHuyaOneLive(url,callback);
        }else{

        }
    }

    private static void  parseHuyaOneLive(final String url, final ParserCallback callback){
        new Thread(){
            @Override
            public void run() {
                try {
                    Document dom= Jsoup.connect(url).get();
                    Element sc=dom.body().select("script[data-fixed=\"true\"]").get(3);
                    notifyCallback(callback,subString(sc.html(),"\"stream\": ","\\};"));
                } catch (Exception e) {
                    e.printStackTrace();
                    notifyCallback(callback,null);
                }
            }
        }.start();
    }

    private static void huyaAllLive(final ParserCallback callback){
        new Thread(){
            @Override
            public void run() {
                try {
                    Document dom= Jsoup.connect("http://www.huya.com/l").get();
//                    Elements rooms=dom.selectFirst("#js-live-list").children();
//                    JSONArray jsonRooms=new JSONArray();
//                    for(Element room:rooms){
//                        Elements as=room.children();
//                        Element a=as.get(0);
//                        Element span=as.get(2);
//                        JSONObject jsonRoom=new JSONObject();
//                        jsonRoom.put("title",as.get(1).text());
//                        jsonRoom.put("num",span.selectFirst(".js-num").text());
//                        jsonRoom.put("url",a.attr("href"));
//                        jsonRoom.put("roomImg","http:"+a.selectFirst("img").attr("src"));
//                        jsonRoom.put("userNick",span.selectFirst("i.nick").text());
//                        jsonRoom.put("userImg","http:"+span.selectFirst("img").attr("src"));
//                        jsonRoom.put("liveType",span.selectFirst("a").text());
//                        jsonRooms.add(jsonRoom);
//                    }

                    Elements scripts=dom.body().select("script");
                    JSONArray jsonRooms=new JSONArray();
                    for (Element sc:scripts){
                        if(sc.attributes().size()==0){
                            String js=sc.html();
                            int start=js.indexOf(" = ");
                            int end =js.indexOf("require(")-7;
                            String data=js.substring(start+3,end);
//                            Log.e("fragment",data);
//                            Log.e("fragment",data.substring(data.length()-20,data.length()));
                            JSONArray rooms= JSON.parseArray(data);
                            for (Object obj:rooms){
                                JSONObject room= (JSONObject) obj;
                                JSONObject jsonRoom=new JSONObject();
                                jsonRoom.put("title",room.getString("introduction"));
                                int num=Integer.parseInt(room.getString("totalCount"));
                                if(num>=10000){
                                    jsonRoom.put("num",(num/10000)+"万");
                                }else{
                                    jsonRoom.put("num",num+"");
                                }

                                jsonRoom.put("url","http://www.huya.com/"+room.getString("privateHost"));
                                jsonRoom.put("roomImg",room.getString("screenshot"));
                                jsonRoom.put("userNick",room.getString("nick"));
                                jsonRoom.put("userImg",room.getString("avatar180"));
                                jsonRoom.put("liveType",room.getString("gameFullName"));
                                jsonRooms.add(jsonRoom);
                            }
                            break;
                        }
                    }
                    notifyCallback(callback,jsonRooms.toString());
                } catch (Exception e) {
                    notifyCallback(callback,null);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private static void notifyCallback(final ParserCallback callback, final String result){
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.callback(result);
            }
        });
    }

    public interface ParserCallback {
        void callback(String result);
    }

    private static String subString(String text,String start,String end){
        String pattern = start+"(.*?)"+end;
        Log.e("fragment",pattern);
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(text);
        if(m.find()){
            Log.e("fragment","true");
            return m.group(1);
        }else{
            return null;
        }
    }
}
