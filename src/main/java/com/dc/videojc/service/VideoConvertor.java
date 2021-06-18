package com.dc.videojc.service;

import com.dc.videojc.model.ConvertContext;

/***
 * 定义一种视频流转换的实现
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
public interface VideoConvertor {
    /**
     * 对于一种转换参数是否支持
     *
     * @param convertContext 上下文
     * @return 是否支持
     */
    boolean isSupport(ConvertContext convertContext);
    
    /**
     * @return 优先级
     */
    default Integer priority() {
        return 0;
    }
    
    /**
     * 开启视频流进行转换的服务
     *
     * @param convertContext 上下文
     * @return 视频流任务(用于管理)
     */
    VideoConvertorTask doConvert(ConvertContext convertContext);
}
