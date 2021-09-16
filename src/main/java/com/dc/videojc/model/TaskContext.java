package com.dc.videojc.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private Collection<ClientInfo> clientList = new ConcurrentLinkedQueue<>();
    private Long lastNoClientTime;
    private String sourceProtocol;
    private boolean closeable = true;
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
