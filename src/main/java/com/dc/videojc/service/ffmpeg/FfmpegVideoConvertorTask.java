package com.dc.videojc.service.ffmpeg;

import cn.hutool.core.collection.CollUtil;
import com.dc.videojc.model.TaskContext;
import com.dc.videojc.service.AbstractVideoConvertorTask;
import com.dc.videojc.service.VideoConvertorTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/21
 */
@Slf4j
public class FfmpegVideoConvertorTask extends AbstractVideoConvertorTask implements VideoConvertorTask {
    private final String ffmpegPath;
    private boolean running = true;
    private final ThreadPoolTaskExecutor processMonitorTaskPool;
    private boolean traceLog = false;
    
    public FfmpegVideoConvertorTask(TaskContext taskContext, String ffmpegPath, ThreadPoolTaskExecutor processMonitorTaskPool) {
        super(taskContext);
        this.ffmpegPath = ffmpegPath;
        this.processMonitorTaskPool = processMonitorTaskPool;
    }
    
    private ServerSocket tcpServer = null;
    private Process process;
    private Socket client;
    private InputStream inputStream;
    private List<String> cmd;
    
    @Override
    public void run() {
        try {
            final byte[] buffer = new byte[102400];
            int len = 0;
            while (running && (len = inputStream.read(buffer)) > -1) {
                if (len == 0) {
                    continue;
                }
                if (len == buffer.length) {
                    sendFrameData(buffer);
                } else {
                    final byte[] data = new byte[len];
                    System.arraycopy(buffer, 0, data, 0, len);
                    sendFrameData(data);
                }
                
            }
        } catch (Exception e) {
              /*
                任务变为不可用 防止这时还在添加客户端
             */
            running = false;
            if (onAbort != null) {
                onAbort.run();
            }
            log.error("转换任务-异常!! [" + this.getTaskContext() + "}][" + this + "]", e);
        }
        closeAllClient();
        try {
            process.destroy();
        } catch (Exception e) {
            log.warn("转换任务-关闭FFMPEG--失败-强行关闭[" + this.getTaskContext() + "}][" + this + "]", e);
            process.destroyForcibly();
        }
        try {
            inputStream.close();
        } catch (Exception e) {
            log.warn("转换任务-关闭媒体流--失败[" + this.getTaskContext() + "}][" + this + "]", e);
        }
        try {
            client.close();
        } catch (Exception e) {
            log.warn("转换任务-关闭媒体流--失败[" + this.getTaskContext() + "}][" + this + "]", e);
        }
        log.info("转换任务-关闭媒体流[{}][{}]", this.getTaskContext(), this);
    }
    
    @Override
    public void shutdown() {
        running = false;
    }
    
    @Override
    public void init() {
        try {
            buildServerSocket();
            buildCommand();
        } catch (IOException e) {
            throw new IllegalStateException("转换任务-初始化失败,Tcp连接建立失败", e);
        }
        log.info(CollUtil.join(cmd, " "));
        try {
            process = new ProcessBuilder(cmd).start();
        } catch (Exception e) {
            throw new IllegalStateException("转换任务-初始化失败,转换进程启动失败", e);
        }
        try {
            monitorOutput();
            client = tcpServer.accept();
            inputStream = client.getInputStream();
            header = new byte[1024];
            int len = inputStream.read(header);
            if (len < header.length) {
                byte[] temp = header;
                header = new byte[len];
                System.arraycopy(temp, 0, header, 0, len);
            } else {
                throw new IllegalStateException("header 大于1k???");
            }
            running = true;
        } catch (Exception e) {
            process.destroy();
            running = false;
            throw new IllegalStateException("转换任务-初始化失败,视频网络桥接初始化失败", e);
        }
        log.info("转换任务-启动成功[{}][{}]", this.getTaskContext(), this);
    }
    
    private void monitorOutput() {
        processMonitorTaskPool.execute(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while (null != (line = reader.readLine())) {
                    if (!traceLog) {
                        log.error("[{}{}]:{}", this.getTaskContext(), this, line);
                    } else {
                        if (line.contains("fail") | line.contains("err")) {
                            log.error("[{}{}]:{}", this.getTaskContext(), this, line);
                        } else {
                            log.trace("[{}{}]:{}", this.getTaskContext(), this, line);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Monitor Process failed[" + this.getTaskContext() + "][" + this + "]", e);
            }
        });
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    private void buildServerSocket() throws IOException {
        tcpServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        tcpServer.setSoTimeout(10000);
    }
    
    private void buildCommand() {
        final String outputUrl = "tcp://" + tcpServer.getInetAddress().getHostAddress() + ":" + tcpServer.getLocalPort();
        cmd = new ArrayList<>();
        cmd.add(ffmpegPath);
        if ("rtsp".equals(taskContext.getSourceProtocol())) {
            cmd.add("-rtsp_transport");
            cmd.add("tcp");
        }
        cmd.add("-i");
        cmd.add(taskContext.getVideoInfo().getSource());
        cmd.add("-max_delay");
        cmd.add("100");
        cmd.add("-g");
        cmd.add("25");
        cmd.add("-r");
        cmd.add("25");
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-preset:v");
        cmd.add("fast");
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-f");
        if (!traceLog) {
            cmd.add("-loglevel");
            cmd.add("error");
        }
        cmd.add(taskContext.getVideoInfo().getTargetFormat());
        cmd.add(outputUrl);
    }
    
    public FfmpegVideoConvertorTask setTraceLog(boolean traceLog) {
        this.traceLog = traceLog;
        return this;
    }
}
