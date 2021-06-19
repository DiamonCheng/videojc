package com.dc.videojc.service;

import com.dc.videojc.model.TaskContext;

/**
 * <p>Descriptions...
 *
 * @author Diamon.Cheng
 * @date 2021/6/19.
 */
public abstract class AbstractVideoConvertorTask implements VideoConvertorTask {
    protected TaskContext taskContext;
    protected Runnable onAbort;
    
    public void setTaskContext(TaskContext taskContext) {
        this.taskContext = taskContext;
    }
    
    @Override
    public TaskContext getTaskContext() {
        return null;
    }
    
    @Override
    public void setOnAbort(Runnable callable) {
        this.onAbort = callable;
    }
}
