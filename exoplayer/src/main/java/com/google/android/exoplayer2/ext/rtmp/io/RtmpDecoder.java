package com.google.android.exoplayer2.ext.rtmp.io;

import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

import com.google.android.exoplayer2.ext.rtmp.packets.Abort;
import com.google.android.exoplayer2.ext.rtmp.packets.Audio;
import com.google.android.exoplayer2.ext.rtmp.packets.Command;
import com.google.android.exoplayer2.ext.rtmp.packets.Data;
import com.google.android.exoplayer2.ext.rtmp.packets.RtmpHeader;
import com.google.android.exoplayer2.ext.rtmp.packets.RtmpPacket;
import com.google.android.exoplayer2.ext.rtmp.packets.SetChunkSize;
import com.google.android.exoplayer2.ext.rtmp.packets.SetPeerBandwidth;
import com.google.android.exoplayer2.ext.rtmp.packets.UserControl;
import com.google.android.exoplayer2.ext.rtmp.packets.Video;
import com.google.android.exoplayer2.ext.rtmp.packets.WindowAckSize;
import com.google.android.exoplayer2.ext.rtmp.packets.Acknowledgement;
import com.google.android.exoplayer2.ext.rtmp.packets.Aggregate;

/**
 * @author francois
 */
public class RtmpDecoder {

	private static final String TAG = "RtmpDecoder";

	private RtmpSessionInfo rtmpSessionInfo;

	public RtmpDecoder(RtmpSessionInfo rtmpSessionInfo) {
		this.rtmpSessionInfo = rtmpSessionInfo;
	}

	public RtmpPacket readPacket(InputStream in) throws IOException {
		RtmpHeader header = RtmpHeader.readHeader(in, rtmpSessionInfo);
		if(header==null){
			return null;
		}
		ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(header.getChunkStreamId());
		chunkStreamInfo.setPrevHeaderRx(header);

		if (header.getPacketLength() > rtmpSessionInfo.getRxChunkSize()) {
			// If the packet consists of more than one chunk,
			// store the chunks in the chunk stream until everything is read
			if (!chunkStreamInfo.storePacketChunk(in, rtmpSessionInfo.getRxChunkSize())) {
				// return null because of incomplete packet
//				Log.e(TAG, "return null");
				return null;
			} else {
//				Log.e(TAG, "return not null");
				// stored chunks complete packet, get the input stream of the
				// chunk stream;
				in = chunkStreamInfo.getStoredPacketInputStream();
			}
		}

		return readPacketImpl(header,in);
	}
	private RtmpPacket readPacketImpl(RtmpHeader header,InputStream in) throws IOException{
		RtmpPacket rtmpPacket;
		switch (header.getMessageType()) {
		case SET_CHUNK_SIZE:
			SetChunkSize setChunkSize = new SetChunkSize(header);
			setChunkSize.readBody(in);
			Log.d(TAG, "readPacket(): Setting chunk size to: " + setChunkSize.getChunkSize());
			rtmpSessionInfo.setRxChunkSize(setChunkSize.getChunkSize());
			return null;
		case ABORT:
			rtmpPacket = new Abort(header);
			break;
		case USER_CONTROL_MESSAGE:
			rtmpPacket = new UserControl(header);
			break;
		case WINDOW_ACKNOWLEDGEMENT_SIZE:
			rtmpPacket = new WindowAckSize(header);
			break;
		case SET_PEER_BANDWIDTH:
			rtmpPacket = new SetPeerBandwidth(header);
			break;
		case AUDIO:
			rtmpPacket = new Audio(header);
			break;
		case VIDEO:
			rtmpPacket = new Video(header);
			break;
		case COMMAND_AMF0:
			rtmpPacket = new Command(header);
			break;
		case DATA_AMF0:
			rtmpPacket = new Data(header);
			break;
		case ACKNOWLEDGEMENT:
			rtmpPacket = new Acknowledgement(header);
			break;
		case AGGREGATE_MESSAGE:
			rtmpPacket=new Aggregate(header);
			break;
		default:
			Log.e(TAG, "unknow pag length:" + header.getPacketLength());
			throw new IOException("No packet body implementation for message type: " + header.getMessageType());
		}
		rtmpPacket.readBody(in);
		return rtmpPacket;
	}


}
