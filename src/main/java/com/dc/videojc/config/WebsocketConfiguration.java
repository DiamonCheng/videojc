package com.dc.videojc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Map;

/**
 * 添加websocket 支持
 */
@Configuration
public class WebsocketConfiguration extends ServerEndpointConfig.Configurator {
    public static final String CLIENT_IP_KEY = "CLIENT_IP_KEY";
    
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
    
    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        String clientIp = getClientIp(request);
        config.getUserProperties().put(CLIENT_IP_KEY, String.valueOf(clientIp));
    }
    
    private String getClientIp(HandshakeRequest request) {
        Map<String, List<String>> headers = request.getHeaders();
        String ip = headers.get("X-Forwarded-For") == null ? null : headers.get("X-Forwarded-For").stream().findFirst().orElse(null);
        
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.get("Proxy-Client-IP") == null ? null : headers.get("Proxy-Client-IP").stream().findFirst().orElse(null);
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.get("WL-Proxy-Client-IP") == null ? null : headers.get("WL-Proxy-Client-IP").stream().findFirst().orElse(null);
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.get("HTTP_CLIENT_IP") == null ? null : headers.get("HTTP_CLIENT_IP").stream().findFirst().orElse(null);
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.get("HTTP_X_FORWARDED_FOR") == null ? null : headers.get("HTTP_X_FORWARDED_FOR").stream().findFirst().orElse(null);
        }
        return ip;
    }
    
}
