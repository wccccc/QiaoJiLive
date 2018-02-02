package com.google.android.exoplayer2.ext.rtsp.Stream;

import java.nio.ByteBuffer;

import com.google.android.exoplayer2.ext.rtsp.RtpStreamBuffer;

import android.util.Log;

/**
 * Created by chao chao Wen on 2017/6/5.
 */

public class TsStream extends RtpStream {
    private static final String TAG="TsStream";
    private RtpStreamBuffer _Buffer;
//    private int currentIndex;
    
    public TsStream(RtpStreamBuffer buffer){
    	_Buffer=buffer;
    }
    @Override
    protected void recombinePacket(StreamPacks sp) {
//        Log.e(TAG,"pt:"+sp.pt);
//        Log.e(TAG,"mark:"+sp.mark);
//        Log.e(TAG,"Ssrc:"+sp.Ssrc);
//        Log.e(TAG,"timestamp:"+sp.timestamp);
//        Log.e(TAG,"sequenceNumber:"+sp.sequenceNumber);
//        Log.e(TAG,"data length:"+sp.data.length);
//        Log.e(TAG,"----------------------");
//    	if(currentIndex>sp.sequenceNumber){
//    		Log.e(TAG, "currentIndex:"+currentIndex+" sequenceNumber:"+sp.sequenceNumber);
//    	}else{
//    		if(sp.sequenceNumber==65535){
//    			currentIndex=0;
//    		}else{
//    			currentIndex=sp.sequenceNumber;
//    		}
//    	}
        ByteBuffer data=ByteBuffer.allocate(sp.data.length);
        data.put(sp.data);
        data.position(0);
        _Buffer.push(data);
    }
}
