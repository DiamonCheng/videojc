package com.dc.videojc.service;

import com.dc.videojc.model.ClientInfo;
import com.dc.videojc.model.TaskContext;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
public interface VideoConvertorTask extends Runnable {
    @Override
    void run();
    
    void shutdown();
    
    TaskContext getTaskContext();
    
    void setOnAbort(Runnable callable);
    
    void addClient(ClientInfo clientInfo);
    
    void init();
    
    boolean isRunning();
}
