package com.dc.videojc.service;

import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
public class EmitterDataSender implements DataSender {
    private final ResponseBodyEmitter emitter;
    
    public EmitterDataSender(ResponseBodyEmitter emitter) {
        this.emitter = emitter;
    }
    
    @Override
    public void send(byte[] data) throws IOException {
        emitter.send(data);
    }
    
    @Override
    public void close() {
        emitter.complete();
    }
}
