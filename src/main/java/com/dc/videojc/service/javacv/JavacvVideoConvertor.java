package com.dc.videojc.service.javacv;

import com.dc.videojc.model.ConvertContext;
import com.dc.videojc.model.TaskContext;
import com.dc.videojc.service.VideoConvertor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Set;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
@Service
@Slf4j
public class JavacvVideoConvertor implements VideoConvertor {
    static {
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        FFmpegLogCallback.set();
    }
    
    @Value("${vediojc.javacv.protocols.support:rtsp,rtmp,http}")
    private Set<String> supportProtocols;
    @Value("${vediojc.javacv.target-formats.support:flv}")
    private Set<String> supportTargetFormats;
    
    @PostConstruct
    public void init() throws Exception {
        log.info("[Javacv]正在初始化资源，请稍等...");
        FFmpegFrameGrabber.tryLoad();
        FFmpegFrameRecorder.tryLoad();
    }
    
    @Override
    public boolean isSupport(ConvertContext convertContext) {
        return supportProtocols.contains(convertContext.getSourceProtocol())
                       && supportTargetFormats.contains(convertContext.getVideoInfo().getTargetFormat())
                       && !convertContext.getVideoInfo().getFfmpeg()
                ;
    }
    
    @Override
    public Integer priority() {
        return 100;
    }
    
    @Override
    public JavacvVideoConvertorTask prepareConvert(ConvertContext convertContext) {
        JavacvVideoConvertorTask javacvVideoConvertorTask;
        TaskContext taskContext = new TaskContext();
        taskContext.setId(convertContext.getTaskId());
        taskContext.setNotAutoClose(Boolean.FALSE.equals(convertContext.getAutoClose()));
        taskContext.setVideoInfo(convertContext.getVideoInfo());
        taskContext.setSourceProtocol(convertContext.getSourceProtocol());
        javacvVideoConvertorTask = new JavacvVideoConvertorTask(taskContext);
        return javacvVideoConvertorTask;
    }
}
