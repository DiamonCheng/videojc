package com.dc.videojc.controller;

import com.dc.videojc.model.VideoInfo;
import com.dc.videojc.service.ConvertService;
import com.dc.videojc.service.EmitterDataSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
@RestController
@RequestMapping
public class StreamController {
    @Autowired
    private ConvertService convertService;
    
    @GetMapping("live")
    public Object live(VideoInfo videoInfo) {
        ResponseBodyEmitter responseBodyEmitter = new ResponseBodyEmitter();
        convertService.doConvert(new EmitterDataSender(responseBodyEmitter), videoInfo);
        return responseBodyEmitter;
    }
}
