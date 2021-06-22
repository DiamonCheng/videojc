package com.dc.videojc.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.stream.Collectors;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskInfoVO extends SVideoInfo {
    private Boolean notAutoClose;
    private List<ClientInfoVO> clientList;
    private Long lastNoClientTime;
    private String sourceProtocol;
    
    public TaskInfoVO(TaskContext taskContext) {
        this.setId(taskContext.getId());
        this.setFfmpeg(taskContext.getVideoInfo().getFfmpeg());
        this.setSource(taskContext.getVideoInfo().getSource());
        this.setTargetFormat(taskContext.getVideoInfo().getTargetFormat());
        this.notAutoClose = taskContext.isNotAutoClose();
        this.lastNoClientTime = taskContext.getLastNoClientTime();
        this.sourceProtocol = taskContext.getSourceProtocol();
        this.clientList = taskContext.getClientList().stream().map(e -> {
            ClientInfoVO clientInfoVO = new ClientInfoVO();
            clientInfoVO.setClientIp(e.getClientIp());
            clientInfoVO.setConnectTime(e.getConnectTime());
            clientInfoVO.setDataSender(e.getDataSender().getClass().getSimpleName());
            return clientInfoVO;
        }).collect(Collectors.toList());
    }
}
