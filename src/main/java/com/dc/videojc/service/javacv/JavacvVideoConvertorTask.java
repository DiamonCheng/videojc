package com.dc.videojc.service.javacv;

import com.dc.videojc.model.ClientInfo;
import com.dc.videojc.model.TaskContext;
import com.dc.videojc.service.AbstractVideoConvertorTask;
import com.dc.videojc.service.VideoConvertorTask;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
@Slf4j
public class JavacvVideoConvertorTask extends AbstractVideoConvertorTask implements VideoConvertorTask {
    /**
     * flv header
     */
    private byte[] header = null;
    /**
     * 输出流，视频最终会输出到此
     */
    private final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    /**
     * 拉流器
     */
    private FFmpegFrameGrabber grabber;
    /**
     * 推流录制器
     */
    private FFmpegFrameRecorder recorder;
    
    /**
     * 标记1 可以循环处理
     * 标记2 可以接收客户端
     */
    private volatile boolean isRunning = true;
    /**
     * true:转复用,false:转码
     * 默认转码
     */
    boolean notTransformFlag = false;
    /**
     * 时间戳计算
     */
    long startTime = 0;
    long videoTs;
    
    public JavacvVideoConvertorTask(TaskContext taskContext) {
        super(taskContext);
    }
    
    @Override
    public void init() {
        initGrabber();
        supportFlvFormatCodec();
        initRecorder();
        try {
            grabber.flush();
        } catch (FrameGrabber.Exception e) {
            throw new IllegalStateException("清空拉流器缓存失败", e);
        }
        if (header == null) {
            header = bos.toByteArray();
            bos.reset();
        }
    }
    
    @Override
    public boolean isRunning() {
        return isRunning;
    }
    
    @Override
    public void run() {
        try {
            while (isRunning) {
                processFramesLoop();
            }
        } catch (Exception e) {
            /*
                任务变为不可用 防止这时还在添加客户端
             */
            isRunning = false;
            if (onAbort != null) {
                onAbort.run();
            }
            log.error("转换任务-异常!! [" + this.getTaskContext() + "}][" + this + "]", e);
        }
        closeAllClient();
        try {
            recorder.close();
        } catch (FrameRecorder.Exception e) {
            log.warn("转换任务-关闭媒体流--失败[" + this.getTaskContext() + "}][" + this + "]", e);
        }
        try {
            grabber.close();
        } catch (FrameGrabber.Exception e) {
            log.warn("转换任务-关闭媒体流--失败[" + this.getTaskContext() + "}][" + this + "]", e);
        }
        try {
            bos.close();
        } catch (IOException e) {
            log.warn("转换任务-关闭媒体流--失败[" + this.getTaskContext() + "}][" + this + "]", e);
        }
        log.info("转换任务-关闭媒体流[{}][{}]", this.getTaskContext(), this);
    }
    
    protected void processFramesLoop() throws FrameGrabber.Exception, FrameRecorder.Exception {
        long currentTimestamp = System.currentTimeMillis();
        boolean isNotNull;
        if (notTransformFlag) {
            //转复用
            isNotNull = processTransfer(currentTimestamp);
        } else {
            //转码
            isNotNull = processTransform(currentTimestamp);
        }
        if (!isNotNull) {
            isRunning = false;
            log.warn("转换任务-没有有效帧-结束任务[{}][{}]", this.getTaskContext(), this);
        } else if (bos.size() > 0) {
            byte[] b = bos.toByteArray();
            bos.reset();
            // 发送视频到前端
            sendFrameData(b);
        }
    }
    
    protected boolean processTransform(long currentTimestamp) throws FrameGrabber.Exception, FrameRecorder.Exception {
        Frame frame = grabber.grabFrame();
        if (frame != null) {
            if (startTime == 0) {
                startTime = currentTimestamp;
            }
            videoTs = 1000 * (currentTimestamp - startTime);
            // 判断时间偏移
            if (videoTs > recorder.getTimestamp()) {
                recorder.setTimestamp((videoTs));
            }
            recorder.record(frame);
            log.trace("转换任务-转码帧[{}][{}][{}]", this.getTaskContext(), this, videoTs);
            return true;
        } else {
            return false;
        }
    }
    
    protected boolean processTransfer(long currentTimestamp) throws FrameGrabber.Exception, FrameRecorder.Exception {
        AVPacket pkt = grabber.grabPacket();
        if (null != pkt && !pkt.isNull()) {
            if (startTime == 0) {
                startTime = currentTimestamp;
            }
            videoTs = 1000 * (currentTimestamp - startTime);
            // 判断时间偏移
            if (videoTs > recorder.getTimestamp()) {
                recorder.setTimestamp((videoTs));
            }
            recorder.recordPacket(pkt);
            log.trace("转换任务-转复用[{}][{}][{}]", this.getTaskContext(), this, videoTs);
            return true;
        } else {
            return false;
        }
    }
    
    private void sendFrameData(byte[] data) {
        for (Iterator<ClientInfo> iterator = taskContext.getClientList().iterator(); iterator.hasNext(); ) {
            ClientInfo client = iterator.next();
            try {
                if (!client.isHeaderSent()) {
                    client.getDataSender().send(header);
                    client.setHeaderSent(true);
                }
                client.getDataSender().send(data);
            } catch (Exception e) {
                iterator.remove();
                log.warn("转换任务-发送客户端数据失败,应该是客户端已经关闭[{}][{}],clientInfo:[{}]", this.getTaskContext(), this, client);
                log.warn("", e);
            }
        }
        if (taskContext.getClientList().isEmpty() && taskContext.getLastNoClientTime() == null) {
            taskContext.setLastNoClientTime(System.currentTimeMillis());
        } else if (!taskContext.getClientList().isEmpty()) {
            taskContext.setLastNoClientTime(null);
        }
    
    }
    
