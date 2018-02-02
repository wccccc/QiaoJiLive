package xin.heipichao.qiaojilive;

import android.content.Intent;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.ijkplayer.IjkVideoView;

import java.util.ArrayList;

import xin.heipichao.qiaojilive.data.PlaySource;

public class PlayActivity extends AppCompatActivity {
    public static final String INTENT_KEY_PLAY_LIST ="playList";
    public static final String INTENT_KEY_PLAY_TITLE="title";
    private IjkVideoView mPlayer;
    private ArrayList<PlaySource> mPlayList;
    private int mPlayIndex;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        findView();
        onNewIntent(getIntent());
    }

    private void findView() {
        mPlayer= (IjkVideoView) findViewById(R.id.player);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mPlayList= intent.getParcelableArrayListExtra(INTENT_KEY_PLAY_LIST);
        mPlayIndex=mPlayList.size()-1;
        mPlayer.setVideoPath(mPlayList.get(mPlayIndex).mUrl);
    }
}
