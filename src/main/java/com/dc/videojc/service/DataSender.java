package com.dc.videojc.service;

import java.io.IOException;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
public interface DataSender {
    void send(byte[] data) throws IOException;
    
    void close();
}
