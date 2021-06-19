package com.dc.videojc.service;

import com.dc.videojc.model.ClientInfo;
import com.dc.videojc.model.TaskContext;
import lombok.extern.slf4j.Slf4j;

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
    
    }
    
}
