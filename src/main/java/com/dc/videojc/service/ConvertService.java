package com.dc.videojc.service;

import com.dc.videojc.model.ClientInfo;
import com.dc.videojc.model.TaskInfoVO;
import com.dc.videojc.model.VideoInfo;

import java.util.List;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
public interface ConvertService {
    
    void mergeClient(String taskId, ClientInfo clientInfo);
    
    void addStableTask(VideoInfo videoInfo);
    
    List<TaskInfoVO> listTasks();
    
    void deleteStableTask(String taskId);
    
    void restartTask(String taskId);
    
    void doConvert(ClientInfo clientInfo, VideoInfo videoInfo);
    
    void registerUnCloseableTask(VideoConvertorTask videoConvertorTask);
}
