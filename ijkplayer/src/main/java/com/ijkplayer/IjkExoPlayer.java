package com.ijkplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

import tv.danmaku.ijk.media.player.AbstractMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.MediaInfo;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;
import tv.danmaku.ijk.media.player.pragma.DebugLog;

/**
 * Created by Chaochao.Wen on 2017/10/11.
 */

public class IjkExoPlayer extends AbstractMediaPlayer implements Player.EventListener, SimpleExoPlayer.VideoListener, ExtractorMediaSource.EventListener, AdaptiveMediaSourceEventListener {
    private SimpleExoPlayer mInternalPlayer;
    private Context mAppContext;
    private Uri mDataSource;
    private int mVideoWidth;
    private int mVideoHeight;
    private final Handler mMainHandler;
    private static final String TAG = "IjkExoPlayer";
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private boolean mIsBuffering;
    private boolean mScreenOnWhilePlaying;
    private boolean mStayAwake;
    private boolean mIsSeeking;
    private Map<String, String> mHeaders;
    private SurfaceHolder mSurfaceHolder;
    private PowerManager.WakeLock mWakeLock = null;
    private boolean mFirstFrameRendered;

    private Runnable mBufferUpdateRunner=new Runnable() {
        @Override
        public void run() {
            notifyOnBufferingUpdate(mInternalPlayer.getBufferedPercentage());
            mMainHandler.postDelayed(this,500);
        }
    };

    public IjkExoPlayer(Context context) {
        mAppContext = context.getApplicationContext();
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(mAppContext, null, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector();
        mInternalPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
        mInternalPlayer.addListener(this);
//        mInternalPlayer.addVideoListener(this);
        mInternalPlayer.setVideoListener(this);
        mMainHandler = new Handler();
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        if (mInternalPlayer != null) {
            mInternalPlayer.setVideoSurfaceHolder(sh);
            mSurfaceHolder=sh;
        }
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        setDataSource(context, uri, null);
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mDataSource = uri;
        mHeaders=headers;
    }

    @Override
    public void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException {
        // TODO: no support
        throw new UnsupportedOperationException("no support");
    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        setDataSource(null, Uri.parse(path));
    }

    @Override
    public String getDataSource() {
        return mDataSource == null ? null : mDataSource.toString();
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        if(mInternalPlayer!=null){
            mInternalPlayer.prepare(buildMediaSource(mDataSource,null));
            notifyOnPrepared();
        }
    }

    private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
        if(uri.getLastPathSegment()==null){
            throw new IllegalArgumentException("uri.getLastPathSegment()==null");
        }
        int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
                : uri.getLastPathSegment());

