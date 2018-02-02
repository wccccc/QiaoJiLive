package com.google.android.exoplayer2.ext.rtsp;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
/**
 * Created by chao chao Wen on 2017/6/5.
 */
public class RtpStreamBuffer {
	private boolean _Active;
	private final Object _MediaDataLock = new Object();
	private final ConcurrentLinkedQueue<ByteBuffer> _MediaDataQueue;
	public RtpStreamBuffer(){
		_Active=true;
		_MediaDataQueue=new ConcurrentLinkedQueue<>();
	}
	public ByteBuffer pull(){
		while (_Active) {
			while (!_MediaDataQueue.isEmpty()) {
				return _MediaDataQueue.poll();
			}
			// Wait for next received packet
			synchronized (_MediaDataLock) {
				try {
					_MediaDataLock.wait(500);
				} catch (InterruptedException ex) {
					// Ignore
				}
			}
		}
		return null;
	}
	public void push(ByteBuffer data){
		_MediaDataQueue.add(data);
		synchronized (_MediaDataLock) {
			_MediaDataLock.notifyAll();
		}
	}
	public void release(){
		_Active=false;
	}
}
