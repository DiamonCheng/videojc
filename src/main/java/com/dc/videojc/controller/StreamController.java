package com.dc.videojc.controller;

import com.dc.videojc.base.AjaxResult;
import com.dc.videojc.config.WebContextBinder;
import com.dc.videojc.model.ClientInfo;
import com.dc.videojc.model.VideoInfo;
import com.dc.videojc.service.ConvertService;
import com.dc.videojc.service.DataSender;
import com.dc.videojc.service.EmitterDataSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

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
        final ResponseBodyEmitter responseBodyEmitter = new ResponseBodyEmitter(-1L);
        final DataSender dataSender = new EmitterDataSender(responseBodyEmitter);
        final ClientInfo clientInfo = new ClientInfo();
        clientInfo.setDataSender(dataSender);
        clientInfo.setConnectTime(new Date());
        clientInfo.setClientIp(getIpAddress());
        convertService.doConvert(clientInfo, videoInfo);
        return responseBodyEmitter;
    }
    
    @GetMapping(value = "live/{taskId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Object liveTaskId(@PathVariable String taskId) {
        final ResponseBodyEmitter responseBodyEmitter = new ResponseBodyEmitter(-1L);
        final DataSender dataSender = new EmitterDataSender(responseBodyEmitter);
        final ClientInfo clientInfo = new ClientInfo();
        clientInfo.setDataSender(dataSender);
        clientInfo.setConnectTime(new Date());
        clientInfo.setClientIp(getIpAddress());
        convertService.mergeClient(taskId, clientInfo);
        return responseBodyEmitter;
    }
    
    /**
     * 获取request的客户端IP地址
     *
     * @return ip
     */
    private static String getIpAddress() {
        HttpServletRequest request = WebContextBinder.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    @GetMapping("livem")
    public AjaxResult list() {
        return new AjaxResult().setData(convertService.listTasks());
    }
    
    @PostMapping("livem")
    public AjaxResult add(@RequestBody VideoInfo videoInfo) {
        convertService.addStableTask(videoInfo);
        return new AjaxResult();
    }
    
    @DeleteMapping("livem/{taskId}")
    public Object delete(@PathVariable String taskId) {
        convertService.deleteStableTask(taskId);
        return new AjaxResult();
    }
    
    @PostMapping("livem/{taskId}/restart")
    public AjaxResult restart(@PathVariable String taskId) {
        convertService.restartTask(taskId);
        return new AjaxResult();
    }
}
