package com.dc.red5slim.server.stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.codec.IAudioStreamCodec;
import org.red5.codec.IStreamCodecInfo;
import org.red5.codec.IVideoStreamCodec;
import org.red5.codec.StreamCodecInfo;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.stream.ClientBroadcastStream;

/***
 * 接入 FlvPublishListener
 * @author Diamon.Cheng
 * @date 2021/9/14
 */
@Slf4j
public class FlvPublishClientBroadcastStream extends ClientBroadcastStream {
    private FlvPublishListenerFactory flvPublishListenerFactory;
    private FlvPublishListener flvPublishListener;
    
    public void setFlvPublishListenerFactory(FlvPublishListenerFactory flvPublishListenerFactory) {
        this.flvPublishListenerFactory = flvPublishListenerFactory;
    }
    
    @Override
    public void startPublishing() {
        super.startPublishing();
        if (flvPublishListenerFactory == null) {
            log.warn("No flvPublishListenerFactory assigned. Con not publish flv data");
            return;
        }
        IStreamCapableConnection conn = getConnection();
        if (conn == null) {
            log.warn("Stream is no longer connected");
            return;
        }
        FlvPublishListener listener = flvPublishListenerFactory.create(conn.getScope(), publishedName);
        //log.debug("Created: {}", listener);
        // initialize the listener
        // get decoder info if it exists for the stream
        IStreamCodecInfo codecInfo = getCodecInfo();
        //log.debug("Codec info: {}", codecInfo);
        if (codecInfo instanceof StreamCodecInfo) {
            StreamCodecInfo info = (StreamCodecInfo) codecInfo;
            IVideoStreamCodec videoCodec = info.getVideoCodec();
            //log.debug("Video codec: {}", videoCodec);
            if (videoCodec != null) {
                //check for decoder configuration to send
                IoBuffer config = videoCodec.getDecoderConfiguration();
                if (config != null) {
                    //log.debug("Decoder configuration is available for {}", videoCodec.getName());
                    VideoData videoConf = new VideoData(config.asReadOnlyBuffer());
                    try {
                        //log.debug("Setting decoder configuration for recording");
                        listener.setVideoDecoderConfiguration(videoConf);
                    } finally {
                        videoConf.release();
                    }
                }
            } else {
                log.debug("Could not initialize stream output, videoCodec is null.");
            }
            IAudioStreamCodec audioCodec = info.getAudioCodec();
            //log.debug("Audio codec: {}", audioCodec);
            if (audioCodec != null) {
                //check for decoder configuration to send
                IoBuffer config = audioCodec.getDecoderConfiguration();
                if (config != null) {
                    //log.debug("Decoder configuration is available for {}", audioCodec.getName());
                    AudioData audioConf = new AudioData(config.asReadOnlyBuffer());
                    try {
                        //log.debug("Setting decoder configuration for recording");
                        listener.setAudioDecoderConfiguration(audioConf);
                    } finally {
                        audioConf.release();
                    }
                }
            } else {
                log.debug("No decoder configuration available, audioCodec is null.");
            }
            // set as primary listener
            flvPublishListenerFactory.start(listener);
            flvPublishListener = listener;
            // add as a listener
            addStreamListener(listener);
        } else {
            log.warn("Flv Publish listener failed to initialize for stream: {}", publishedName);
        }
    }
    
    @Override
    public void close() {
        if (flvPublishListener != null) {
            flvPublishListener.stop();
        }
        super.close();
    }
}
