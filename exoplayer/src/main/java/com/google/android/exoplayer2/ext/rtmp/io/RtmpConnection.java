package com.google.android.exoplayer2.ext.rtmp.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import com.google.android.exoplayer2.ext.rtmp.RtmpPlayer;
import com.google.android.exoplayer2.ext.rtmp.Util;
import com.google.android.exoplayer2.ext.rtmp.amf.AmfNull;
import com.google.android.exoplayer2.ext.rtmp.amf.AmfNumber;
import com.google.android.exoplayer2.ext.rtmp.amf.AmfObject;
import com.google.android.exoplayer2.ext.rtmp.amf.AmfString;
import com.google.android.exoplayer2.ext.rtmp.packets.Abort;
import com.google.android.exoplayer2.ext.rtmp.packets.Aggregate;
import com.google.android.exoplayer2.ext.rtmp.packets.Audio;
import com.google.android.exoplayer2.ext.rtmp.packets.ContentData;
import com.google.android.exoplayer2.ext.rtmp.packets.Data;
import com.google.android.exoplayer2.ext.rtmp.packets.Command;
import com.google.android.exoplayer2.ext.rtmp.packets.Handshake;
import com.google.android.exoplayer2.ext.rtmp.packets.RtmpHeader.MessageType;
import com.google.android.exoplayer2.ext.rtmp.packets.RtmpPacket;
import com.google.android.exoplayer2.ext.rtmp.packets.SetPeerBandwidth;
import com.google.android.exoplayer2.ext.rtmp.packets.UserControl;
import com.google.android.exoplayer2.ext.rtmp.packets.Video;
import com.google.android.exoplayer2.ext.rtmp.packets.WindowAckSize;

/**
 * Main RTMP connection implementation class
 *
 * @author francois, leoma
 */
public class RtmpConnection implements RtmpPlayer {

	private static final String TAG = "RtmpConnection";
	private static final Pattern rtmpUrlPattern = Pattern.compile("^rtmp://([^/:]+)(:(\\d+))*/([^/]+)(/(.*))*$");

	private String appName;
	private String streamName;
	private String swfUrl;
	private String tcUrl;
	private String pageUrl;
	private Socket socket;
	private RtmpSessionInfo rtmpSessionInfo;
	private RtmpDecoder rtmpDecoder = new RtmpDecoder(rtmpSessionInfo);
	private BufferedInputStream inputStream;
	private BufferedOutputStream outputStream;
	private final ConcurrentLinkedQueue<ByteBuffer> mediaDataQueue = new ConcurrentLinkedQueue<>();
	private final Object mediaDataLock = new Object();
	private volatile boolean active = false;
	private volatile boolean connected = false;
	private AtomicInteger videoFrameCacheNumber = new AtomicInteger(0);
	private int currentStreamId = 0;
	private int transactionIdCounter = 0;
	private AmfString serverIpAddr;
	private AmfNumber serverPid;
	private AmfNumber serverId;

	private void handshake(InputStream in, OutputStream out) throws IOException {
		Handshake handshake = new Handshake();
		handshake.writeC0(out);
		handshake.writeC1(out); // Write C1 without waiting for S0
		out.flush();
		handshake.readS0(in);
		handshake.readS1(in);
		handshake.writeC2(out);
		handshake.readS2(in);
	}

	@Override
	public void connect(String url, int timeout) throws IOException {
		int port;
		String host;
		Matcher matcher = rtmpUrlPattern.matcher(url);
		if (matcher.matches()) {
			tcUrl = url.substring(0, url.lastIndexOf('/'));
//			Log.e(TAG, "tcUrl:"+tcUrl);//rtmp://localhost/tslsChannelLive/pr3Qevo
			swfUrl = "";
			pageUrl = "";
			host = matcher.group(1);
			String portStr = matcher.group(3);
			port = portStr != null ? Integer.parseInt(portStr) : 1935;
//			appName = matcher.group(4);//TODO
			int beginIndex=url.indexOf("//");
			appName = url.substring(url.indexOf('/', beginIndex+2)+1,url.lastIndexOf('/'));
//			streamName = matcher.group(6);//TODO
			streamName = url.substring(url.lastIndexOf('/')+1,url.length());
			Log.e(TAG, "url:"+url);
			Log.e(TAG, "appName："+appName);
			Log.e(TAG, "streamName："+streamName);
		} else {
			throw new IllegalArgumentException(
					"Invalid RTMP URL. Must be in format:" + "rtmp://host[:port]/application[/streamName]");
		}

		// socket connection
		Log.d(TAG, "connect() called. Host: " + host + ", port: " + port + ", appName: " + appName + ", playPath: "
				+ streamName);
		// Note the chunk size must be reset as 128 on each connection.
		rtmpSessionInfo = new RtmpSessionInfo();
		rtmpDecoder = new RtmpDecoder(rtmpSessionInfo);
		socket = new Socket();
		SocketAddress socketAddress = new InetSocketAddress(host, port);
		socket.connect(socketAddress, timeout);
		inputStream = new BufferedInputStream(socket.getInputStream());
		outputStream = new BufferedOutputStream(socket.getOutputStream());
		Log.d(TAG, "connect(): socket connection established, doing handhake...");
		handshake(inputStream, outputStream);
		active = true;
		Log.d(TAG, "connect(): handshake done");

		// Start the "main" handling thread
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Log.d(TAG, "starting main rx handler loop");
					handleRxPacketLoop();
				} catch (IOException ex) {
					Logger.getLogger(RtmpConnection.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}).start();

