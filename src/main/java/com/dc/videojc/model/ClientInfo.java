package com.dc.videojc.model;

import lombok.Data;

import java.util.Date;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
@Data
public class ClientInfo {
    private String clientIp;
    private Date connectTime;
}
