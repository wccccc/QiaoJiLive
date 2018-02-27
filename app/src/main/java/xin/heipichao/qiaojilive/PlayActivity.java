package xin.heipichao.qiaojilive;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.alibaba.fastjson.JSON;
import com.ijkplayer.IjkVideoView;


import xin.heipichao.qiaojilive.data.DataParser;
import xin.heipichao.qiaojilive.data.bean.LiveRoom;
import xin.heipichao.qiaojilive.data.bean.LiveRoomSettings;
import xin.heipichao.qiaojilive.fragment.SettingsFragment;
import xin.heipichao.qiaojilive.util.Util;

public class PlayActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG="PlayActivity";
    private IjkVideoView mPlayer;
    private Toolbar mToolbar;
    private String mRoomUrl;
    private LiveRoom mRoom;
    private LiveRoomSettings mSettings;
    private SettingsFragment mSettingsFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        findView();
        init();
        onNewIntent(getIntent());
    }

    private void init() {
        setSupportActionBar(mToolbar);
        if(Build.VERSION.SDK_INT>=19){
            int statusBarHeight = Util.getStatusBarHeight(this);
            RelativeLayout.LayoutParams params= (RelativeLayout.LayoutParams) mToolbar.getLayoutParams();
            params.topMargin=statusBarHeight;
            mToolbar.setLayoutParams(params);
        }
        mToolbar.setNavigationOnClickListener(this);
        hideSystemUI();
        mPlayer.setOnClickListener(this);
        mToolbar.setVisibility(View.GONE);
        mSettingsFragment=new SettingsFragment();
    }

    private void findView() {
        mPlayer= (IjkVideoView) findViewById(R.id.player);
        mToolbar= (Toolbar) findViewById(R.id.tb_toolBar);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mRoomUrl=intent.getStringExtra("url");
        if(mRoomUrl!=null){
            DataParser.parseOneLive(mRoomUrl, new DataParser.ParserCallback() {
                @Override
                public void callback(String result) {
                    mRoom=JSON.parseObject(result, LiveRoom.class);
                    if(mRoom!=null){
                        mSettingsFragment.setRoom(mRoom);
                        if(mSettings==null){
                            mSettings=LiveRoomSettings.create(mRoom.getUserNick());
                        }
                        mToolbar.setTitle(mRoom.getTitle());
                        String url=mRoom.getCdns().get(1).getUrl()+mRoom.getStreams().get(2).getValue();
                        mPlayer.setPlayerType(mSettings.getPlayerType());
                        mPlayer.setVideoPath(url);
                        mPlayer.start();
                    }else{
                        Toast.makeText(PlayActivity.this,"null",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }


    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    public void showSystemUI(){
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.VISIBLE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
//            case R.id.menu_resolution:
//                // TODO
//                break;
            case R.id.menu_setting:
                hideToolBar();
                FragmentManager fragmentManager=getSupportFragmentManager();
                FragmentTransaction fragmentTransaction=fragmentManager.beginTransaction();
                fragmentTransaction.setCustomAnimations(R.anim.anim_settings_show,R.anim.anim_settings_hide);
                fragmentTransaction.replace(R.id.fl_menu_content,mSettingsFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commitAllowingStateLoss();
                break;
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPlayer.stopPlayback();
    }

    @Override
    public void onClick(View view) {
        if(view==mPlayer){
            if(mToolbar.getVisibility()==View.VISIBLE){
                hideToolBar();
            }else if(mSettingsFragment.isVisible()){
                onBackPressed();
            }else{
                showToolBar();
            }
        }else{
            finish();
        }
    }

    private void showToolBar(){
        mToolbar.setVisibility(View.VISIBLE);
        showSystemUI();
    }

    private void hideToolBar(){
        mToolbar.setVisibility(View.GONE);
        hideSystemUI();
    }
}
