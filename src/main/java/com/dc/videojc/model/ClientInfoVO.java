package com.dc.videojc.model;

import lombok.Data;

import java.util.Date;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/22
 */
@Data
public class ClientInfoVO {
    private String clientIp;
    private Date connectTime;
    private String dataSender;
}
