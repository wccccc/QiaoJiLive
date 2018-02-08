package xin.heipichao.qiaojilive.data.bean;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Created by Chaochao.Wen on 2018/2/6.
 */

public class LiveRoom {
    private String title;
    private String num;
    private String url;
    private String roomImg;
    private String userNick;
    private String userImg;
    private String liveType;

    public String getTitle() {
        return title;
    }

    public String getNum() {
        return num;
    }

    public String getUrl() {
        return url;
    }

    public String getRoomImg() {
        return roomImg;
    }

    public String getUserNick() {
        return userNick;
    }

    public String getUserImg() {
        return userImg;
    }

    public String getLiveType() {
        return liveType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setRoomImg(String roomImg) {
        this.roomImg = roomImg;
    }

    public void setUserNick(String userNick) {
        this.userNick = userNick;
    }

    public void setUserImg(String userImg) {
        this.userImg = userImg;
    }

    public void setLiveType(String liveType) {
        this.liveType = liveType;
    }
}
