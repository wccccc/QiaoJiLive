package com.google.android.exoplayer2.ext.rtmp.packets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.google.android.exoplayer2.ext.rtmp.Util;

public class FlvTag {
	private int _TagType;
	private int _TagLength;
	private int _Timestamp;
	private int _StreamID;
	private int _BackPoint;
	private byte[] _TagData;
	public int readTag(InputStream is) throws IOException{
		int tatolLength=11;
		_TagType=is.read();
		if(_TagType==-1){
			throw new IOException("read _TagType fail");
		}
		_TagLength=Util.readUnsignedInt24(is);
		byte[] timestamp=new byte[4];
		if(is.read(timestamp, 1, 3)!=3){
			throw new IOException("read timestamp fail");
		}
		timestamp[0]=(byte) is.read();
		_Timestamp=Util.toUnsignedInt32(timestamp);
		_StreamID=Util.readUnsignedInt24(is);
		_TagData=new byte[_TagLength];
		Util.readBytesUntilFull(is, _TagData);
		tatolLength+=_TagLength;
		_BackPoint=Util.readUnsignedInt32(is);
		tatolLength+=4;
		return tatolLength;
	}
	
	public byte[] getData(){
		ByteBuffer data=ByteBuffer.allocate(_TagLength+15);
		data.put((byte)_TagType);
		byte[] length=Util.unsignedInt32ToByteArray(_TagLength);
		data.put(length, 1, 3);
		byte[] timestamp=Util.unsignedInt32ToByteArray(_Timestamp);
		data.put(timestamp, 1, 3);
		data.put(timestamp[0]);
		byte[] streamID=Util.unsignedInt32ToByteArray(_StreamID);
		data.put(streamID, 1, 3);
		data.put(_TagData);
		data.put(Util.unsignedInt32ToByteArray(_BackPoint));
		data.position(0);
		return data.array();
	}

	public int getTimestamp() {
		return _Timestamp;
	}

	public void setTimestamp(int timestamp) {
		this._Timestamp = timestamp;
	}
	
}
