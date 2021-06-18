package com.dc.videojc.model;

import lombok.Data;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
@Data
public class VideoInfo {
    private String source;
    private Boolean ffmpeg;
    private String targetFormat;
}