        if (type == C.TYPE_OTHER&&uri.toString().contains("m3u8")||uri.toString().contains("wtvlive/iqilu")) {
            type = C.TYPE_HLS;
        }
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, buildDefaultDataSourceFactory(false),
                        new DefaultSsChunkSource.Factory(buildDefaultDataSourceFactory(true)), mMainHandler, this);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, buildDefaultDataSourceFactory(false),
                        new DefaultDashChunkSource.Factory(buildDefaultDataSourceFactory(true)), mMainHandler, this);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, buildDefaultDataSourceFactory(true), mMainHandler, this);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, buildDefaultDataSourceFactory(true), new DefaultExtractorsFactory(),
                        mMainHandler, this);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    private DataSource.Factory buildDefaultDataSourceFactory(boolean useDefaultBandwidthMeter) {
        DefaultBandwidthMeter defaultBandwidthMeter = useDefaultBandwidthMeter ? BANDWIDTH_METER : null;
        String userAgent;
        if(mHeaders!=null&&mHeaders.containsKey("User-Agent")){
            userAgent =mHeaders.remove("User-Agent");
        }else{
            userAgent= Util.getUserAgent(mAppContext, "ExoPlayer");
        }
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent, defaultBandwidthMeter);
        if(mHeaders!=null){
            httpDataSourceFactory.getDefaultRequestProperties().set(mHeaders);
        }
        return new DefaultDataSourceFactory(mAppContext, defaultBandwidthMeter,
                httpDataSourceFactory);
    }

    @Override
    public void start() throws IllegalStateException {
        if (mInternalPlayer != null) {
            stayAwake(true);
            mInternalPlayer.setPlayWhenReady(true);
        }
    }

    @Override
    public void stop() throws IllegalStateException {
        if (mInternalPlayer != null) {
            stayAwake(false);
            mInternalPlayer.stop();
            mIsBuffering=false;
            mIsSeeking=false;
            mFirstFrameRendered=false;
        }
    }

    @Override
    public void pause() throws IllegalStateException {
        if (mInternalPlayer != null) {
            stayAwake(false);
            mInternalPlayer.setPlayWhenReady(false);
        }
    }

    private void stayAwake(boolean awake) {
        if (mWakeLock != null) {
            if (awake && !mWakeLock.isHeld()) {
                mWakeLock.acquire();
            } else if (!awake && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
        mStayAwake = awake;
        updateSurfaceScreenOn();
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {
        if (mScreenOnWhilePlaying != screenOn) {
            if (screenOn && mSurfaceHolder == null) {
                DebugLog.w(TAG,
                        "setScreenOnWhilePlaying(true) is ineffective without a SurfaceHolder");
            }
            mScreenOnWhilePlaying = screenOn;
            updateSurfaceScreenOn();
        }
    }

    private void updateSurfaceScreenOn() {
        if (mSurfaceHolder != null) {
            mSurfaceHolder.setKeepScreenOn(mScreenOnWhilePlaying && mStayAwake);
        }
    }

    @Override
    public int getVideoWidth() {
        return mVideoWidth;
    }

    @Override
    public int getVideoHeight() {
        return mVideoHeight;
    }

    @Override
    public boolean isPlaying() {
        if (mInternalPlayer != null) {
            int state = mInternalPlayer.getPlaybackState();
            switch (state) {
                case Player.STATE_BUFFERING:
                case Player.STATE_READY:
                    return mInternalPlayer.getPlayWhenReady();
                case Player.STATE_IDLE:
                case Player.STATE_ENDED:
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        if (mInternalPlayer != null) {
            mIsSeeking=true;
            mInternalPlayer.seekTo(msec);
        }
    }

    @Override
    public long getCurrentPosition() {
        return mInternalPlayer == null ? 0 : mInternalPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return mInternalPlayer == null ? 0 : mInternalPlayer.getDuration();
    }

    @Override
    public void release() {
        reset();
        if (mInternalPlayer != null) {
            mInternalPlayer.removeListener(this);
//            mInternalPlayer.removeVideoListener(this);
            mInternalPlayer.clearVideoListener(this);
            mMainHandler.removeCallbacks(mBufferUpdateRunner);
            mSurfaceHolder=null;
            mInternalPlayer = null;
        }
    }

    @Override
    public void reset() {
        if (mInternalPlayer != null) {
            stayAwake(false);
            mInternalPlayer.release();
            mDataSource = null;
            mHeaders=null;
            mVideoWidth = 0;
            mVideoHeight = 0;
            mIsBuffering=false;
            mIsSeeking=false;
            mFirstFrameRendered=false;
        }
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        if(mInternalPlayer!=null){
            mInternalPlayer.setVolume(leftVolume);
        }
    }

    @Override
    public int getAudioSessionId() {
        if(mInternalPlayer!=null){
            return mInternalPlayer.getAudioSessionId();
        }else{
            return 0;
        }
    }

    @Override
    public MediaInfo getMediaInfo() {
        // TODO: no support
        return null;
    }

    @Override
    public void setLogEnabled(boolean enable) {
        // do nothing
    }

    @Override
    public boolean isPlayable() {
        return mInternalPlayer == null ? false : true;
    }

    @Override
    public void setAudioStreamType(int streamtype) {
        // do nothing
    }

    @Override
    public void setKeepInBackground(boolean keepInBackground) {
        // do nothing
    }

    @Override
    public int getVideoSarNum() {
        return 1;
    }

    @Override
    public int getVideoSarDen() {
        return 1;
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        // FIXME: implement
        boolean washeld = false;
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                washeld = true;
                mWakeLock.release();
            }
            mWakeLock = null;
        }

        PowerManager pm = (PowerManager) context
                .getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(mode | PowerManager.ON_AFTER_RELEASE,
                IjkMediaPlayer.class.getName());
        mWakeLock.setReferenceCounted(false);
        if (washeld) {
            mWakeLock.acquire();
        }
    }

    @Override
    public void setLooping(boolean looping) {
        // TODO: no support
        throw new UnsupportedOperationException("no support");
    }

    @Override
    public boolean isLooping() {
        // TODO: no support
        return false;
    }

    @Override
    public ITrackInfo[] getTrackInfo() {
        // TODO: implement
        return null;
    }

    @Override
    public void setSurface(Surface surface) {
        if (mInternalPlayer != null) {
            mInternalPlayer.setVideoSurface(surface);

        }
    }

    //exo listener
    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
//        DebugLog.d(TAG,"onTimelineChanged()\ntimeline:"+timeline+
//                "\nmanifest:"+manifest);
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
//        DebugLog.d(TAG,"onTracksChanged()\ntrackGroups:"+trackGroups+
//                "\ntrackSelections:"+trackSelections);
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
//        DebugLog.d(TAG,"onLoadingChanged()\nisLoading:"+isLoading);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
//        DebugLog.d(TAG,"onPlayerStateChanged()\nplayWhenReady:"+playWhenReady+
//                "\nplaybackState:"+playbackState);

        switch (playbackState) {
            case Player.STATE_IDLE:
                break;
            case Player.STATE_BUFFERING:
                if(mFirstFrameRendered){
                    notifyOnInfo(IMediaPlayer.MEDIA_INFO_BUFFERING_START, mInternalPlayer.getBufferedPercentage());
                    mIsBuffering = true;
                    mMainHandler.post(mBufferUpdateRunner);
                }
                break;
            case Player.STATE_READY:
                if(mIsBuffering){
                    notifyOnInfo(IMediaPlayer.MEDIA_INFO_BUFFERING_END, mInternalPlayer.getBufferedPercentage());
                    mIsBuffering = false;
                    mMainHandler.removeCallbacks(mBufferUpdateRunner);
                }
                if(mIsSeeking){
                    mIsSeeking=false;
                    notifyOnSeekComplete();
                }
                break;
            case Player.STATE_ENDED:
                if(mIsBuffering){
                    notifyOnInfo(IMediaPlayer.MEDIA_INFO_BUFFERING_END, mInternalPlayer.getBufferedPercentage());
                    mIsBuffering = false;
                    mMainHandler.removeCallbacks(mBufferUpdateRunner);
                }
                if(mIsSeeking){
                    mIsSeeking=false;
                    notifyOnSeekComplete();
                }
                stayAwake(false);
                notifyOnCompletion();
                break;
            default:
                break;
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
//        DebugLog.d(TAG,"onRepeatModeChanged()\nrepeatMode:"+repeatMode);
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        DebugLog.e(TAG,"onPlayerError()\nerror:"+error);
        stayAwake(false);
        notifyOnError(IMediaPlayer.MEDIA_ERROR_UNKNOWN, IMediaPlayer.MEDIA_ERROR_UNKNOWN);
    }

    @Override
    public void onPositionDiscontinuity() {
//        DebugLog.d(TAG,"onPositionDiscontinuity()");
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
//        DebugLog.d(TAG,"onPlaybackParametersChanged()\nplaybackParameters:"+playbackParameters);
    }

    //video Listener
    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        mVideoHeight=height;
        mVideoWidth=width;
        notifyOnVideoSizeChanged(width, height, 1, 1);
        if (unappliedRotationDegrees > 0)
            notifyOnInfo(IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED, unappliedRotationDegrees);
    }

    @Override
    public void onRenderedFirstFrame() {
        mFirstFrameRendered=true;
        notifyOnInfo(IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START, mInternalPlayer.getBufferedPercentage());
    }

    //media source listener
    @Override
    public void onLoadError(IOException error) {
        DebugLog.e(TAG,"onLoadError()\nerror:"+error);
        notifyOnError(IMediaPlayer.MEDIA_ERROR_IO, IMediaPlayer.MEDIA_ERROR_IO);
    }

    @Override
    public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs) {
//        DebugLog.d(TAG,"onLoadStarted()\ndataSpec:"+dataSpec+
//        "\ndataType:"+dataType+
//        "\ntrackType:"+trackType+
//        "\ntrackFormat:"+trackFormat+
//        "\ntrackSelectionReason:"+trackSelectionReason+
//        "\ntrackSelectionData:"+trackSelectionData+
//        "\nmediaStartTimeMs:"+mediaStartTimeMs+
//        "\nmediaEndTimeMs:"+mediaEndTimeMs+
//        "\nelapsedRealtimeMs:"+elapsedRealtimeMs);
    }

    @Override
    public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
//        DebugLog.d(TAG,"onLoadCompleted()\ndataSpec:"+dataSpec+
//                "\ndataType:"+dataType+
//                "\ntrackType:"+trackType+
//                "\ntrackFormat:"+trackFormat+
//                "\ntrackSelectionReason:"+trackSelectionReason+
//                "\ntrackSelectionData:"+trackSelectionData+
//                "\nmediaStartTimeMs:"+mediaStartTimeMs+
//                "\nmediaEndTimeMs:"+mediaEndTimeMs+
//                "\nelapsedRealtimeMs:"+elapsedRealtimeMs+
//                "\nloadDurationMs:"+loadDurationMs+
//                "\nbytesLoaded:"+bytesLoaded);
    }

    @Override
    public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
