package com.google.android.exoplayer2.ext.rtsp;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import android.net.Uri;
import android.util.Log;

public final class RtspDataSource implements DataSource {
	private RtspClient _RtspClient;
	private Uri _Uri;
	private boolean _Opened;
	private RtpStreamBuffer _Buffer;
	private ByteBuffer _MediaData;
	private static final String TAG="RtspDataSource";

	@Override
	public long open(DataSpec dataSpec) throws IOException {
		if (!_Opened) {
			_Uri = dataSpec.uri;
			_Opened = true;
			_Buffer=new RtpStreamBuffer();
			_RtspClient = new RtspClient(_Uri.toString(), _Buffer);
			_RtspClient.start();
		} else {
			throw new IOException("rtsp is opened");
		}
		return C.LENGTH_UNSET;
	}

	@Override
	public int read(byte[] buffer, int offset, int readLength) throws IOException {
		int bytes = 0;
		if (_MediaData == null || _MediaData.remaining() == 0) {
			_MediaData = _Buffer.pull();
		}

		if (_MediaData != null) {
	      bytes = _MediaData.remaining() >= readLength ? readLength : _MediaData.remaining();
	      _MediaData.get(buffer, offset, bytes);
	    }
		return bytes;
	}

	@Override
	public Uri getUri() {
		return _Uri;
	}

	@Override
	public void close() throws IOException {
		//TODO has some bug
		Log.d(TAG, "rtsp data source close!");
		if (_RtspClient != null) {
			_RtspClient.shutdown();
		}
	}

	public static class Factory implements DataSource.Factory{

		@Override
		public DataSource createDataSource() {
			return new RtspDataSource();
		}
		
	}
}
