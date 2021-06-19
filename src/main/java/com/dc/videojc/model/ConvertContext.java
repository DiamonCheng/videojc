package com.dc.videojc.model;

import lombok.Data;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
@Data
public class ConvertContext {
    private String taskId;
    private VideoInfo videoInfo;
    private ClientInfo clientInfo;
    private String sourceProtocol;
    private Boolean autoClose;
}
