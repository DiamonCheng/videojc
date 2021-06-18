package com.dc.videojc.service.javacv;

import com.dc.videojc.model.ClientInfo;
import com.dc.videojc.service.VideoConvertorTask;

import java.util.List;

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
    public String getId() {
        return null;
    }
    
    @Override
    public List<ClientInfo> clientList() {
        return null;
    }
}
