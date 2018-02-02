package com.google.android.exoplayer2.ext.rtsp;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;



import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.exoplayer2.ext.rtsp.Socket.RtpSocket;
import com.google.android.exoplayer2.ext.rtsp.Stream.RtpStream;
import com.google.android.exoplayer2.ext.rtsp.Stream.TsStream;
import com.google.android.exoplayer2.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class RtspClient {

    private final static String tag = "RtspClient";
    private final static String UserAgent = "Rtsp/0.1";
    private final static int STATE_STARTED = 0x00;
    private final static int STATE_STARTING = 0x01;
    private final static int STATE_STOPPING = 0x02;
    private final static int STATE_STOPPED = 0x03;
    private final static String METHOD_UDP = "udp";
    private final static String METHOD_TCP = "tcp";
    private final static int TRACK_VIDEO = 0x01;
    private final static int TRACK_AUDIO = 0x02;

    private class Parameters {
        private String host;
        private String address;
        private int port;
        private int rtpPort;
        private int serverPort;
    }

    public class SDPInfo {
        public boolean audioTrackFlag;
        public boolean videoTrackFlag;
        public String videoTrack;
        public String audioTrack;
        public String SPS;
        public String PPS;
        public int packetizationMode;
        @Override
        public String toString() {
            JSONObject obj=new JSONObject();
            try {
                obj.put("audioTrackFlag",audioTrackFlag);
                obj.put("videoTrackFlag",videoTrackFlag);
                obj.put("videoTrack",videoTrack);
                obj.put("audioTrack",audioTrack);
                obj.put("SPS",SPS);
                obj.put("PPS",PPS);
                obj.put("packetizationMode",packetizationMode);
                return obj.toString();
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private Socket mSocket;
    private BufferedReader mBufferreader;
    private OutputStream mOutputStream;
    private Parameters mParams;
    private Handler mHandler;
    private int CSeq;
    private int mState;
    private String mSession;
    private RtpSocket mRtpSocket;
    private boolean isTCPtranslate;
    private static boolean Describeflag = false; //used to get SDP info
    private static SDPInfo sdpInfo;
    private String authorName, authorPassword, authorBase64;
    private HandlerThread thread;

    private RtpStream mRtpStream;

    private RtpStreamBuffer mBuffer;

    public RtspClient(String address,String name, String password,RtpStreamBuffer buffer) {
        this("udp",address,name,password,buffer);
    }
    public RtspClient(String address,RtpStreamBuffer buffer) {
        this("udp",address,null,null,buffer);
    }

    public RtspClient(String method, String address,RtpStreamBuffer buffer) {
        this(method,address,null,null,buffer);
    }

    public RtspClient(String method, String address,String name,String password,RtpStreamBuffer buffer) {
        mBuffer=buffer;
        authorName = name;
        authorPassword = password;
        parseUrl(address);
        if( method.equalsIgnoreCase(METHOD_UDP) ) {
            isTCPtranslate = false;
        } else if( method.equalsIgnoreCase(METHOD_TCP)) {
            isTCPtranslate = true;
        }
    }

    private void parseUrl(String address){
    	String url = address.substring(address.indexOf("//") + 2);
        url = url.substring(0,url.indexOf("/"));
        String[] tmp = url.split(":");
        Log.d(tag, url);
        if(tmp.length == 1) {
        	ClientConfig(tmp[0], address, 554);
        }else if(tmp.length == 2) {
        	ClientConfig(tmp[0], address, Integer.parseInt(tmp[1]));
        }
    }
    private void ClientConfig(String host, String address, int port) {
        mParams = new Parameters();
        sdpInfo = new SDPInfo();
        mParams.host = host;
        mParams.port = port;
        mParams.address = address.substring(7);
        CSeq = 0;
        mState = STATE_STOPPED;
        mSession = null;
        if(authorName == null && authorPassword == null) {
            authorBase64 = null;
        }
        else {
            authorBase64 = Base64.encodeToString((authorName+":"+authorPassword).getBytes(),Base64.DEFAULT);
        }

        final Semaphore signal = new Semaphore(0);
        thread = new HandlerThread("RTSPCilentThread") {
            protected void onLooperPrepared() {
                mHandler = new Handler();
                signal.release();
            }
        };
        thread.start();
        signal.acquireUninterruptibly();
    }

    public void start() {
        mHandler.post(startConnection);
    }

    private Runnable startConnection = new Runnable() {
        @Override
        public void run() {
            if (mState != STATE_STOPPED) return;
            mState = STATE_STARTING;

            Log.d(tag, "Start to connect the server...");

            try {
                tryConnection();
                mHandler.post(sendGetParameter);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(tag, e.toString());
                abort();
            }
        }
    };

    private Runnable sendGetParameter = new Runnable() {
        @Override
        public void run() {
            try {
                sendRequestGetParameter();
                mHandler.postDelayed(sendGetParameter,55000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private void abort() {
        try {
            if(mState == STATE_STARTED) sendRequestTeardown();
        } catch ( IOException e ) {}
        try {
            if(mSocket!=null) mSocket.close();
        } catch ( IOException e ) {}
        mState = STATE_STOPPED;
        mHandler.removeCallbacks(startConnection);
        mHandler.removeCallbacks(sendGetParameter);
    }

    void shutdown(){
        if(mState == STATE_STARTED ||
                mState == STATE_STARTING) {
            mHandler.removeCallbacks(startConnection);
            mHandler.removeCallbacks(sendGetParameter);
            try {
                if(mRtpStream!=null) {
                    mRtpStream.stop();
                    mRtpStream = null;
                }
                if(mRtpSocket!=null) {
                    mRtpSocket.stop();
                    mRtpSocket = null;
                }
                if(mState == STATE_STARTED) sendRequestTeardown();
                if(mSocket!=null) {
                    mSocket.close();
                    mSocket = null;
                }
                mState = STATE_STOPPED;
                thread.quit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(mBuffer!=null){
        	mBuffer.release();
		}
    }

    public boolean isStarted() {
        return mState == STATE_STARTED | mState == STATE_STARTING;
    }
    
    private void open() throws IOException{
    	Log.d(tag, "open!");
    	mSocket = new Socket(mParams.host, mParams.port);
        mBufferreader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
        mOutputStream = mSocket.getOutputStream();
        Log.d(tag, "open successs!");
    }

    private void tryConnection () throws IOException {
        open();
        mState = STATE_STARTING;
        sendRequestOptions();
        sendRequestDescribe();
        sendRequestSetup();
        sendRequestPlay();
    }
    
    private void redirect(String url) throws IOException{
    	if(url==null){
    		Log.e(tag, "redirect url is null!");
    		return;
    	}
    	parseUrl(url);
    	mBufferreader.close();
    	mOutputStream.close();
    	mSocket.close();
    	open();
    	sendRequestDescribe();
    }

    private void sendRequestOptions() throws IOException {
        Log.d(tag,"sendRequestOptions()");
        String request = "OPTIONS rtsp://" + mParams.address + " RTSP/1.0\r\n" + addHeaders()
                + "\r\n";
        Log.d(tag, "request:\r\n"+request);
        mOutputStream.write(request.getBytes("UTF-8"));
        Response.parseResponse(mBufferreader);
    }

    private void sendRequestDescribe() throws IOException {
        String request = "DESCRIBE rtsp://" + mParams.address + " RTSP/1.0\r\n" + addHeaders()
                + "Accept: application/sdp\r\n"
                + "x-NAT: "+mSocket.getLocalAddress().getHostAddress()+":"+mSocket.getPort()+"\r\n"//TODO
                + "\r\n";
        Log.d(tag, request);
        Describeflag = true;
        mOutputStream.write(request.getBytes("UTF-8"));
        Response response=Response.parseResponse(mBufferreader);
        if(response.state==302){
        	redirect(response.headers.get("location"));
        }
    }
    private static volatile int clientPort=55600;
    private void sendRequestSetup() throws IOException {
        Matcher matcher;
        String request;
        clientPort+=2;
        if(clientPort>55680){
        	clientPort=55600;
        }
        String client_Port=clientPort+"-"+(clientPort+1);
        if ( isTCPtranslate ) {
            request = "SETUP rtsp://" + mParams.address + "/" + sdpInfo.videoTrack + " RTSP/1.0\r\n"
                    + "Transport: RTP/AVP/TCP;unicast;client_port="+"client_Port"+ "\r\n"
                    + addHeaders()
                    + "\r\n";
        } else {
            request = "SETUP rtsp://" + mParams.address + "/" + sdpInfo.videoTrack +" RTSP/1.0\r\n"
                    + "Transport: RTP/AVP/UDP;unicast;client_port="+client_Port+";unicast;interleaved=0-1,MP2T/RTP/TCP;unicast;interleaved=0-1" + "\r\n"
                    + addHeaders()
                    + "\r\n";
        }
        Log.d(tag, request);
        mOutputStream.write(request.getBytes("UTF-8"));
        Response mResponse = Response.parseResponse(mBufferreader);

        //there has two different session type, one is without timeout , another is with timeout
        matcher = Response.regexSessionWithTimeout.matcher(mResponse.headers.get("session"));
        if(matcher.find())  mSession = matcher.group(1);
        else mSession = mResponse.headers.get("session");
        Log.d(tag,"the session is " + mSession);

        //get the port information and start the RTP socket, ready to receive data
        matcher=Response.regexClientPort.matcher(mResponse.headers.get("transport"));
        if(matcher.find()) {
            Log.d(tag, "The client port is:" + matcher.group(1));
            mParams.rtpPort = Integer.parseInt(matcher.group(1));
            if(!isTCPtranslate){
            	Matcher serverPort=Response.regexServerPort.matcher(mResponse.headers.get("transport"));
            	if(serverPort.find()){
            		mParams.serverPort = Integer.parseInt(serverPort.group(1));
            		Log.d(tag, "The server port is:"+mParams.serverPort);
            	}
            }
            
            //TODO prepare for the video decoder
            mRtpStream= new TsStream(mBuffer);

            if(isTCPtranslate) mRtpSocket = new RtpSocket(isTCPtranslate, mParams.rtpPort, mParams.host, -1,TRACK_VIDEO);
            else mRtpSocket = new RtpSocket(isTCPtranslate, mParams.rtpPort, mParams.host, mParams.serverPort,TRACK_VIDEO);
            mRtpSocket.startRtpSocket();
            mRtpSocket.setStream(mRtpStream);
        } else {
            if(isTCPtranslate) {
                Log.d(tag,"Without get the transport port infom, use the rtsp tcp socket!");
                mParams.rtpPort = mParams.port;

                //TODO prepare for the video decoder
                mRtpStream= new TsStream(mBuffer);

                mRtpSocket = new RtpSocket(isTCPtranslate,mParams.rtpPort,mParams.host,-2,TRACK_VIDEO);
                mRtpSocket.setRtspSocket(mSocket);
                mRtpSocket.startRtpSocket();
                mRtpSocket.setStream(mRtpStream);
                mState = STATE_STARTED;
            }
        }
    }

    private void sendRequestPlay() throws IOException {
        String request = "PLAY rtsp://" + mParams.address + " RTSP/1.0\r\n"
//                + "Range: npt=0.000-\r\n"
                + addHeaders()
                + "\r\n";
        Log.d(tag, request);
        mOutputStream.write(request.getBytes("UTF-8"));
        Response.parseResponse(mBufferreader);
        //TODO
//        initUDPNAT();
//        mRtpSocket.sendUDPPacket(mybuf);
    }

    private void sendRequestTeardown() throws IOException {
        String request = "TEARDOWN rtsp://" + mParams.address + "/" + sdpInfo.videoTrack + " RTSP/1.0\r\n" + addHeaders()
                + "\r\n";
        Log.d(tag, request.substring(0, request.indexOf("\r\n")));
        mOutputStream.write(request.getBytes("UTF-8"));
        mState = STATE_STOPPING;
    }

    private void sendRequestGetParameter() throws IOException {
        String request = "GET_PARAMETER rtsp://" + mParams.address + "/" + sdpInfo.videoTrack + " RTSP/1.0\r\n" + addHeaders()
                + "\r\n";
        Log.d(tag, request.substring(0, request.indexOf("\r\n")));
        mOutputStream.write(request.getBytes("UTF-8"));
        Response.parseResponse(mBufferreader);
    }
    private byte mybuf[]=new byte[84];
    private void initUDPNAT(){
        //ZXV10STB
        mybuf[0]=0x5A;
        mybuf[1]=0x58;
        mybuf[2]=0x56;
        mybuf[3]=0x31;
        mybuf[4]=0x30;
        mybuf[5]=0x53;
        mybuf[6]=0x54;
        mybuf[7]=0x42;
        
        byte[] sessionID=Util.getBytesFromHexString(mSession);
        mybuf[8] = sessionID[0];
        mybuf[9] = sessionID[1];
        mybuf[10] = sessionID[2];
        mybuf[11] = sessionID[3];
        
//        int sessionID=Integer.parseInt(mSession, 16);
//        mybuf[8] = (byte) (sessionID >> 24);
//        mybuf[9] = (byte) ((sessionID >> 16) & 0xff);
//        mybuf[10] = (byte) ((sessionID >> 8) & 0xff);
//        mybuf[11] = (byte) (sessionID &0xff);
        byte ip[]=mSocket.getInetAddress().getAddress();
        mybuf[12]=ip[3];
        mybuf[13]=ip[2];
        mybuf[14]=ip[1];
        mybuf[15]=ip[0];
        short udpportid = (short) (10000+Math.random());//一个大于10000随机数
        mybuf[16] = (byte) (udpportid >> 8);
        mybuf[17] = (byte) (udpportid & 0xff);
        short tcpportid= (short) mParams.port;
        mybuf[18] = (byte) (tcpportid >> 8);
        mybuf[19] = (byte) (tcpportid & 0xff);
    }

    private String addHeaders() {
        return "CSeq: " + (++CSeq) + "\r\n"
                + ((authorBase64 == null)?"":("Authorization: Basic " +authorBase64 +"\r\n"))
                + "User-Agent: " + UserAgent + "\r\n"
                + ((mSession == null)?"":("Session: " + mSession + "\r\n"));

    }

    private static class Response {

        static final Pattern regexStatus = Pattern.compile("RTSP/\\d.\\d (\\d+) .+",Pattern.CASE_INSENSITIVE);
        static final Pattern regexHeader = Pattern.compile("(\\S+): (.+)",Pattern.CASE_INSENSITIVE);
//        static final Pattern regexUDPTransport = Pattern.compile("client_port=(\\d+)-\\d+;server_port=(\\d+)-\\d+",Pattern.CASE_INSENSITIVE);
//        static final Pattern regexUDPTransport = Pattern.compile("server_port=(\\d+);client_port=(\\d+)-(\\d+)",Pattern.CASE_INSENSITIVE);
//        static final Pattern regexUDPTransport = Pattern.compile("client_port=(\\d+)-(\\d+);.*?server_port=(\\d+)-(\\d+);",Pattern.CASE_INSENSITIVE);
        static final Pattern regexClientPort = Pattern.compile("client_port=(\\d+)-\\d+",Pattern.CASE_INSENSITIVE);//TODO
        static final Pattern regexServerPort = Pattern.compile("server_port=(\\d+)",Pattern.CASE_INSENSITIVE);//TODO
//        static final Pattern regexTCPTransport = Pattern.compile("client_port=(\\d+)-\\d+;",Pattern.CASE_INSENSITIVE);
        static final Pattern regexSessionWithTimeout = Pattern.compile("(\\S+);timeout=(\\d+)",Pattern.CASE_INSENSITIVE);
        static final Pattern regexSDPgetTrack1 = Pattern.compile("trackID=(\\d+)",Pattern.CASE_INSENSITIVE);
        static final Pattern regexSDPgetTrack2 = Pattern.compile("control:(\\S+)",Pattern.CASE_INSENSITIVE);
        static final Pattern regexSDPmediadescript = Pattern.compile("m=(\\S+) .+",Pattern.CASE_INSENSITIVE);
        static final Pattern regexSDPpacketizationMode = Pattern.compile("packetization-mode=(\\d);",Pattern.CASE_INSENSITIVE);
        static final Pattern regexSDPspspps = Pattern.compile("sprop-parameter-sets=(\\S+),(\\S+)",Pattern.CASE_INSENSITIVE);
        static final Pattern regexSDPlength = Pattern.compile("Content-length: (\\d+)",Pattern.CASE_INSENSITIVE);
        static final Pattern regexSDPstartFlag = Pattern.compile("v=(\\d)",Pattern.CASE_INSENSITIVE);

        public int state;
        public HashMap<String,String> headers = new HashMap<>();

        public static Response parseResponse(BufferedReader input) throws IOException  {
            Response response = new Response();
            String line;
            Matcher matcher;
            int sdpContentLength = 0;
            if( (line = input.readLine()) == null) throw new IOException("Connection lost");
            matcher = regexStatus.matcher(line);
            if(matcher.find())
                response.state = Integer.parseInt(matcher.group(1));
            else
                while ( (line = input.readLine()) != null ) {
                    matcher = regexStatus.matcher(line);
                    if(matcher.find()) {
                        response.state = Integer.parseInt(matcher.group(1));
                        break;
                    }
                }
            Log.d(tag, "The line is: " + line + "...");
            Log.d(tag, "The response state is: "+response.state);

            int foundMediaType = 0;
            int sdpHaveReadLength = 0;
            boolean sdpStartFlag = false;

            while ( (line = input.readLine()) != null) {
                if( line.length() > 3 || Describeflag ) {
                    Log.d(tag, "The line is: " + line + "...");
                    matcher = regexHeader.matcher(line);
                    if (matcher.find()){
                        response.headers.put(matcher.group(1).toLowerCase(Locale.US), matcher.group(2)); //$ to $
                    }

                    matcher = regexSDPlength.matcher(line);
                    if(matcher.find()) {
                        sdpContentLength = Integer.parseInt(matcher.group(1));
                        sdpHaveReadLength = 0;
                    }
                    //Here is trying to get the SDP information from the describe response
                    if (Describeflag) {
                        matcher = regexSDPmediadescript.matcher(line);
                        if (matcher.find())
                            if (matcher.group(1).equalsIgnoreCase("audio")) {
                                foundMediaType = 1;
                                sdpInfo.audioTrackFlag = true;
                            } else if (matcher.group(1).equalsIgnoreCase("video")) {
                                foundMediaType = 2;
                                sdpInfo.videoTrackFlag = true;
                            }

                        matcher = regexSDPpacketizationMode.matcher(line);
                        if (matcher.find()) {
                            sdpInfo.packetizationMode = Integer.parseInt(matcher.group(1));
                        }

                        matcher = regexSDPspspps.matcher(line);
                        if(matcher.find()) {
                            sdpInfo.SPS = matcher.group(1);
                            sdpInfo.PPS = matcher.group(2);
                        }

                        matcher = regexSDPgetTrack1.matcher(line);
                        if(matcher.find())
                            if (foundMediaType == 1) sdpInfo.audioTrack = "trackID=" + matcher.group(1);
                            else if (foundMediaType == 2) sdpInfo.videoTrack = "trackID=" + matcher.group(1);


                        matcher = regexSDPgetTrack2.matcher(line);
                        if(matcher.find())
                            if (foundMediaType == 1) sdpInfo.audioTrack = matcher.group(1);
                            else if (foundMediaType == 2) sdpInfo.videoTrack = matcher.group(1);


                        matcher = regexSDPstartFlag.matcher(line);
                        if(matcher.find()) sdpStartFlag = true;
                        if(sdpStartFlag) sdpHaveReadLength += line.getBytes().length + 2;
                        if((sdpContentLength < sdpHaveReadLength + 2) && (sdpContentLength != 0)) {
                            Describeflag = false;
                            sdpStartFlag = false;
                            Log.d(tag, "The SDP info: "
                                    + (sdpInfo.audioTrackFlag ? "have audio info.. " : "haven't the audio info.. ")
                                    + ";" + (sdpInfo.audioTrackFlag ? (" the audio track is " + sdpInfo.audioTrack) : ""));
                            Log.d(tag, "The SDP info: "
                                    + (sdpInfo.videoTrackFlag ? "have video info.. " : "haven't the vedio info..")
                                    + (sdpInfo.videoTrackFlag ? (" the video track is " + sdpInfo.videoTrack) : "")
                                    + ";" + (sdpInfo.videoTrackFlag ? (" the video SPS is " + sdpInfo.SPS) : "")
                                    + ";" + (sdpInfo.videoTrackFlag ? (" the video PPS is " + sdpInfo.PPS) : "")
                                    + ";" + (sdpInfo.videoTrackFlag ? (" the video packetization mode is " + sdpInfo.packetizationMode) : ""));
                            break;
                        }
                    }
                } else {
                    break;
                }

            }

//            if( line == null ) throw new IOException("Connection lost");

            return  response;
        }
    }
}
