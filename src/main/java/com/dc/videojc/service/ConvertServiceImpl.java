package com.dc.videojc.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.dc.videojc.config.SpringContextUtil;
import com.dc.videojc.model.ClientInfo;
import com.dc.videojc.model.ConvertContext;
import com.dc.videojc.model.SVideoInfo;
import com.dc.videojc.model.TaskInfoVO;
import com.dc.videojc.model.VideoInfo;
import com.dc.videojc.repository.StableVideoTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
public class ConvertServiceImpl implements ConvertService {
    
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
    
    @Autowired
    private StableVideoTaskRepository stableVideoTaskRepository;
    
    @PostConstruct
    public void init() {
        String[] beanNames = SpringContextUtil.getApplicationContext().getBeanNamesForType(VideoConvertor.class);
        videoConvertors = Arrays.stream(beanNames)
                                  .map(beanName -> SpringContextUtil.getApplicationContext().getBean(beanName, VideoConvertor.class))
                                  .sorted(Comparator.comparing(VideoConvertor::priority).reversed())
                                  .collect(Collectors.toList());
        final List<VideoInfo> stableTasks = stableVideoTaskRepository.list();
        if (!stableTasks.isEmpty()) {
            log.info("????????????{}?????????????????????", stableTasks.size());
            stableTasks.forEach(this::addStableTask0);
            stableVideoTaskRepository.flush();
            log.info("????????????{}?????????????????????", stableTasks.size());
        }
    }
    
    @PreDestroy
    public void destroy() {
        log.info("????????????????????????");
        videoConvertorTaskMap.values().forEach(VideoConvertorTask::shutdown);
    }
    
