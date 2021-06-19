package com.dc.videojc.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
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
    private boolean notAutoClose;
    @Setter(AccessLevel.PRIVATE)
    private List<ClientInfo> clientList = Collections.synchronizedList(new ArrayList<>());
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
                       ", notAutoClose=" + notAutoClose +
                       '}';
    }
}