		rtmpConnect();
	}

	private void rtmpConnect() throws IOException, IllegalStateException {
		if (connected) {
			throw new IllegalStateException("Already connected or connecting to RTMP server");
		}

		// Mark session timestamp of all chunk stream information on connection.
		ChunkStreamInfo.markSessionBeginTimestamp();

		Log.d(TAG, "rtmpConnect(): Building 'connect' invoke packet");
		ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_OVER_CONNECTION);
		Command invoke = new Command("connect", ++transactionIdCounter, chunkStreamInfo);
		invoke.getHeader().setMessageStreamId(0);
		AmfObject args = new AmfObject();
		args.setProperty("app", appName);//appName
		args.setProperty("flashVer", "LNX 11,2,202,233"); //LNX 11,2,202,233
//		args.setProperty("swfUrl", swfUrl);
		args.setProperty("tcUrl", tcUrl);//tcUrl
		args.setProperty("fpad", false);
		args.setProperty("capabilities", 239);//239
		args.setProperty("audioCodecs", 3575);//1024 is aac
//		args.setProperty("audioCodecs", 4071);// 1024 is aac
		args.setProperty("videoCodecs", 252);
		args.setProperty("videoFunction", 1);
//		args.setProperty("pageUrl", pageUrl);
		args.setProperty("objectEncoding", 0);
		invoke.addData(args);
		sendRtmpPacket(invoke);
	}

	private void createStream() throws IllegalStateException, IOException {
		if (!connected) {
			throw new IllegalStateException("Not connected to RTMP server");
		}
		if (currentStreamId != 0) {
			throw new IllegalStateException("Current stream object has existed");
		}

		Log.d(TAG, "createStream(): Sending createStream command...");
		ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_OVER_CONNECTION);
		// transactionId == 2
		Command createStream = new Command("createStream", ++transactionIdCounter, chunkStreamInfo);
		createStream.addData(new AmfNull());
		sendRtmpPacket(createStream);
	}

	private void _checkbw() throws IllegalStateException, IOException {
		if (!connected) {
			throw new IllegalStateException("Not connected to RTMP server");
		}
		if (currentStreamId != 0) {
			throw new IllegalStateException("Current stream object has existed");
		}

		Log.d(TAG, "_checkbw(): Sending _checkbw command...");
		ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_OVER_CONNECTION);
		// transactionId == 3
		Command _checkbw = new Command("_checkbw", ++transactionIdCounter, chunkStreamInfo);
		_checkbw.addData(new AmfNull());
		sendRtmpPacket(_checkbw);
	}

	private void play() throws IOException {
		Log.d(TAG, "play(): Sending play command...");
		// transactionId == 3
		Command play = new Command("play", ++transactionIdCounter);
		play.getHeader().setChunkStreamId(ChunkStreamInfo.RTMP_CID_OVER_STREAM);
		play.getHeader().setMessageStreamId(currentStreamId);
		play.addData(new AmfNull()); // command object: null for "play"
		play.addData(streamName);//streamName
		play.addData(-1);
		sendRtmpPacket(play);

		setBufferLength();
	}

	private void setBufferLength() throws IllegalStateException, IOException {
		if (!connected) {
			throw new IllegalStateException("Not connected to RTMP server");
		}

		Log.d(TAG, "setBufferLength(): Sending setBufferLength command...");
		ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_PROTOCOL_CONTROL);
		UserControl setBufferLength = new UserControl(UserControl.Type.SET_BUFFER_LENGTH, chunkStreamInfo);
		setBufferLength.setEventData(currentStreamId, 3000);
		sendRtmpPacket(setBufferLength);
	}

	@Override
	public ByteBuffer poll() {
		while (active) {
			while (!mediaDataQueue.isEmpty()) {
				return mediaDataQueue.poll();
			}
			// Wait for next received packet
			synchronized (mediaDataLock) {
				try {
					mediaDataLock.wait(500);
				} catch (InterruptedException ex) {
					// Ignore
				}
			}
		}

		return null;
	}

	@Override
	public void close() throws IllegalStateException, IOException {
		if (socket != null) {
			closeStream();
		}
		shutdown();
	}

	private void closeStream() throws IOException {
		if (!connected) {
			throw new IllegalStateException("Not connected to RTMP server");
		}
		if (currentStreamId == 0) {
			throw new IllegalStateException("No current stream object exists");
		}
		Log.d(TAG, "closeStream(): setting current stream ID to 0");
		Command closeStream = new Command("closeStream", 0);
		closeStream.getHeader().setChunkStreamId(ChunkStreamInfo.RTMP_CID_OVER_STREAM);
		closeStream.getHeader().setMessageStreamId(currentStreamId);
		closeStream.addData(new AmfNull());
		sendRtmpPacket(closeStream);
	}

	private void shutdown() {
		// shutdown handleRxPacketLoop
		active = false;
		mediaDataQueue.clear();
		synchronized (mediaDataLock) {
			mediaDataLock.notifyAll();
		}

		// shutdown socket as well as its input and output stream
		if (socket != null) {
			try {
				socket.shutdownInput();
				socket.shutdownOutput();
				socket.close();
				Log.d(TAG, "socket closed");
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		reset();
	}

	private void reset() {
		active = false;
		connected = false;
		tcUrl = null;
		swfUrl = null;
		pageUrl = null;
		appName = null;
		streamName = null;
		currentStreamId = 0;
		transactionIdCounter = 0;
		videoFrameCacheNumber.set(0);
		serverIpAddr = null;
		serverPid = null;
		serverId = null;
		rtmpSessionInfo = null;
		rtmpDecoder = null;
	}

	/** Transmit the specified RTMP packet */
	private void sendRtmpPacket(RtmpPacket rtmpPacket) throws IOException {
		ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(rtmpPacket.getHeader().getChunkStreamId());
		chunkStreamInfo.setPrevHeaderTx(rtmpPacket.getHeader());
		if (!(rtmpPacket instanceof Video || rtmpPacket instanceof Audio)) {
			rtmpPacket.getHeader().setAbsoluteTimestamp((int) ChunkStreamInfo.markAbsoluteTimestamp());
		}
		rtmpPacket.writeTo(outputStream, rtmpSessionInfo.getTxChunkSize(), chunkStreamInfo);
		Log.d(TAG, "wrote packet: " + rtmpPacket + ", size: " + rtmpPacket.getHeader().getPacketLength());
		if (rtmpPacket instanceof Command) {
			rtmpSessionInfo.addInvokedCommand(((Command) rtmpPacket).getTransactionId(),
					((Command) rtmpPacket).getCommandName());
		}
		outputStream.flush();
	}

	private void handleRxPacketLoop() throws IOException {
		// Handle all queued received RTMP packets
		while (active) {
			try {
				// It will be blocked when no data in input stream buffer
				RtmpPacket rtmpPacket = rtmpDecoder.readPacket(inputStream);
				if (rtmpPacket != null) {
//					Log.e(TAG,"MessageType:"+rtmpPacket.getHeader().getMessageType());
					// Log.d(TAG, "handleRxPacketLoop(): RTMP rx packet
					// messagetype: " +
					// rtmpPacket.getHeader().getMessageType());
					switch (rtmpPacket.getHeader().getMessageType()) {
					case ABORT:
						rtmpSessionInfo.getChunkStreamInfo(((Abort) rtmpPacket).getChunkStreamId()).clearStoredChunks();
						break;
					case USER_CONTROL_MESSAGE:
						UserControl user = (UserControl) rtmpPacket;
						switch (user.getType()) {
						case STREAM_BEGIN:
							if (currentStreamId != user.getFirstEventData()) {
								throw new IllegalStateException("Current stream ID error!");
							}
							break;
						case PING_REQUEST:
							ChunkStreamInfo channelInfo = rtmpSessionInfo
									.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_PROTOCOL_CONTROL);
							Log.d(TAG, "handleRxPacketLoop(): Sending PONG reply..");
							UserControl pong = new UserControl(user, channelInfo);
							sendRtmpPacket(pong);
							break;
						case STREAM_EOF:
							Log.i(TAG, "handleRxPacketLoop(): Stream EOF reached, closing RTMP writer...");
							break;
						default:
							// Ignore...
							Log.d(TAG, "unknow USER_CONTROL_MESSAGE:" + user.getType()+" size:"+user.getHeader().getPacketLength());
							break;
						}
						break;
					case WINDOW_ACKNOWLEDGEMENT_SIZE:
						WindowAckSize windowAckSize = (WindowAckSize) rtmpPacket;
						int size = windowAckSize.getAcknowledgementWindowSize();
						Log.d(TAG, "handleRxPacketLoop(): Setting acknowledgement window size: " + size);
						rtmpSessionInfo.setAcknowledgmentWindowSize(size);
						break;
					case SET_PEER_BANDWIDTH:
						SetPeerBandwidth bw = (SetPeerBandwidth) rtmpPacket;
						rtmpSessionInfo.setAcknowledgmentWindowSize(bw.getAcknowledgementWindowSize());
						int acknowledgementWindowsize = rtmpSessionInfo.getAcknowledgementWindowSize();
						ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo
								.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_PROTOCOL_CONTROL);
						Log.d(TAG,
								"handleRxPacketLoop(): Send acknowledgement window size: " + acknowledgementWindowsize);
						sendRtmpPacket(new WindowAckSize(acknowledgementWindowsize, chunkStreamInfo));
						// Set socket option
						socket.setSendBufferSize(acknowledgementWindowsize);
						break;
					case DATA_AMF0:

						Data data = (Data) rtmpPacket;
						Log.d(TAG, "handleRxPacketLoop(): " + ((Data) rtmpPacket).getType());
						if (data.getType().contains("onMetaData")) {
							addPacketToDataQueue(rtmpPacket);
						}
						break;
					case COMMAND_AMF0:
						handleRxInvoke((Command) rtmpPacket);
						break;
					case VIDEO:
						int header = ((ContentData)rtmpPacket).getData()[0];
					    int frameType = (header >> 4) & 0x0F;
//					    Log.e(TAG, "frameType:"+frameType);
					    if(frameType!=5){
					    	int compositionTimeMs=0;
							byte[] compositionTimeMstoByte=new byte[4];
							compositionTimeMstoByte[1]=((ContentData)rtmpPacket).getData()[2];
							compositionTimeMstoByte[2]=((ContentData)rtmpPacket).getData()[3];
							compositionTimeMstoByte[3]=((ContentData)rtmpPacket).getData()[4];
							compositionTimeMs=Util.toUnsignedInt24(compositionTimeMstoByte);
							if(rtmpPacket.getHeader().getAbsoluteTimestamp()==0&&compositionTimeMs>1000){
								Log.d(TAG, "reset this packet");
								((ContentData)rtmpPacket).getData()[2]=0;
								((ContentData)rtmpPacket).getData()[3]=0;
								((ContentData)rtmpPacket).getData()[4]=0;
							}
					    }
						
					case AUDIO:
						if (rtmpPacket.getHeader().getPacketLength() != 0) {
							addPacketToDataQueue(rtmpPacket);
						}
						break;
					case AGGREGATE_MESSAGE:
						ByteBuffer mediaData = ByteBuffer.allocate(rtmpPacket.getHeader().getPacketLength());
						mediaData.position(0);
						mediaData.put(((Aggregate) rtmpPacket).getData());
						mediaData.position(0);
						mediaDataQueue.add(mediaData);
						synchronized (mediaDataLock) {
							mediaDataLock.notifyAll();
						}
						break;
					default:
						Log.w(TAG, "handleRxPacketLoop(): Not handling unimplemented/unknown packet of type: "
								+ rtmpPacket.getHeader().getMessageType());
						break;
					}
				}
			} catch (EOFException eof) {
				active = false;
				eof.printStackTrace();
			} catch (SocketException se) {
				active = false;
				se.printStackTrace();
			} catch (IOException ioe) {
				active = false;
				ioe.printStackTrace();
			}
		}
	}

	private void addPacketToDataQueue(RtmpPacket rtmpPacket) {
		// Log.e(TAG, "addPacketToDataQueue");
		ByteBuffer mediaData = collectMediaData(rtmpPacket);
		if(mediaData==null){
			return;
		}
		mediaData.position(0);
		mediaDataQueue.add(mediaData);
		synchronized (mediaDataLock) {
			mediaDataLock.notifyAll();
		}
	}


	private ByteBuffer collectMediaData(RtmpPacket packet) {
		ByteBuffer mediaData = null;
		int length = packet.getHeader().getPacketLength();
		mediaData = ByteBuffer.allocate(11 + length + 4);
		mediaData.position(0);
		mediaData.put(packet.getHeader().getMessageType().getValue());
		byte lengthToBytes[] = new byte[3];
		lengthToBytes[0] = (byte) ((length & 0x00FF0000) >> 16);
		lengthToBytes[1] = (byte) ((length & 0x0000FF00) >> 8);
		lengthToBytes[2] = (byte) (length & 0x000000FF);
		mediaData.put(lengthToBytes);
		int timestamp = packet.getHeader().getAbsoluteTimestamp();
		byte timestampToBytes[] = new byte[4];
		timestampToBytes[0] = (byte) ((timestamp & 0x00FF0000) >> 16);
		timestampToBytes[1] = (byte) ((timestamp & 0x0000FF00) >> 8);
		timestampToBytes[2] = (byte) (timestamp & 0x000000FF);
		timestampToBytes[3] = (byte) ((timestamp & 0xFF000000) >> 24);
		mediaData.put(timestampToBytes);// timestamp
		mediaData.put(new byte[3]);// stream id
		if (packet.getHeader().getMessageType() == MessageType.DATA_AMF0) {
			mediaData.put(((Data) packet).getByteData());
		} else {
			mediaData.put(((ContentData) packet).getData());
		}
//		Log.e(TAG,packet.getHeader().getMessageType()+":"+timestamp );
		mediaData.put(new byte[4]);
		return mediaData;
	}

	private void handleRxInvoke(Command invoke) throws IOException {
		String commandName = invoke.getCommandName();

		if (commandName.equals("_result")) {
			// This is the result of one of the methods invoked by us
			String method = rtmpSessionInfo.takeInvokedCommand(invoke.getTransactionId());

			Log.d(TAG, "handleRxInvoke: Got result for invoked method: " + method);
			if ("connect".equals(method)) {
				// Capture server ip/pid/id information if any
				onSrsServerInfo(invoke);
				// We can now send createStream commands
				String code = ((AmfString) ((AmfObject) invoke.getData().get(1)).getProperty("code")).getValue();
				if (code.equals("NetConnection.Connect.Success")) {
					connected = true;
					setBufferLength();
					createStream();
					_checkbw();
				}
			} else if ("createStream".contains(method)) {
				// Get stream id
				currentStreamId = (int) ((AmfNumber) invoke.getData().get(1)).getValue();
				Log.d(TAG, "handleRxInvoke(): Stream ID to play: " + currentStreamId);
				play();
			} else if ("play".contains(method)) {
				Log.d(TAG, "handleRxInvoke(): response play");
			} else {
				Log.w(TAG, "handleRxInvoke(): '_result' message received for unknown method: " + method);
			}
		} else if (commandName.equals("onBWDone")) {
			Log.d(TAG, "handleRxInvoke(): 'onBWDone'");
		} else if (commandName.equals("onStatus")) {
			String code = ((AmfString) ((AmfObject) invoke.getData().get(1)).getProperty("code")).getValue();
			Log.d(TAG, "handleRxInvoke(): 'onStatus' code :" + code);
		} else {
			Log.e(TAG, "handleRxInvoke(): Unknown/unhandled server invoke: " + invoke);
		}
	}

	private String onSrsServerInfo(Command invoke) {
		// SRS server special information
		AmfObject objData = (AmfObject) invoke.getData().get(1);
		if ((objData).getProperty("data") instanceof AmfObject) {
			objData = ((AmfObject) objData.getProperty("data"));
			serverIpAddr = (AmfString) objData.getProperty("srs_server_ip");
			serverPid = (AmfNumber) objData.getProperty("srs_pid");
			serverId = (AmfNumber) objData.getProperty("srs_id");
		}
		String info = "";
		info += serverIpAddr == null ? "" : " ip: " + serverIpAddr.getValue();
		info += serverPid == null ? "" : " pid: " + (int) serverPid.getValue();
		info += serverId == null ? "" : " id: " + (int) serverId.getValue();
		return info;
	}

	@Override
	public final String getServerIpAddr() {
		return serverIpAddr == null ? null : serverIpAddr.getValue();
	}

	@Override
	public final int getServerPid() {
		return serverPid == null ? 0 : (int) serverPid.getValue();
	}

	@Override
	public final int getServerId() {
		return serverId == null ? 0 : (int) serverId.getValue();
	}
}
