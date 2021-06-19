package com.dc.videojc.service;

import com.dc.videojc.model.TaskContext;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
public interface VideoConvertorTask extends Runnable {
    void run();
    
    void shutdown();
    
    TaskContext getTaskContext();
    
    void setOnAbort(Runnable callable);
}
