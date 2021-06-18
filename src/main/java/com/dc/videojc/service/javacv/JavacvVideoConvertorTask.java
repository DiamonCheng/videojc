package com.dc.videojc.service.javacv;

import com.dc.videojc.model.TaskContext;
import com.dc.videojc.service.VideoConvertorTask;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
public class JavacvVideoConvertorTask implements VideoConvertorTask {
    
    
    @Override
    public void start() {
    
    }
    
    @Override
    public void shutdown() {
    
    }
    
    @Override
    public TaskContext getTaskContext() {
        return null;
    }
    
    @Override
    public void setOnAbort(Runnable callable) {
    
    }
    
}
