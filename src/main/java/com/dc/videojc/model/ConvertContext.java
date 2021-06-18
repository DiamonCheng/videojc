package com.dc.videojc.model;

import com.dc.videojc.service.DataSender;
import lombok.Data;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
@Data
public class ConvertContext {
    private String taskId;
    private String sourceProtocol;
    private VideoInfo videoInfo;
    private DataSender dataSender;
    private ClientInfo clientInfo;
}
