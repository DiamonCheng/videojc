package com.dc.videojc.controller;

import com.dc.videojc.config.SpringContextUtil;
import com.dc.videojc.config.WebsocketConfiguration;
import com.dc.videojc.model.ClientInfo;
import com.dc.videojc.model.VideoInfo;
import com.dc.videojc.service.ConvertService;
import com.dc.videojc.service.DataSender;
import com.dc.videojc.service.WebSocketDataSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.Date;
import java.util.List;
import java.util.Map;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/21
 */
@ServerEndpoint(value = "/live", configurator = WebsocketConfiguration.class)
@Component
@Slf4j
public class StreamWsController {
    /**
     * 收到客户端发来消息
     *
     * @param message 消息对象
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.warn("有用户发来了没有用的消息 {},{}", message, session);
    }
    
    @OnOpen
    public void onOpen(Session session) {
        final ConvertService convertService = SpringContextUtil.getBean(ConvertService.class);
        final Map<String, List<String>> params = session.getRequestParameterMap();
        final VideoInfo videoInfo = new VideoInfo();
        videoInfo.setTargetFormat(params.get("targetFormat") == null ? null : params.get("targetFormat").stream().findFirst().orElse(null));
        videoInfo.setSource(params.get("source") == null ? null : params.get("source").stream().findFirst().orElse(null));
        videoInfo.setFfmpeg(params.get("ffmpeg") == null ? null : params.get("ffmpeg").stream().findFirst().map(Boolean::parseBoolean).orElse(null));
        final DataSender dataSender = new WebSocketDataSender(session);
        final ClientInfo clientInfo = new ClientInfo();
        clientInfo.setDataSender(dataSender);
        clientInfo.setConnectTime(new Date());
        clientInfo.setClientIp(getIpAddress(session));
        convertService.doConvert(clientInfo, videoInfo);
    }
    
    /**
     * 客户端关闭
     *
     * @param session session
     */
    @OnClose
    public void onClose(Session session) {
        //...
    }
    
    /**
     * 获取request的客户端IP地址
     * * 获取IP地址失败
     *
     * @return ip
     */
    private static String getIpAddress(Session session) {
        if (session.getUserProperties().get("CLIENT_IP_KEY") != null) {
            return session.getUserProperties().get("CLIENT_IP_KEY").toString();
        }
        return "null";
    }
}