    @Scheduled(fixedRateString = "${vediojc.task.restart.duration:1000}")
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
                log.info("????????????-?????????????????????????????????????????????[{}][{}]", task.getTaskContext(), task);
                task.shutdown();
                return true;
            } else {
                return false;
            }
        });
    }
    
    @Scheduled(fixedRate = 30000)
    public void monitorNotAutoCloseTasks() {
        videoConvertorTaskMap.values()
                .stream()
                .filter(e -> e.getTaskContext().isNotAutoClose() && !e.isRunning())
                .forEach(this::restartTask);
    }
    
    private void restartTask(VideoConvertorTask task) {
        synchronized (task.getTaskContext()) {
            if (task.isRunning()) {
                log.info("???????????????????????? ???????????????[{}][{}]", task.getTaskContext(), task);
                return;
            }
            log.info("????????????-????????????[{}][{}]", task.getTaskContext(), task);
            task.init();
            threadPoolTaskExecutor.execute(task);
        }
    }
    
    @Override
    public void mergeClient(String taskId, ClientInfo clientInfo) {
        VideoConvertorTask convertTask = videoConvertorTaskMap.get(taskId);
        if (convertTask != null) {
            convertTask.addClient(clientInfo);
        } else {
            throw new IllegalStateException("????????????????????????!");
        }
    }
    
    @Override
    public void addStableTask(VideoInfo videoInfo) {
        final String taskId = addStableTask0(videoInfo);
        stableVideoTaskRepository.insert(new SVideoInfo().fromVideoInfo(videoInfo, taskId));
    }
    
    @Override
    public List<TaskInfoVO> listTasks() {
        return videoConvertorTaskMap.values().stream().map(e -> new TaskInfoVO(e.getTaskContext())).collect(Collectors.toList());
    }
    
    @Override
    public void deleteStableTask(String taskId) {
        VideoConvertorTask convertTask = videoConvertorTaskMap.get(taskId);
        if (convertTask == null) {
            throw new IllegalStateException("????????????????????? " + taskId);
        } else {
            convertTask.getTaskContext().setNotAutoClose(false);
        }
        
        stableVideoTaskRepository.delete(taskId);
    }
    
    @Override
    public void restartTask(String taskId) {
        VideoConvertorTask convertTask = videoConvertorTaskMap.get(taskId);
        if (convertTask == null) {
            throw new IllegalStateException("????????????????????? " + taskId);
        } else {
            restartTask(convertTask);
        }
    }
    
    private synchronized String addStableTask0(VideoInfo videoInfo) {
        final String taskId = genTaskId(videoInfo);
        if (videoInfo instanceof SVideoInfo) {
            ((SVideoInfo) videoInfo).setId(taskId);
        }
        VideoConvertorTask convertTask = videoConvertorTaskMap.get(taskId);
        if (convertTask == null) {
            doConvert(null, videoInfo);
        } else {
            if (convertTask.isRunning()) {
                if (convertTask.getTaskContext().isNotAutoClose()) {
                    throw new IllegalStateException("???????????????????????? " + convertTask.getTaskContext());
                } else {
                    convertTask.getTaskContext().setNotAutoClose(true);
                }
            } else {
                doConvert(null, videoInfo);
            }
        }
        return taskId;
    }
    
    /**
     * ????????????????????????
     * ?????????1?????????/??????
     * ??????????????????????????????????????????,??????????????????????????????
     *
     * @param clientInfo null if start a stable task
     * @param videoInfo  videoInfo
     */
    @Override
    public void doConvert(ClientInfo clientInfo, VideoInfo videoInfo) {
        final ConvertContext convertContext = buildContext(clientInfo, videoInfo);
        VideoConvertorTask convertTask;
        boolean newTask = false;
        synchronized (videoConvertorTaskMap) {
            convertTask = videoConvertorTaskMap.get(convertContext.getTaskId());
            final boolean unusableTask = convertTask == null || !convertTask.isRunning();
            final boolean unstableTask = convertTask == null || !convertTask.getTaskContext().isNotAutoClose();
            // ??????????????????????????????????????????????????????
            if (unusableTask && unstableTask) {
                VideoConvertor videoConvertor = videoConvertors.stream()
                                                        .filter(e -> e.isSupport(convertContext))
                                                        .findFirst()
                                                        .orElseThrow(() -> new IllegalStateException("????????????????????? " + videoInfo));
                convertTask = videoConvertor.prepareConvert(convertContext);
                final VideoConvertorTask finalConvertTask = convertTask;
                convertTask.setOnAbort(() -> {
                    if (convertContext.getAutoClose()) {
                        //???????????????????????????????????????
                        videoConvertorTaskMap.remove(convertContext.getTaskId(), finalConvertTask);
                    }
                    log.info("????????????-(???????????????)??????![{}][{}]", finalConvertTask.getTaskContext(), finalConvertTask);
                });
                videoConvertorTaskMap.put(convertContext.getTaskId(), convertTask);
                log.info("????????????-?????????...[{}][{}]", convertTask.getTaskContext(), convertTask);
                newTask = true;
            }
        }
        //????????????????????? addClient
        if (newTask) {
            try {
                convertTask.init();
                threadPoolTaskExecutor.execute(convertTask);
            } catch (RuntimeException e) {
                videoConvertorTaskMap.remove(convertContext.getTaskId(), convertTask);
                throw e;
            }
        }
        if (clientInfo != null) {
            convertTask.addClient(clientInfo);
        }
    }
    
    private ConvertContext buildContext(ClientInfo clientInfo, VideoInfo videoInfo) {
        Assert.hasText(videoInfo.getSource(), "??????????????? source ????????????");
        String protocol = videoInfo.getSource().substring(0, videoInfo.getSource().indexOf("://"));
        Assert.isTrue(supportProtocols.contains(protocol), "?????????????????? " + protocol + ", ??????:" + supportProtocols);
        if (videoInfo.getFfmpeg() == null) {
            videoInfo.setFfmpeg(false);
        }
        if (videoInfo.getTargetFormat() == null) {
            videoInfo.setTargetFormat(defaultTargetFormat);
        }
        Assert.isTrue(supportTargetFormats.contains(videoInfo.getTargetFormat()), "????????????????????????: " + videoInfo.getTargetFormat() + ", ?????? " + supportTargetFormats);
        ConvertContext convertContext = new ConvertContext();
        convertContext.setTaskId(genTaskId(videoInfo));
        convertContext.setVideoInfo(videoInfo);
        convertContext.setSourceProtocol(protocol);
        if (clientInfo == null) {
            convertContext.setAutoClose(false);
        } else {
            convertContext.setClientInfo(clientInfo);
        }
        return convertContext;
    }
    
    private String genTaskId(VideoInfo videoInfo) {
        return DigestUtil.md5Hex(String.valueOf(videoInfo), StandardCharsets.UTF_8);
    }
}
