package com.google.android.exoplayer2.ext.rtsp.Stream;

import android.os.HandlerThread;
import android.os.Handler;
import android.util.Log;


import java.util.concurrent.LinkedBlockingDeque;

/**
 *This class is used to analysis the data from rtp socket , recombine it to video or audio stream
 * 1. get the data from rtp socket
 * 2. put the data into buffer
 * 3. use the thread to get the data from buffer, and unpack it
 */
public abstract class RtpStream {

    private final static String tag = "RtspStream";
    protected final static int TRACK_VIDEO = 0x01;
    protected final static int TRACK_AUDIO = 0x02;

    private Handler mHandler;
    private byte[] buffer;
    private HandlerThread thread;
    private boolean isStoped;

    protected class StreamPacks {
        public boolean mark;
        public int pt;
        public long timestamp;
        public int sequenceNumber;
        public long Ssrc;
        public byte[] data;
    }

    private static class bufferUnit {
        public byte[] data;
        public int len;
    }

    private LinkedBlockingDeque<bufferUnit> bufferQueue = new LinkedBlockingDeque<>();

    public RtpStream() {
        thread = new HandlerThread("RTPStreamThread");
        thread.start();
        mHandler = new Handler(thread.getLooper());
        unpackThread();
        isStoped = false;
    }

    public void receiveData(byte[] data, int len) {
        bufferUnit tmpBuffer = new bufferUnit();
        tmpBuffer.data = new byte[len];
        System.arraycopy(data,0,tmpBuffer.data,0,len);
        tmpBuffer.len = len;
        try {
            bufferQueue.put(tmpBuffer);
        } catch (InterruptedException e) {
        }
    }

    private void unpackThread() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                bufferUnit tmpBuffer;
                while (!isStoped) {
                    try {
                        tmpBuffer = bufferQueue.take();
                        buffer = new byte[tmpBuffer.len];
                        System.arraycopy(tmpBuffer.data,0,buffer,0,tmpBuffer.len);
                        unpackData();
                    } catch (InterruptedException e) {
                        Log.e(tag,"wait the new data into the queue..");
                    }
                }
                buffer = null;
                bufferQueue.clear();
            }
        });
    }

    public void stop(){
        isStoped = true;
//        try {
//            Thread.sleep(100);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        bufferQueue.clear();
//        buffer = null;
        thread.quit();
    }


    protected abstract void recombinePacket(StreamPacks sp);

    private void unpackData() {
        StreamPacks tmpStreampack = new StreamPacks();
        if(buffer.length == 0) return;
        int rtpVersion = (buffer[0]&0xFF)>>6;
        if(rtpVersion != 2) {
            Log.e(tag,"This is not a rtp packet.");
            return;
        }

        //标记位（M）：1比特,该位的解释由配置文档（Profile）来承担.
        tmpStreampack.mark = (buffer[1] & 0xFF & 0x80) >> 7 == 1;
        //载荷类型（PT）：7比特，标识了RTP载荷的类型。
        tmpStreampack.pt = buffer[1] & 0x7F;
        //序列号（SN）：16比特，发送方在每发送完一个RTP包后就将该域的值增加1，
        // 接收方可以由该域检测包的丢失及恢复包序列。序列号的初始值是随机的。
        tmpStreampack.sequenceNumber = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
        //时间戳：32比特，记录了该包中数据的第一个字节的采样时刻。在一次会话开始时，
        // 时间戳初始化成一个初始值。即使在没有信号发送时，时间戳的数值也要随时间而不断地增加（时间在流逝嘛）。
        // 时间戳是去除抖动和实现同步不可缺少的。
        tmpStreampack.timestamp = Long.parseLong(Integer.toHexString(((buffer[4] & 0xFF) << 24) | ((buffer[5]&0xFF) << 16) | ((buffer[6]&0xFF) << 8) | (buffer[7]&0xFF)) , 16);
        //同步源标识符(SSRC)：32比特，同步源就是指RTP包流的来源。在同一个RTP会话中不能有两个相同的SSRC值。
        // 该标识符是随机选取的 RFC1889推荐了MD5随机算法。
        tmpStreampack.Ssrc = ((buffer[8]&0xFF) << 24) | ((buffer[9]&0xFF) << 16) | ((buffer[10]&0xFF) << 8) | (buffer[11]&0xFF);
        //这里为csrc数据
        //贡献源列表（CSRC List）：0～15项，每项32比特，用来标志对一个RTP混合器产生的新包有贡献的所有RTP包的源。
        // 由混合器将这些有贡献的SSRC标识符插入表中。SSRC标识符都被列出来，以便接收端能正确指出交谈双方的身份。
        tmpStreampack.data = new byte[buffer.length-12];

        System.arraycopy(buffer, 12, tmpStreampack.data, 0, buffer.length - 12);
        recombinePacket(tmpStreampack);
    }

}
