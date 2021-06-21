package com.dc.videojc.service;

import com.dc.videojc.model.ClientInfo;
import com.dc.videojc.model.TaskContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;

/**
 * <p>Descriptions...
 *
 * @author Diamon.Cheng
 * @date 2021/6/19.
 */
@Slf4j
public abstract class AbstractVideoConvertorTask implements VideoConvertorTask {
    protected final TaskContext taskContext;
    protected Runnable onAbort;
    /**
     * flv header
     */
    protected byte[] header;
    
    public AbstractVideoConvertorTask(TaskContext taskContext) {
        this.taskContext = taskContext;
    }
    
    @Override
    public TaskContext getTaskContext() {
        return taskContext;
    }
    
    @Override
    public void setOnAbort(Runnable callable) {
        this.onAbort = callable;
    }
    
    protected void closeAllClient() {
        taskContext.getClientList().forEach(e -> {
            try {
                e.getDataSender().close();
            } catch (Exception ex) {
                log.warn("关闭客户端连接失败", ex);
            }
        });
        taskContext.getClientList().clear();
    }
    
    @Override
    public void addClient(ClientInfo clientInfo) {
        try {
            if (header != null && !clientInfo.isHeaderSent()) {
                clientInfo.getDataSender().send(header);
                clientInfo.setHeaderSent(true);
            }
            taskContext.getClientList().add(clientInfo);
            log.info("转换任务-添加客户端成功[{}][{}][{}]", this.getTaskContext(), this, clientInfo);
        } catch (Exception e) {
            throw new IllegalStateException("发送视频流头部信息失败", e);
        }
    }
    
    protected void sendFrameData(byte[] data) {
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
}
