package com.dc.videojc.service;

import com.dc.videojc.model.ConvertContext;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
public interface VideoConvertor {
    boolean isSupport(ConvertContext convertContext);
    
    Integer priority();
    
    VideoConvertorTask doConvert(ConvertContext convertContext);
}
