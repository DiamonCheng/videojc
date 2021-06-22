package com.dc.videojc.repository;

import com.dc.videojc.model.SVideoInfo;
import com.dc.videojc.model.VideoInfo;

import java.util.List;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/22
 */
public interface StableVideoTaskRepository {
    void insert(SVideoInfo videoInfo);
    
    void delete(String id);
    
    List<VideoInfo> list();
    
    void flush();
}
