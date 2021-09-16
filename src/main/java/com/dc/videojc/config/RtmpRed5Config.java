package com.dc.videojc.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/***
 * 根据application.properties启用 red5 rtmp 服务器
 * @author Diamon.Cheng
 * @date 2021/9/14
 */
@Configuration
@ConditionalOnProperty(name = "videojc.rtmp-red5.enabled")
@ImportResource({"classpath:red5.xml"})
public class RtmpRed5Config {
    public RtmpRed5Config() {
        System.setProperty("red5.deployment.type", "spring-boot");
        
    }
}
