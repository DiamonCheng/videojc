package com.dc.videojc.service;

import cn.hutool.crypto.digest.MD5;
import com.dc.videojc.config.SpringContextUtil;
import com.dc.videojc.model.ClientInfo;
import com.dc.videojc.model.ConvertContext;
import com.dc.videojc.model.VideoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
@Service
@Slf4j
public class ConvertService {
    
    @Value("${vediojc.protocols.support}")
    private Set<String> supportProtocols;
    @Value("${vediojc.target-formats.support:flv}")
    private Set<String> supportTargetFormats;
    @Value("${vediojc.target-format.default:flv}")
    private String defaultTargetFormat;
    @Value("${vediojc.task.close.wait:60000}")
    private Long taskCloseWait;
    
    private List<VideoConvertor> videoConvertors;
    
    private final Map<String, VideoConvertorTask> videoConvertorTaskMap = Collections.synchronizedMap(new LinkedHashMap<>());
    
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    
    @PostConstruct
    public void init() {
        String[] beanNames = SpringContextUtil.getApplicationContext().getBeanNamesForType(VideoConvertor.class);
        videoConvertors = Arrays.stream(beanNames)
                                  .map(beanName -> SpringContextUtil.getApplicationContext().getBean(beanName, VideoConvertor.class))
                                  .sorted(Comparator.comparing(VideoConvertor::priority).reversed())
                                  .collect(Collectors.toList());
    }
    
    @Scheduled(fixedRate = 1000)
    public void monitorTasks() {
        long currentTimestamp = System.currentTimeMillis();
        videoConvertorTaskMap.entrySet().removeIf(e -> {
            VideoConvertorTask task = e.getValue();
            if (task.getTaskContext().isNotAutoClose()) {
                return false;
            }
            if (task.getTaskContext().getLastNoClientTime() == null) {
                return false;
            }
            if (currentTimestamp - task.getTaskContext().getLastNoClientTime() > taskCloseWait) {
                log.info("转换任务-因一段时间没有客户端连接而结束[{}][{}]", task.getTaskContext(), task);
                task.shutdown();
                return true;
            } else {
                return false;
            }
        });
    }
    
    public void doConvert(ClientInfo clientInfo, VideoInfo videoInfo) {
        ConvertContext convertContext = buildContext(clientInfo, videoInfo);
        VideoConvertorTask convertTask;
        boolean newTask;
        convertTask = videoConvertorTaskMap.get(convertContext.getTaskId());
        newTask = false;
        if (convertTask == null || !convertTask.isRunning()) {
            VideoConvertor videoConvertor = videoConvertors.stream()
                                                    .filter(e -> e.isSupport(convertContext))
                                                    .findFirst()
                                                    .orElseThrow(() -> new IllegalStateException("当前转换不支持 " + videoInfo));
            convertTask = videoConvertor.prepareConvert(convertContext);
            final VideoConvertorTask finalConvertTask = convertTask;
            convertTask.setOnAbort(() -> {
                //防止移除掉重新生成的新任务
                videoConvertorTaskMap.remove(finalConvertTask.getTaskContext().getId(), finalConvertTask);
                log.info("转换任务-(可能是异常)结束![{}][{}]", finalConvertTask.getTaskContext(), finalConvertTask);
            });
            videoConvertorTaskMap.put(convertContext.getTaskId(), convertTask);
            log.info("转换任务-启动![{}]启动![{}]", convertTask.getTaskContext(), convertTask);
            newTask = true;
        }
        //保证初始化之后 addClient
        if (newTask) {
            try {
                convertTask.init();
                threadPoolTaskExecutor.execute(convertTask);
            } catch (RuntimeException e) {
                videoConvertorTaskMap.remove(convertContext.getTaskId(), convertTask);
                throw e;
            }
        }
        convertTask.addClient(clientInfo);
    }
    
    private ConvertContext buildContext(ClientInfo clientInfo, VideoInfo videoInfo) {
        Assert.hasText(videoInfo.getSource(), "视频源信息 source 不能为空");
        String protocol = videoInfo.getSource().substring(0, videoInfo.getSource().indexOf("://"));
        Assert.isTrue(supportProtocols.contains(protocol), "不支持的协议 " + protocol + ", 支持:" + supportProtocols);
        if (videoInfo.getFfmpeg() == null) {
            videoInfo.setFfmpeg(false);
        }
        if (videoInfo.getTargetFormat() == null) {
            videoInfo.setTargetFormat(defaultTargetFormat);
        }
        Assert.isTrue(supportTargetFormats.contains(videoInfo.getTargetFormat()), "不支持的目标格式: " + videoInfo.getTargetFormat() + ", 支持 " + supportTargetFormats);
        ConvertContext convertContext = new ConvertContext();
        convertContext.setTaskId(MD5.create().digestHex(videoInfo.toString(), StandardCharsets.UTF_8));
        convertContext.setVideoInfo(videoInfo);
        convertContext.setSourceProtocol(protocol);
        convertContext.setClientInfo(clientInfo);
        return convertContext;
    }
    
}
