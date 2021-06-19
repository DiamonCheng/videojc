package com.dc.videojc.model;

import lombok.Data;

import java.util.List;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
@Data
public class TaskContext {
    private String id;
    private VideoInfo videoInfo;
    private List<ClientInfo> clientList;
    private Long lastNoClientTime;
    private String sourceProtocol;
    
    /**
     * toString
     */
    @Override
    public String toString() {
        return "TaskContext{" +
                       "id='" + id + '\'' +
                       ", videoInfo=" + videoInfo +
                       '}';
    }
}
