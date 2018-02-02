package com.ijkplayer;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.MediaController;

import java.io.IOException;
import java.util.Map;

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * Created by Chaochao.Wen on 2017/10/12.
 * 封装了ExoPlayer、MediaPlayer、IjkPlayer，使用方法与VideoView相同
 */

public class IjkVideoView extends SurfaceView implements MediaController.MediaPlayerControl, SurfaceHolder.Callback {
    private static final String TAG="IjkVideoView";
    public static final int PLAYER_TYPE_EXO=0;
    public static final int PLAYER_TYPE_MEDIA=1;
    public static final int PLAYER_TYPE_IJK=2;



    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private IMediaPlayer mMediaPlayer;
    private SurfaceHolder mSurfaceHolder;
    private int mPlayerType;

    private int mCurrentBufferPercentage;
    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;
    private int mSeekWhenPrepared;
    private Uri mUri;
    private Map<String,String> mHeaders;
    private IMediaPlayer.OnPreparedListener mPreparedListener=new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer mp) {
            mCurrentState = STATE_PREPARED;
            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mp);
            }
            int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }
            if (mTargetState == STATE_PLAYING) {
                start();
            }
        }
    };
    private IMediaPlayer.OnCompletionListener mCompletionListener=new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer mp) {
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;
            if (mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(mp);
            }
        }
    };
    private IMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {
            mCurrentBufferPercentage=percent;
        }
    };
    private IMediaPlayer.OnErrorListener mErrorListener=new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            /* If an error handler has been supplied, use it and finish. */
            if (mOnErrorListener != null) {
                if (mOnErrorListener.onError(mp, what, extra)) {
                    return true;
                }
            }
            return false;
        }
    };

    private IMediaPlayer.OnPreparedListener mOnPreparedListener;
    private IMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private IMediaPlayer.OnCompletionListener mOnCompletionListener;
    private IMediaPlayer.OnErrorListener mOnErrorListener;
    private IMediaPlayer.OnInfoListener mOnInfoListener;

    private IMediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener;

    public IjkVideoView(Context context) {
        this(context,null);
    }

    public IjkVideoView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public IjkVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
    }

    public void setPlayerType(int playerType) {
        this.mPlayerType = playerType;
    }



    public void setVideoPath(String path){
        setVideoPath(path,null);
    }

    public void setVideoPath(String path,Map<String,String> header){
        setVideoUri(Uri.parse(path),header);
    }

    public void setVideoUri(Uri uri){
        setVideoUri(uri,null);
    }

    public void setVideoUri(Uri uri,Map<String,String> header){
        mUri=uri;
        mHeaders=header;
        openVideo();
    }

    public void setOnPreparedListener(IMediaPlayer.OnPreparedListener onPreparedListener) {
        this.mOnPreparedListener = onPreparedListener;
    }

    public void setOnInfoListener(IMediaPlayer.OnInfoListener onInfoListener) {
        this.mOnInfoListener = onInfoListener;
    }

    public void setOnErrorListener(IMediaPlayer.OnErrorListener onErrorListener) {
        this.mOnErrorListener = onErrorListener;
    }

    public void setOnCompletionListener(IMediaPlayer.OnCompletionListener onCompletionListener) {
        this.mOnCompletionListener = onCompletionListener;
    }

    public void setOnSeekCompleteListener(IMediaPlayer.OnSeekCompleteListener onSeekCompleteListener) {
        this.mOnSeekCompleteListener = onSeekCompleteListener;
    }

    public void setOnVideoSizeChangedListener(IMediaPlayer.OnVideoSizeChangedListener onVideoSizeChangedListener) {
        this.mOnVideoSizeChangedListener = onVideoSizeChangedListener;
    }

    private void openVideo(){
        if(mUri==null||mSurfaceHolder==null){
            return;
        }
        release();
        mMediaPlayer=createMediaPlayer();
        mMediaPlayer.setOnPreparedListener(mPreparedListener);
        mMediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        mMediaPlayer.setOnCompletionListener(mCompletionListener);
        mMediaPlayer.setOnErrorListener(mErrorListener);
        mMediaPlayer.setOnInfoListener(mOnInfoListener);
        mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
        mMediaPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
        mCurrentBufferPercentage=0;
        try {
            mMediaPlayer.setDataSource(getContext(), mUri, mHeaders);
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mCurrentState = STATE_PREPARING;
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, IMediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, IMediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }catch (IllegalStateException ex){
            ex.printStackTrace();
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, IMediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }


    public void stopPlayback() {
        release();
        mUri=null;
        mHeaders=null;
    }

    private IMediaPlayer createMediaPlayer(){
        Log.d(TAG,"createMediaPlayer() mPlayerType:"+mPlayerType);
        IMediaPlayer player;
        switch (mPlayerType){
            case PLAYER_TYPE_EXO:
                player=new IjkExoPlayer(getContext());
                break;
            case PLAYER_TYPE_IJK:
                //TODO do not need FFPlayer now.if you want use FFPlayer you must have .so file.
//                IjkMediaPlayer ijkMediaPlayer=new IjkMediaPlayer();
//                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);//1表示优先使用MediaCodec
//                player=ijkMediaPlayer;
//                break;
            case PLAYER_TYPE_MEDIA:
            default:
                player=new AndroidMediaPlayer();
        }
        return player;
    }

    private void release(){
        if(mMediaPlayer!=null){
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer=null;
            mCurrentState = STATE_IDLE;
            mTargetState  = STATE_IDLE;
        }
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    @Override
    public void start() {
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        mTargetState = STATE_PAUSED;
    }

    public int getPlayerType() {
        return mPlayerType;
    }

    public String getPlayerName() {
        switch (getPlayerType()){
            case PLAYER_TYPE_EXO:
                return "ExoPlayer";
            case PLAYER_TYPE_MEDIA:
                return "MediaPlayer";
            case PLAYER_TYPE_IJK:
                //TODO
            default:
                return "MediaPlayer";
        }
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getDuration();
        }

        return -1;
    }

    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getVideoWidth(){
        if (isInPlaybackState()) {
            return mMediaPlayer.getVideoWidth();
        }else{
            return 0;
        }
    }

    public int getVideoHeight(){
        if (isInPlaybackState()) {
            return mMediaPlayer.getVideoHeight();
        }else{
            return 0;
        }
    }

    @Override
    public void seekTo(int pos) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(pos);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = pos;
        }
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    @Override
    public boolean canPause() {
        //TODO implement
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        //TODO implement
        return true;
    }

    @Override
    public boolean canSeekForward() {
        //TODO implement
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return mMediaPlayer.getAudioSessionId();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG,"surfaceCreated");
        mSurfaceHolder=holder;
        if(mMediaPlayer==null||!mMediaPlayer.isPlaying()){
            openVideo();
        }else{
            mMediaPlayer.setDisplay(mSurfaceHolder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder=null;
    }
}