//        DebugLog.d(TAG,"onLoadCanceled()\ndataSpec:"+dataSpec+
//                "\ndataType:"+dataType+
//                "\ntrackType:"+trackType+
//                "\ntrackFormat:"+trackFormat+
//                "\ntrackSelectionReason:"+trackSelectionReason+
//                "\ntrackSelectionData:"+trackSelectionData+
//                "\nmediaStartTimeMs:"+mediaStartTimeMs+
//                "\nmediaEndTimeMs:"+mediaEndTimeMs+
//                "\nelapsedRealtimeMs:"+elapsedRealtimeMs+
//                "\nloadDurationMs:"+loadDurationMs+
//                "\nbytesLoaded:"+bytesLoaded);
    }

    @Override
    public void onLoadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {
//        DebugLog.d(TAG,"onLoadCanceled()\ndataSpec:"+dataSpec+
//                "\ndataType:"+dataType+
//                "\ntrackType:"+trackType+
//                "\ntrackFormat:"+trackFormat+
//                "\ntrackSelectionReason:"+trackSelectionReason+
//                "\ntrackSelectionData:"+trackSelectionData+
//                "\nmediaStartTimeMs:"+mediaStartTimeMs+
//                "\nmediaEndTimeMs:"+mediaEndTimeMs+
//                "\nelapsedRealtimeMs:"+elapsedRealtimeMs+
//                "\nloadDurationMs:"+loadDurationMs+
//                "\nbytesLoaded:"+bytesLoaded+
//                "\nerror:"+error+
//                "\nwasCanceled:"+wasCanceled);
        if(wasCanceled){
            notifyOnError(IMediaPlayer.MEDIA_ERROR_IO, IMediaPlayer.MEDIA_ERROR_IO);
        }
    }

    @Override
    public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs, long mediaEndTimeMs) {
//        DebugLog.d(TAG,"onUpstreamDiscarded()\ntrackType:"+trackType+
//                "\nmediaStartTimeMs:"+mediaStartTimeMs+
//                "\nmediaEndTimeMs:"+mediaEndTimeMs);
    }

    @Override
    public void onDownstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaTimeMs) {
//        DebugLog.d(TAG,"onDownstreamFormatChanged()\ntrackType:"+trackType+
//                "\ntrackFormat:"+trackFormat+
//                "\ntrackSelectionReason:"+trackSelectionReason+
//                "\ntrackSelectionData:"+trackSelectionData+
//                "\nmediaTimeMs:"+mediaTimeMs);
    }
}
