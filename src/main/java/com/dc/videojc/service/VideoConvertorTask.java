package com.dc.videojc.service;

import com.dc.videojc.model.ClientInfo;

import java.util.List;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
public interface VideoConvertorTask {
    void start();
    
    void shutdown();
    
    String getId();
    
    List<ClientInfo> clientList();
}
