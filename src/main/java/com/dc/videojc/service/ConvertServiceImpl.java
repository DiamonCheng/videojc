package com.dc.videojc.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.dc.videojc.config.SpringContextUtil;
import com.dc.videojc.model.ClientInfo;
import com.dc.videojc.model.ConvertContext;
import com.dc.videojc.model.SVideoInfo;
import com.dc.videojc.model.TaskContext;
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
            log.info("开始启动{}个固定转换任务", stableTasks.size());
            stableTasks.forEach(this::addStableTask0);
            stableVideoTaskRepository.flush();
            log.info("成功启动{}个固定转换任务", stableTasks.size());
        }
    }
    
    @PreDestroy
    public void destroy() {
        log.info("停止所有转换任务");
        videoConvertorTaskMap.values().forEach(VideoConvertorTask::shutdown);
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
                log.info("转换任务正在运行 不需要重启[{}][{}]", task.getTaskContext(), task);
                return;
            }
            log.info("转换任务-开始重启[{}][{}]", task.getTaskContext(), task);
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
            throw new IllegalStateException("请求的资源不存在!");
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
            throw new IllegalStateException("指定资源不存在 " + taskId);
        } else if (!convertTask.getTaskContext().isCloseable()) {
            throw new IllegalStateException("指定资源不能被删除 " + taskId);
        } else {
            convertTask.getTaskContext().setNotAutoClose(false);
        }
        
        stableVideoTaskRepository.delete(taskId);
    }
    
    @Override
    public void restartTask(String taskId) {
        VideoConvertorTask convertTask = videoConvertorTaskMap.get(taskId);
        if (convertTask == null) {
            throw new IllegalStateException("指定资源不存在 " + taskId);
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
                    throw new IllegalStateException("持久任务已经存在 " + convertTask.getTaskContext());
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
     * 开启一个转换任务
     * 至少开1个线程/进程
     * 阻塞运行直到新任务初始化完成,并完成头部文件的处理
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
            // 如果是持久的任务即使不在运行也会重启
            if (unusableTask && unstableTask) {
                VideoConvertor videoConvertor = videoConvertors.stream()
                                                        .filter(e -> e.isSupport(convertContext))
                                                        .findFirst()
                                                        .orElseThrow(() -> new IllegalStateException("当前转换不支持 " + videoInfo));
                convertTask = videoConvertor.prepareConvert(convertContext);
                final VideoConvertorTask finalConvertTask = convertTask;
                convertTask.setOnAbort(() -> {
                    if (convertContext.getAutoClose()) {
                        //防止移除掉重新生成的新任务
                        videoConvertorTaskMap.remove(convertContext.getTaskId(), finalConvertTask);
                    }
                    log.info("转换任务-(可能是异常)结束![{}][{}]", finalConvertTask.getTaskContext(), finalConvertTask);
                });
                videoConvertorTaskMap.put(convertContext.getTaskId(), convertTask);
                log.info("转换任务-启动中...[{}][{}]", convertTask.getTaskContext(), convertTask);
                newTask = true;
            }
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
        if (clientInfo != null) {
            convertTask.addClient(clientInfo);
        }
    }
    
    @Override
    public void registerUnCloseableTask(VideoConvertorTask videoConvertorTask) {
        final TaskContext taskContext = videoConvertorTask.getTaskContext();
        videoConvertorTask.setOnAbort(() -> {
            videoConvertorTaskMap.remove(taskContext.getId());
            log.info("转换任务-(可能是异常)结束![{}][{}]", taskContext, videoConvertorTask);
        });
        videoConvertorTaskMap.put(taskContext.getId(), videoConvertorTask);
        videoConvertorTask.init();
        threadPoolTaskExecutor.execute(videoConvertorTask);
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
