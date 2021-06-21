package com.dc.videojc.service;

import lombok.extern.slf4j.Slf4j;

import javax.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
@Slf4j
public class WebSocketDataSender implements DataSender {
    private final Session session;
    
    public WebSocketDataSender(Session session) {
        this.session = session;
    }
    
    @Override
    public void send(byte[] data) throws IOException {
        session.getBasicRemote().sendBinary(ByteBuffer.wrap(data));
    }
    
    @Override
    public void close() {
        try {
            session.close();
        } catch (IOException e) {
            log.warn("session 关闭失败" + session, e);
        }
    }
}
