package xin.heipichao.qiaojilive.data;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * Created by Chaochao.Wen on 2018/2/2.
 */

public class PlaySource implements Parcelable {
    public String mUrl;
    public String mName;
    public PlaySource(String url,String name){
        mName=name;
        mUrl=url;
    }

    protected PlaySource(Parcel in) {
        mUrl = in.readString();
        mName = in.readString();
    }

    public static final Creator<PlaySource> CREATOR = new Creator<PlaySource>() {
        @Override
        public PlaySource createFromParcel(Parcel in) {
            return new PlaySource(in);
        }

        @Override
        public PlaySource[] newArray(int size) {
            return new PlaySource[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mUrl);
        dest.writeString(mName);
    }
}
