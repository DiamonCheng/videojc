package com.dc.videojc.service.ffmpeg;

import com.dc.videojc.model.ConvertContext;
import com.dc.videojc.service.VideoConvertor;
import com.dc.videojc.service.VideoConvertorTask;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.Loader;
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
public class FfmpegVideoConvertor implements VideoConvertor {
    
    @Value("${vediojc.javacv.protocols.support:rtsp,rtmp,http}")
    private Set<String> supportProtocols;
    @Value("${vediojc.javacv.target-formats.support:flv}")
    private Set<String> supportTargetFormats;
    
    private String ffmpegPath;
    
    @PostConstruct
    public void init() {
        log.info("[ffmpeg]正在初始化资源，请稍等...");
        ffmpegPath = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
    }
    
    @Override
    public boolean isSupport(ConvertContext convertContext) {
        return supportProtocols.contains(convertContext.getSourceProtocol())
                       && supportTargetFormats.contains(convertContext.getVideoInfo().getTargetFormat())
                       && convertContext.getVideoInfo().getFfmpeg()
                ;
    }
    
    @Override
    public Integer priority() {
        return 100;
    }
    
    @Override
    public VideoConvertorTask doConvert(ConvertContext convertContext) {
        return null;
    }
}