    @Override
    public void shutdown() {
        isRunning = false;
    }
    
    protected void supportFlvFormatCodec() {
        int vcodec = grabber.getVideoCodec();
        int acodec = grabber.getAudioCodec();
        notTransformFlag = (!"file".equals(taskContext.getSourceProtocol()))
                                   && ("desktop".equals(taskContext.getSourceProtocol()) || avcodec.AV_CODEC_ID_H264 == vcodec || avcodec.AV_CODEC_ID_H263 == vcodec)
                                   && (avcodec.AV_CODEC_ID_AAC == acodec || avcodec.AV_CODEC_ID_AAC_LATM == acodec);
    }
    
    protected void initGrabber() {
        /*
         * stimeout //超时时间(15秒)
         * threads 1
         * buffer_size 1024000 //设置缓存大小，提高画质、减少卡顿花屏
         * rw_timeout //读写超时，适用于所有协议的通用读写超时
         * probesize 5000000 //探测视频流信息，为空默认5000000微秒
         * analyzeduration 5000000 //解析视频流信息，为空默认5000000微秒
         */
        // 拉流器
        grabber = new FFmpegFrameGrabber(taskContext.getVideoInfo().getSource());
        grabber.setOption("threads", "1");
        grabber.setOption("buffer_size", "1024000");
        
        // 如果为rtsp流，增加配置
        if ("rtsp".equals(taskContext.getSourceProtocol())) {
            // 设置打开协议tcp / udp
            grabber.setOption("rtsp_transport", "tcp");
            //首选TCP进行RTP传输
            grabber.setOption("rtsp_flags", "prefer_tcp");
            
        } else if ("rtmp".equals(taskContext.getSourceProtocol())) {
            // rtmp拉流缓冲区，默认3000毫秒
            grabber.setOption("rtmp_buffer", "1000");
            // 默认rtmp流为直播模式，不允许seek
            //grabber.setOption("rtmp_live", "live");
            
        } else if ("desktop".equals(taskContext.getSourceProtocol())) {
            //支持本地屏幕采集，可以用于监控屏幕、局域网和wifi投屏等
            grabber.setFormat("gdigrab");
            //绘制鼠标
            grabber.setOption("draw_mouse", "1");
            grabber.setNumBuffers(0);
            grabber.setOption("fflags", "nobuffer");
            grabber.setOption("framerate", "25");
            grabber.setFrameRate(25);
        }
        
        try {
            grabber.start();
            log.info("转换任务-启动拉流器成功[{}][{}]", this.getTaskContext(), this);
        } catch (FrameGrabber.Exception e) {
            throw new IllegalStateException("启动拉流器失败，网络超时或视频源不可用", e);
        }
    }
    
    protected void initRecorder() {
        recorder = new FFmpegFrameRecorder(bos, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
        recorder.setFormat(taskContext.getVideoInfo().getTargetFormat());
        if (!notTransformFlag) {
            //转码
            recorder.setInterleaved(false);
            recorder.setVideoOption("tune", "zerolatency");
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setVideoOption("crf", "26");
            recorder.setVideoOption("threads", "1");
            // 设置帧率
            recorder.setFrameRate(25);
            // 设置gop,与帧率相同，相当于间隔1秒chan's一个关键帧
            recorder.setGopSize(25);
            recorder.setVideoCodecName("libx264");
            recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
            recorder.setAudioCodecName("aac");
            /*
             * 启用RDOQ算法，优化视频质量 1：在视频码率和视频质量之间取得平衡 2：最大程度优化视频质量（会降低编码速度和提高码率）
             */
            recorder.setTrellis(1);
            //设置延迟
            recorder.setMaxDelay(0);
            try {
                recorder.start();
            } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                throw new IllegalStateException("启动转码录制器失败", e);
            }
        } else {
            // 转复用
            //不让recorder关联关闭outputStream
            recorder.setCloseOutputStream(false);
            try {
                recorder.start(grabber.getFormatContext());
            } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                log.error("启动转复用录制器失败...尝试启用转码", e);
                // 如果转复用失败，则自动切换到转码模式
                notTransformFlag = false;
                if (recorder != null) {
                    try {
                        recorder.stop();
                    } catch (org.bytedeco.javacv.FrameRecorder.Exception e1) {
                        log.error("启动转复用录制器失败...尝试启用转码...guanbishibai ", e1);
                    }
                }
                initRecorder();
            }
        }
    }
    
    @Override
    public void addClient(ClientInfo clientInfo) {
        try {
            if (!clientInfo.isHeaderSent()) {
                clientInfo.getDataSender().send(header);
                clientInfo.setHeaderSent(true);
            }
            super.addClient(clientInfo);
            log.info("转换任务-添加客户端成功[{}][{}][{}]", this.getTaskContext(), this, clientInfo);
        } catch (Exception e) {
            throw new IllegalStateException("发送视频流头部信息失败", e);
        }
    }
}
