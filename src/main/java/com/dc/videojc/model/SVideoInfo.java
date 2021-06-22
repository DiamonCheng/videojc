package com.dc.videojc.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SVideoInfo extends VideoInfo {
    private String id;
    
    public SVideoInfo fromVideoInfo(VideoInfo videoInfo, String id) {
        this.id = id;
        this.setFfmpeg(videoInfo.getFfmpeg());
        this.setSource(videoInfo.getSource());
        this.setTargetFormat(videoInfo.getTargetFormat());
        return this;
    }
    
    @Override
    public String toString() {
        return super.toString();
    }
}
