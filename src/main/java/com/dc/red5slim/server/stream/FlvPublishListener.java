package com.dc.red5slim.server.stream;

import org.red5.server.api.stream.IStreamListener;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.VideoData;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/9/15
 */
public interface FlvPublishListener extends IStreamListener {
    void stop();
    
    void setAudioDecoderConfiguration(AudioData audioConfig);
    
    void setVideoDecoderConfiguration(VideoData videoConfig);
}
