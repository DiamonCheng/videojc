package com.dc.videojc.service.rtmpred5;

import com.dc.red5slim.server.stream.AbstractFlvPublishListener;
import com.dc.red5slim.server.stream.FlvPublishListener;
import com.dc.videojc.model.TaskContext;
import com.dc.videojc.service.AbstractVideoConvertorTask;
import com.dc.videojc.service.VideoConvertorTask;
import lombok.extern.slf4j.Slf4j;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.VideoData;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/9/15
 */
@Slf4j
public class RtmpRed5VideoConvertorTask extends AbstractVideoConvertorTask implements VideoConvertorTask, FlvPublishListener {
    private volatile boolean running;
    private final VideoConvertorFlvPublishListener videoConvertorFlvPublishListener;
    private final AtomicReference<byte[]> bodyBytes = new AtomicReference<>();
    private boolean useDataQueue = false;
    private BlockingQueue<byte[]> tagQueue;
    
    public RtmpRed5VideoConvertorTask(TaskContext taskContext) {
        super(taskContext);
        videoConvertorFlvPublishListener = new VideoConvertorFlvPublishListener();
    }
    
    @Override
    public void stop() {
        if (onAbort != null) {
            onAbort.run();
        }
        running = false;
    }
    
    @Override
    public void setAudioDecoderConfiguration(AudioData audioConfig) {
        videoConvertorFlvPublishListener.setAudioDecoderConfiguration(audioConfig);
    }
    
    @Override
    public void setVideoDecoderConfiguration(VideoData videoConfig) {
        videoConvertorFlvPublishListener.setVideoDecoderConfiguration(videoConfig);
    }
    
    @Override
    public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
        videoConvertorFlvPublishListener.packetReceived(stream, packet);
    }
    
    private void onHeaderReceived(byte[] header) {
        this.header = header;
    }
    
    private void onDataReceived(byte[] tag) {
        if (useDataQueue) {
            if (!tagQueue.offer(tag)) {
                log.warn("丢包!--{}", taskContext);
            }
        } else {
            byte[] exists = bodyBytes.getAndSet(tag);
            if (exists != null) {
                log.warn("丢包!--{}", taskContext);
            }
        }
    }
    
    class VideoConvertorFlvPublishListener extends AbstractFlvPublishListener {
        
        @Override
        protected void onHeaderReceived(ByteBuffer byteBuffer) {
            if (byteBuffer.limit() == byteBuffer.array().length) {
                RtmpRed5VideoConvertorTask.this.onHeaderReceived(byteBuffer.array());
            } else {
                byte[] header = new byte[byteBuffer.limit()];
                byteBuffer.get(header);
                RtmpRed5VideoConvertorTask.this.onHeaderReceived(header);
            }
            onDataReceived(byteBuffer);
        }
        
        @Override
        protected void onDataReceived(ByteBuffer byteBuffer) {
            if (byteBuffer.limit() == byteBuffer.array().length) {
                RtmpRed5VideoConvertorTask.this.onDataReceived(byteBuffer.array());
            } else {
                byte[] tag = new byte[byteBuffer.limit()];
                byteBuffer.get(tag);
                RtmpRed5VideoConvertorTask.this.onDataReceived(tag);
            }
            
        }
        
        @Override
        protected void onStop() {
        }
    }
    
    @Override
    public void run() {
        if (useDataQueue) {
            List<byte[]> dataList = new LinkedList<>();
            while (running) {
                if (header != null) {
                    tagQueue.drainTo(dataList);
                    for (byte[] data : dataList) {
                        sendFrameData(data);
                    }
                    dataList.clear();
                }
            }
        } else {
            while (running) {
                if (header != null) {
                    byte[] data = bodyBytes.getAndSet(null);
                    if (data != null) {
                        sendFrameData(data);
                    }
                }
            }
        }
        
        closeAllClient();
    }
    
    @Override
    public void shutdown() {
        running = false;
    }
    
    @Override
    public void init() {
        if (useDataQueue) {
            tagQueue = new LinkedBlockingQueue<>(16);
        }
        running = true;
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    public void setUseDataQueue(boolean useDataQueue) {
        this.useDataQueue = useDataQueue;
    }
}
