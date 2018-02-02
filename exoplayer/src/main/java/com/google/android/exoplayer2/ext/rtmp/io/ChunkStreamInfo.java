package com.google.android.exoplayer2.ext.rtmp.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.android.exoplayer2.ext.rtmp.Util;
import com.google.android.exoplayer2.ext.rtmp.packets.RtmpHeader;

/**
 * Chunk stream channel information
 *
 * @author francois, leo
 */
public class ChunkStreamInfo {

  public static final byte RTMP_CID_PROTOCOL_CONTROL = 0x02;
  public static final byte RTMP_CID_OVER_CONNECTION = 0x03;
  public static final byte RTMP_CID_OVER_CONNECTION2 = 0x04;
  public static final byte RTMP_CID_OVER_STREAM = 0x05;
  public static final byte RTMP_CID_VIDEO = 0x06;
  public static final byte RTMP_CID_AUDIO = 0x07;
  public static final byte RTMP_CID_OVER_STREAM2 = 0x08;
  private RtmpHeader prevHeaderRx;
  private RtmpHeader prevHeaderTx;
  private static long sessionBeginTimestamp;
  private long realLastTimestamp = System.nanoTime() / 1000000;  // Do not use wall time!
  private ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 128);

  /**
   * @return the previous header that was received on this channel, or <code>null</code> if no previous header was received
   */
  public RtmpHeader prevHeaderRx() {
    return prevHeaderRx;
  }

  /**
   * Sets the previous header that was received on this channel, or <code>null</code> if no previous header was sent
   */
  public void setPrevHeaderRx(RtmpHeader previousHeader) {
    this.prevHeaderRx = previousHeader;
  }

  /**
   * @return the previous header that was transmitted on this channel
   */
  public RtmpHeader getPrevHeaderTx() {
    return prevHeaderTx;
  }

  public boolean canReusePrevHeaderTx(RtmpHeader.MessageType forMessageType) {
    return (prevHeaderTx != null && prevHeaderTx.getMessageType() == forMessageType);
  }

  /**
   * Sets the previous header that was transmitted on this channel
   */
  public void setPrevHeaderTx(RtmpHeader prevHeaderTx) {
    this.prevHeaderTx = prevHeaderTx;
  }

  /**
   * Sets the session beginning timestamp for all chunks
   */
  public static void markSessionBeginTimestamp() {
    sessionBeginTimestamp = System.nanoTime() / 1000000;
  }

  /**
   * Utility method for calculating & synchronizing transmitted timestamps
   */
  public static long markAbsoluteTimestamp() {
    return System.nanoTime() / 1000000 - sessionBeginTimestamp;
  }

  /**
   * Utility method for calculating & synchronizing transmitted timestamp deltas
   */
  public long markDeltaTimestamp() {
    long currentTimestamp = System.nanoTime() / 1000000;
    long diffTimestamp = currentTimestamp - realLastTimestamp;
    realLastTimestamp = currentTimestamp;
    return diffTimestamp;
  }

  /**
   * @return <code>true</code> if all packet data has been stored, or <code>false</code> if not
   */
//  public boolean storePacketChunk(InputStream in, int chunkSize) throws IOException {
//	  int remainingBytes = prevHeaderRx.getPacketLength() - baos.size();
//      int packetLenfth=prevHeaderRx.getPacketLength();
//      if(remainingBytes<chunkSize){
//      	 byte[] chunk = new byte[Math.min(remainingBytes, chunkSize)];
//           Util.readBytesUntilFull(in, chunk);
//           baos.write(chunk);
//      }else{
//      	byte[] chunk = new byte[chunkSize];
//      	Util.readBytesUntilFull(in, chunk);
//      	baos.write(chunk);
//      	packetLenfth-=chunkSize;
//      	while(packetLenfth>0){
//      		byte chunkType;
//      		int chunkStreamID = 0;
//      		byte[] timestamp = null;
//      		byte[] chunkBasicHeader = new byte[1];
//      		Util.readBytesUntilFull(in, chunkBasicHeader);
//      		chunkType=(byte) ((0xff & chunkBasicHeader[0]) >>> 6);
//      		chunkStreamID = (((chunkBasicHeader[0] << 2) & 0xFF) >>> 2) & 0xFF;
//      		if (chunkStreamID == 0) {
//      			byte[] b = new byte[1];
//      			Util.readBytesUntilFull(in, b);
//      		} else if (chunkStreamID == 1) {
//      			byte[] b = new byte[2];
//      			Util.readBytesUntilFull(in, b);
//      		}
//      		if (chunkType == 0) {
//      			timestamp=new byte[3];
//      			Util.readBytesUntilFull(in, timestamp);
//      			in.skip(8);
//      		} else if (chunkType == 1) {
//      			timestamp=new byte[3];
//      			Util.readBytesUntilFull(in, timestamp);
//      			in.skip(4);
//      		} else if (chunkType == 2) {
//      			timestamp=new byte[3];
//      			Util.readBytesUntilFull(in, timestamp);
//      		}
//      		if(timestamp!=null){
//      			if(bytesToInt(timestamp)==16777215){
//      				in.skip(4);
//      			}
//      		}
//      		//chunk data
//      		byte[] chunk1 = new byte[Math.min(packetLenfth, chunkSize)];
//      		Util.readBytesUntilFull(in, chunk1);
//          	baos.write(chunk1);
//      		packetLenfth-=chunk1.length;
//      		
//      	}
//      }
//      return true;
//  }
  public boolean storePacketChunk(InputStream in, int chunkSize) throws IOException {
      final int remainingBytes = prevHeaderRx.getPacketLength() - baos.size();
      byte[] chunk = new byte[Math.min(remainingBytes, chunkSize)];
      Util.readBytesUntilFull(in, chunk);
      baos.write(chunk);
      return (baos.size() == prevHeaderRx.getPacketLength());
  }

  public static int bytesToInt(byte[] b) {

		int mask = 0xff;
		int temp = 0;
		int n = 0;
		for (int i = 0; i < b.length; i++) {
			n <<= 8;
			temp = b[i] & mask;
			n |= temp;
		}
		return n;
	}
  public ByteArrayInputStream getStoredPacketInputStream() {
    ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
    baos.reset();
    return bis;
  }

  /**
   * Clears all currently-stored packet chunks (used when an ABORT packet is received)
   */
  public void clearStoredChunks() {
    baos.reset();
  }
}
