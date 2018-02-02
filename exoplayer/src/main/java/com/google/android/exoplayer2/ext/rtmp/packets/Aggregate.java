package com.google.android.exoplayer2.ext.rtmp.packets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Aggregate extends RtmpPacket{
	private ArrayList<FlvTag> _FlvTags;
	private int _FirstTagTimestamp=-1;

	public Aggregate(RtmpHeader header) {
		super(header);
		_FlvTags=new ArrayList<FlvTag>();
	}

	@Override
	public void readBody(InputStream in) throws IOException {
		int tatol=0;
		while(tatol<header.getPacketLength()){
			FlvTag tag=new FlvTag();
			tatol+=tag.readTag(in);
			if(_FirstTagTimestamp==-1){
				_FirstTagTimestamp=tag.getTimestamp();
				tag.setTimestamp(header.getAbsoluteTimestamp());
			}else{
				tag.setTimestamp(header.getAbsoluteTimestamp()+(tag.getTimestamp()-_FirstTagTimestamp));
			}
			_FlvTags.add(tag);
		}
		
	}

	@Override
	protected void writeBody(OutputStream out) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	public byte[] getData(){
		ByteBuffer data=ByteBuffer.allocate(header.getPacketLength());
		for(FlvTag tag:_FlvTags){
			data.put(tag.getData());
		}
		data.position(0);
		return data.array();
	}
	
}
