package com.dc.red5slim.server.stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.codec.AudioCodec;
import org.red5.codec.VideoCodec;
import org.red5.io.ITag;
import org.red5.io.amf.Output;
import org.red5.io.flv.FLVHeader;
import org.red5.io.utils.IOUtils;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.event.Aggregate;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.CachedEvent;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.stream.IStreamData;
import org.red5.server.stream.consumer.ImmutableTag;
import org.red5.server.stream.message.RTMPMessage;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/***
 * 将Red5中的直播流转换成二进制
 * @author Diamon.Cheng
 * @date 2021/9/14
 */
@Slf4j
public abstract class AbstractFlvPublishListener implements IStreamListener, FlvPublishListener {
    
    private int startTimestamp = -1;
    
    /**
     * Video decoder configuration
     */
    private ITag videoConfigurationTag;
    
    /**
     * Keeps track of the last spawned write worker.
     */
    private volatile boolean gotKeyFrame = false;
    
    
    /**
     * Whether or not to wait until a video keyframe arrives before writing video.
     */
    private boolean waitForVideoKeyframe = true;
    // the size of the last tag written, which includes the tag header length
    private volatile int lastTagSize;
    
    /**
     * Length of the flv header in bytes
     */
    private final static int HEADER_LENGTH = 9;
    
    /**
     * Length of the flv tag in bytes
     */
    private final static int TAG_HEADER_LENGTH = 11;
    
    /**
     * Id of the audio codec used.
     */
    private volatile int audioCodecId = -1;
    
    /**
     * Id of the video codec used.
     */
    private volatile int videoCodecId = -1;
    
    /**
     * Sampling rate
     */
    private volatile int soundRate;
    
    /**
     * Size of each audio sample
     */
    private volatile int soundSize;
    
    
    /**
     * If video configuration data has been written
     */
    private final AtomicBoolean configWritten = new AtomicBoolean(false);
    /**
     * 是否解析出音频
     */
    private final AtomicBoolean audioConfigReceived = new AtomicBoolean(false);
    
    /**
     * 是否解析出视频
     */
    private final AtomicBoolean videoConfigReceived = new AtomicBoolean(false);
    /**
     * 接受Tag总数
     */
    private final AtomicLong totalTagCount = new AtomicLong(0L);
    /**
     * For now all recorded streams carry a stream id of 0.
     */
    private final static byte[] DEFAULT_STREAM_ID = new byte[]{(byte) 0, (byte) 0, (byte) 0};
    
    
    /**
     * Duration of the file.
     */
    private int duration;
    
    /**
     * Size of video data
     */
    private int videoDataSize = 0;
    
    /**
     * Size of audio data
     */
    private int audioDataSize = 0;
    
    /*
     * Mono (0) or stereo (1) sound
     */
    private boolean soundType;
    
    private final AtomicReference<byte[]> metaDataBytes = new AtomicReference<>(null);
    private final AtomicReference<byte[]> videoConfigTagDataBytes = new AtomicReference<>(null);
    private final AtomicReference<byte[]> audioConfigTagDataBytes = new AtomicReference<>(null);
    
    private long headerWriteTagCountThreshold = 25;
    
    
    abstract protected void onHeaderReceived(ByteBuffer byteBuffer);
    
    abstract protected void onDataReceived(ByteBuffer byteBuffer);
    
    abstract protected void onStop();
    
    /**
     * A packet has been received from a stream.
     *
     * @param stream the stream the packet has been received for
     * @param packet the packet received
     */
    @Override
    public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
        log.debug("A packet was received by recording listener, but it's not recording anymore. {}", stream.getPublishedName());
        CachedEvent cachedEvent = new CachedEvent();
        cachedEvent.setData(packet.getData().duplicate());
        cachedEvent.setDataType(packet.getDataType());
        cachedEvent.setReceivedTime(System.currentTimeMillis());
        cachedEvent.setTimestamp(packet.getTimestamp());
        IRTMPEvent event = null;
        RTMPMessage message = null;
        // get first event in the queue
        // get the data type
        final byte dataType = cachedEvent.getDataType();
        // get the data
        IoBuffer buffer = cachedEvent.getData();
        // get the current size of the buffer / data
        int bufferLimit = buffer.limit();
        if (bufferLimit > 0) {
            // create new RTMP message and push to the consumer
            switch (dataType) {
                case Constants.TYPE_AGGREGATE:
                    event = new Aggregate(buffer);
                    event.setTimestamp(cachedEvent.getTimestamp());
                    message = RTMPMessage.build(event);
                    break;
                case Constants.TYPE_AUDIO_DATA:
                    event = new AudioData(buffer);
                    event.setTimestamp(cachedEvent.getTimestamp());
                    message = RTMPMessage.build(event);
                    break;
                case Constants.TYPE_VIDEO_DATA:
                    event = new VideoData(buffer);
                    event.setTimestamp(cachedEvent.getTimestamp());
                    message = RTMPMessage.build(event);
                    break;
                default:
                    event = new Notify(buffer);
                    event.setTimestamp(cachedEvent.getTimestamp());
                    message = RTMPMessage.build(event);
                    break;
            }
            // push it down to the recorder
            pushMessage(message);
        } else if (bufferLimit == 0 && dataType == Constants.TYPE_AUDIO_DATA) {
            log.debug("Stream data size was 0, sending empty audio message");
            // allow for 0 byte audio packets
            event = new AudioData(IoBuffer.allocate(0));
            event.setTimestamp(cachedEvent.getTimestamp());
            message = RTMPMessage.build(event);
            // push it down to the recorder
            pushMessage(message);
        } else {
            log.debug("Stream data size was 0, recording pipe will not be notified");
        }
    }
    
    private void pushMessage(RTMPMessage message) {
        final IRTMPEvent msg = message.getBody();
        // if writes are delayed, queue the data and sort it by time
        if (!(msg instanceof IStreamData)) {
            return;
        }
        // get the type
        final byte dataType = msg.getDataType();
        // get the timestamp
        int timestamp = msg.getTimestamp();
        if (log.isTraceEnabled()) {
            log.trace("Stream data, body saved, timestamp: {} data type: {} class type: {}", timestamp, dataType, msg.getClass().getName());
        }
        // if the last message was a reset or we just started, use the header timer
        if (startTimestamp == -1) {
            startTimestamp = timestamp;
            timestamp = 0;
        } else {
            timestamp -= startTimestamp;
        }
        ImmutableTag tag = ImmutableTag.build(dataType, timestamp, ((IStreamData<?>) msg).getData());
        
        boolean video = false;
        boolean audio = false;
        boolean config = false;
        int codecId = -1;
        VideoData.FrameType frameType = null;
        if (msg instanceof VideoData) {
            video = true;
            config = ((VideoData) msg).isConfig();
            codecId = ((VideoData) msg).getCodecId();
            frameType = ((VideoData) msg).getFrameType();
        } else if (msg instanceof AudioData) {
            audio = true;
//            config = ((AudioData) msg).isConfig();
            config = ((AudioData) msg).getData().get(1) == 0;
        }
        // ensure that our first video frame written is a key frame
        boolean gotVideoConfigTag = false;
        if (video) {
            if (log.isTraceEnabled()) {
                log.trace("pushMessage video - waitForKeyframe: {} gotKeyframe: {} timestamp: {}", waitForVideoKeyframe, gotKeyFrame, timestamp);
            }
            if (codecId == VideoCodec.AVC.getId()) {
                if (config) {
                    videoConfigurationTag = tag;
                    gotVideoConfigTag = true;
                    gotKeyFrame = true;
                }
                if (videoConfigurationTag == null && waitForVideoKeyframe) {
                    return;
                }
            } else {
                if (frameType == VideoData.FrameType.KEYFRAME) {
                    gotKeyFrame = true;
                }
                if (waitForVideoKeyframe && !gotKeyFrame) {
                    return;
                }
            }
        }
        
        if (video) {
            if (log.isTraceEnabled()) {
                log.trace("Writing packet. frameType={} timestamp={}", frameType, timestamp);
            }
        }
        
        // write
        final ByteBuffer tagBuffer = convertTag(dataType, timestamp, tag);
        if (tagBuffer != null) {
            final long totalTag = totalTagCount.incrementAndGet();
            if (dataType == ITag.TYPE_METADATA) {
                if (tagBuffer.limit() == tagBuffer.array().length) {
                    metaDataBytes.set(tagBuffer.array());
                } else {
                    byte[] data = new byte[tagBuffer.limit()];
                    tagBuffer.get(data);
                    metaDataBytes.set(data);
                }
            }
            if (gotVideoConfigTag) {
                if (tagBuffer.limit() == tagBuffer.array().length) {
                    videoConfigTagDataBytes.set(tagBuffer.array());
                } else {
                    byte[] data = new byte[tagBuffer.limit()];
                    tagBuffer.get(data);
                    videoConfigTagDataBytes.set(data);
                }
            }
            boolean gotAudioConfigTag = false;
            if (audio && config) {
                gotAudioConfigTag = true;
                if (tagBuffer.limit() == tagBuffer.array().length) {
                    audioConfigTagDataBytes.set(tagBuffer.array());
                } else {
                    byte[] data = new byte[tagBuffer.limit()];
                    tagBuffer.get(data);
                    audioConfigTagDataBytes.set(data);
                }
            }
            
            final boolean refreshAnyConfig = dataType == ITag.TYPE_METADATA || gotVideoConfigTag || gotAudioConfigTag;
            final boolean allDataReady = audioConfigTagDataBytes.get() != null && videoConfigTagDataBytes.get() != null && metaDataBytes.get() != null;
            final boolean isNeedRefresh = refreshAnyConfig && allDataReady || !configWritten.get() && totalTag > headerWriteTagCountThreshold;
            if (isNeedRefresh) {
                writeHeader();
                if (!configWritten.get()) {
                    configWritten.set(true);
                }
            }
            if (configWritten.get()) {
                onDataReceived(tagBuffer);
            }
        }
    }
    
    
    /**
     * Adjust timestamp and write to the file.
     */
    private ByteBuffer convertTag(byte dataType, int timestamp, ITag tag) {
        if (tag != null) {
            // only allow blank tags if they are of audio type
            if (tag.getBodySize() > 0 || dataType == ITag.TYPE_AUDIO) {
                try {
                    if (timestamp >= 0) {
                        return convertTag(tag);
                    } else {
                        log.warn("Skipping message with negative timestamp");
                    }
                } catch (Exception ex) {
                    log.error("write Tag error", ex);
                }
            }
        }
        return null;
    }
    
    /**
     * Writes a Tag object
     *
     * @param tag Tag to write
     */
    private ByteBuffer convertTag(ITag tag) {
        // a/v config written flags
        boolean onWrittenSetVideoFlag = false, onWrittenSetAudioFlag = false;
        /*
         * Tag header = 11 bytes |-|---|----|---| 0 = type 1-3 = data size 4-7 = timestamp 8-10 = stream id (always 0) Tag data = variable bytes Previous tag = 4 bytes (tag header size +
         * tag data size)
         */
        log.trace("writeTag: {}", tag);
        // skip tags with no data
        int bodySize = tag.getBodySize();
        log.trace("Tag body size: {}", bodySize);
        // verify previous tag size stored in incoming tag
        int previousTagSize = tag.getPreviousTagSize();
        if (previousTagSize != lastTagSize) {
            // use the last tag size
            log.trace("Incoming previous tag size: {} does not match current value for last tag size: {}", previousTagSize, lastTagSize);
        }
        // ensure that the channel is still open
        // get the data type
        byte dataType = tag.getDataType();
        // when tag is ImmutableTag which is in red5-server-common.jar, tag.getBody().reset() will throw InvalidMarkException because
        // ImmutableTag.getBody() returns a new IoBuffer instance everytime.
        IoBuffer tagBody = tag.getBody();
        // set a var holding the entire tag size including the previous tag length
        int totalTagSize = TAG_HEADER_LENGTH + bodySize + 4;
        // create a buffer for this tag
        ByteBuffer tagBuffer = ByteBuffer.allocate(totalTagSize);
        // get the timestamp
        int timestamp = tag.getTimestamp();
        // allow for empty tag bodies
        byte[] bodyBuf = null;
        if (bodySize > 0) {
            // create an array big enough
            bodyBuf = new byte[bodySize];
            // put the bytes into the array
            tagBody.get(bodyBuf);
            // get the audio or video codec identifier
            if (dataType == ITag.TYPE_AUDIO) {
                audioDataSize += bodySize;
                if (audioCodecId == -1) {
                    int id = bodyBuf[0] & 0xff; // must be unsigned
                    audioCodecId = (id & ITag.MASK_SOUND_FORMAT) >> 4;
                    log.debug("Audio codec id: {}", audioCodecId);
                    // if aac use defaults
                    if (audioCodecId == AudioCodec.AAC.getId()) {
                        log.trace("AAC audio type");
                        // Flash Player ignores	these values and extracts the channel and sample rate data encoded in the AAC bit stream
                        soundRate = 44100;
                        soundSize = 16;
                        soundType = true;
                        // this is aac data, so a config chunk should be written before any media data
                        if (bodyBuf[1] == 0) {
                            // when this config is written set the flag
                            onWrittenSetAudioFlag = true;
                        } else {
                            // reject packet since config hasnt been written yet
                            log.debug("Rejecting AAC data since config has not yet been written");
                            return null;
                        }
                    } else if (audioCodecId == AudioCodec.OPUS.getId()) {
                        log.trace("OPUS audio type");
                        soundRate = 48000;
                        soundSize = 16;
                        soundType = true;
                    } else if (audioCodecId == AudioCodec.SPEEX.getId()) {
                        log.trace("Speex audio type");
                        soundRate = 5500; // actually 16kHz
                        soundSize = 16;
                        soundType = false; // mono
                    } else {
                        switch ((id & ITag.MASK_SOUND_RATE) >> 2) {
                            case ITag.FLAG_RATE_5_5_KHZ:
                                soundRate = 5500;
                                break;
                            case ITag.FLAG_RATE_11_KHZ:
                                soundRate = 11000;
                                break;
                            case ITag.FLAG_RATE_22_KHZ:
                                soundRate = 22000;
                                break;
                            case ITag.FLAG_RATE_44_KHZ:
                                soundRate = 44100;
                                break;
                            case ITag.FLAG_RATE_48_KHZ:
                                soundRate = 48000;
                                break;
                            default:
                        }
                        log.debug("Sound rate: {}", soundRate);
                        switch ((id & ITag.MASK_SOUND_SIZE) >> 1) {
                            case ITag.FLAG_SIZE_8_BIT:
                                soundSize = 8;
                                break;
                            case ITag.FLAG_SIZE_16_BIT:
                                soundSize = 16;
                                break;
                            default:
                        }
                        log.debug("Sound size: {}", soundSize);
                        // mono == 0 // stereo == 1
                        soundType = (id & ITag.MASK_SOUND_TYPE) > 0;
                        log.debug("Sound type: {}", soundType);
                    }
                } else if (!audioConfigReceived.get()) {
                    if (audioCodecId == AudioCodec.AAC.getId()) {
                        // this is aac data, so a config chunk should be written before any media data
                        if (bodyBuf[1] == 0) {
                            // when this config is written set the flag
                            onWrittenSetAudioFlag = true;
                        } else {
                            // reject packet since config hasnt been written yet
                            return null;
                        }
                    }
                }
            } else if (dataType == ITag.TYPE_VIDEO) {
                videoDataSize += bodySize;
                if (videoCodecId == -1) {
                    int id = bodyBuf[0] & 0xff; // must be unsigned
                    videoCodecId = id & ITag.MASK_VIDEO_CODEC;
                    log.debug("Video codec id: {}", videoCodecId);
                    if (videoCodecId == VideoCodec.AVC.getId()) {
                        // this is avc/h264 data, so a config chunk should be written before any media data
                        if (bodyBuf[1] == 0) {
                            // when this config is written set the flag
                            onWrittenSetVideoFlag = true;
                        } else {
                            // reject packet since config hasnt been written yet
                            log.debug("Rejecting AVC data since config has not yet been written");
                            return null;
                        }
                    } else if (videoCodecId == VideoCodec.HEVC.getId()) {
                        // this is HEVC data, so a config chunk should be written before any media data
                        if (bodyBuf[1] == 0) {
                            // when this config is written set the flag
                            onWrittenSetVideoFlag = true;
                        } else {
                            // reject packet since config hasnt been written yet
                            log.debug("Rejecting HEVC data since config has not yet been written");
                            return null;
                        }
                    }
                } else if (!videoConfigReceived.get()) {
                    if (videoCodecId == VideoCodec.AVC.getId()) {
                        // this is avc/h264 data, so a config chunk should be written before any media data
                        if (bodyBuf[1] == 0) {
                            // when this config is written set the flag
                            onWrittenSetVideoFlag = true;
                        } else {
                            // reject packet since config hasnt been written yet
                            log.debug("Rejecting AVC data since config has not yet been written");
                            return null;
                        }
                    } else if (videoCodecId == VideoCodec.HEVC.getId()) {
                        // this is hevc data, so a config chunk should be written before any media data
                        if (bodyBuf[1] == 0) {
                            // when this config is written set the flag
                            onWrittenSetVideoFlag = true;
                        } else {
                            // reject packet since config hasnt been written yet
                            log.debug("Rejecting HEVC data since config has not yet been written");
                            return null;
                        }
                    }
                }
            }
        }
        // Data Type
        IOUtils.writeUnsignedByte(tagBuffer, dataType); //1
        // Body Size - Length of the message. Number of bytes after StreamID to end of tag
        // (Equal to length of the tag - 11)
        IOUtils.writeMediumInt(tagBuffer, bodySize); //3
        // Timestamp
        IOUtils.writeExtendedMediumInt(tagBuffer, timestamp); //4
        // Stream id
        tagBuffer.put(DEFAULT_STREAM_ID); //3
        // get the body if we have one
        if (bodyBuf != null) {
            tagBuffer.put(bodyBuf);
        }
        // store new previous tag size
        lastTagSize = TAG_HEADER_LENGTH + bodySize;
        // we add the tag size
        tagBuffer.putInt(lastTagSize);
        // flip so we can process from the beginning
        tagBuffer.flip();
        log.debug("Current duration: {} timestamp: {}", duration, timestamp);
        duration = Math.max(duration, timestamp);
        // write the tag
        if (onWrittenSetAudioFlag) {
            audioConfigReceived.set(true);
            log.trace("Audio configuration written");
        }
        if (onWrittenSetVideoFlag) {
            videoConfigReceived.set(true);
            log.trace("Video configuration written");
        }
        
        // update the duration
        log.debug("Current duration: {} timestamp: {}", duration, timestamp);
        duration = Math.max(duration, timestamp);
        // validate written amount
        return tagBuffer;
    }
    
    private void writeHeader() {
        // create a buffer
        // FLVHeader (9 bytes) + PreviousTagSize0 (4 bytes)
        final ByteBuffer headerBuf = ByteBuffer.allocate(HEADER_LENGTH + 4);
        // instance an flv header
        final FLVHeader flvHeader = new FLVHeader();
        flvHeader.setFlagAudio(audioCodecId != -1);
        flvHeader.setFlagVideo(videoCodecId != -1);
        // write the flv header in the buffer
        flvHeader.write(headerBuf);
        
        byte[] metadata2 = metaDataBytes.get();
        final byte[] videoConfigData = videoConfigTagDataBytes.get();
        final byte[] audioConfigData = audioConfigTagDataBytes.get();
        
        if (metadata2 == null) {
            metadata2 = writeMetadataTag(duration / 1000d, videoCodecId, audioCodecId);
            log.warn("没有接收到metadata帧, 只能自动生成元数据");
        }
        
        final int totalSize = headerBuf.limit()
                                      + metadata2.length
                                      + (videoConfigData == null ? 0 : videoConfigData.length)
                                      + (audioConfigData == null ? 0 : audioConfigData.length);
        final ByteBuffer headerAndMetaBuf = ByteBuffer.allocate(totalSize);
        headerAndMetaBuf.put(headerBuf);
        headerAndMetaBuf.put(metadata2);
        if (videoConfigData != null) {
            headerAndMetaBuf.put(videoConfigData);
        } else {
            log.warn("没有接收到视频流配置Tag");
        }
        if (audioConfigData != null) {
            headerAndMetaBuf.put(audioConfigData);
        } else {
            log.warn("没有接收到音频流配置Tag");
        }
        headerAndMetaBuf.flip();
        onHeaderReceived(headerAndMetaBuf);
        headerBuf.clear();
    }
    
    
    /**
     * Write "onMetaData" tag to the file.
     * 弃用, 使用推流中自带的metaData
     *
     * @param duration     Duration to write in milliseconds.
     * @param videoCodecId Id of the video codec used while recording.
     * @param audioCodecId Id of the audio codec used while recording.
     */
    private byte[] writeMetadataTag(double duration, int videoCodecId, int audioCodecId) {
        log.debug("writeMetadataTag - duration: {} video codec: {} audio codec: {}", duration, videoCodecId, audioCodecId);
        IoBuffer buf = IoBuffer.allocate(256);
        buf.setAutoExpand(true);
        Output out = new Output(buf);
        out.writeString("onMetaData");
        Map<Object, Object> params = new HashMap<>();
        params.putIfAbsent("server", "Red5");
        params.put("duration", 0);
        if (log.isDebugEnabled()) {
            log.debug("Stored duration: {}", params.get("duration"));
        }
        if (videoCodecId != -1) {
            params.put("videocodecid", (videoCodecId == 7 ? "avc1" : (videoCodecId == 12 ? "hevc" : videoCodecId)));
            if (videoDataSize > 0) {
                params.put("videodatarate", 8 * videoDataSize / 1024 / duration); //from bytes to kilobits
            }
        } else {
            // place holder
            params.put("novideocodec", 0);
        }
        if (audioCodecId != -1) {
            params.put("audiocodecid", (audioCodecId == 10 ? "mp4a" : (audioCodecId == 13 ? "opus" : audioCodecId)));
            if (audioCodecId == AudioCodec.AAC.getId()) {
                params.put("audiosamplerate", 44100);
                params.put("audiosamplesize", 16);
            } else if (audioCodecId == AudioCodec.SPEEX.getId()) {
                params.put("audiosamplerate", 16000);
                params.put("audiosamplesize", 16);
            } else {
                params.put("audiosamplerate", soundRate);
                params.put("audiosamplesize", soundSize);
            }
            params.put("stereo", soundType);
            if (audioDataSize > 0) {
                params.put("audiodatarate", 8 * audioDataSize / 1024 / duration); //from bytes to kilobits
            }
        } else {
            // place holder
            params.put("noaudiocodec", 0);
        }
        // this is actual only supposed to be true if the last video frame is a keyframe
        params.put("canSeekToEnd", true);
        out.writeMap(params);
        buf.flip();
        int bodySize = buf.limit();
        log.debug("Metadata size: {}", bodySize);
        // set a var holding the entire tag size including the previous tag length
        int totalTagSize = TAG_HEADER_LENGTH + bodySize + 4;
        // create a buffer for this tag
        ByteBuffer tagBuffer = ByteBuffer.allocate(totalTagSize);
        // get the timestamp
        int timestamp = 0;
        // create an array big enough
        byte[] bodyBuf = new byte[bodySize];
        // put the bytes into the array
        buf.get(bodyBuf);
        // Data Type
        IOUtils.writeUnsignedByte(tagBuffer, ITag.TYPE_METADATA); //1
        // Body Size - Length of the message. Number of bytes after StreamID to end of tag
        // (Equal to length of the tag - 11)
        IOUtils.writeMediumInt(tagBuffer, bodySize); //3
        // Timestamp
        IOUtils.writeExtendedMediumInt(tagBuffer, timestamp); //4
        // Stream id
        tagBuffer.put(DEFAULT_STREAM_ID); //3
        if (log.isTraceEnabled()) {
            log.trace("Tag buffer (after tag header) limit: {} remaining: {}", tagBuffer.limit(), tagBuffer.remaining());
        }
        // get the body
        tagBuffer.put(bodyBuf);
        if (log.isTraceEnabled()) {
            log.trace("Tag buffer (after body) limit: {} remaining: {}", tagBuffer.limit(), tagBuffer.remaining());
        }
        // we add the tag size
        tagBuffer.putInt(TAG_HEADER_LENGTH + bodySize);
        if (log.isTraceEnabled()) {
            log.trace("Tag buffer (after prev tag size) limit: {} remaining: {}", tagBuffer.limit(), tagBuffer.remaining());
        }
        // flip so we can process from the beginning
        tagBuffer.flip();
        if (tagBuffer.limit() == tagBuffer.array().length) {
            return tagBuffer.array();
        } else {
            byte[] data = new byte[tagBuffer.limit()];
            tagBuffer.get(data);
            return data;
        }
    }
    
    /**
     * Stop the recording.
     */
    @Override
    public void stop() {
        onStop();
    }
    
    /**
     * Whether or not to wait for the first keyframe before processing video frames.
     *
     * @param waitForVideoKeyframe wait for key frame or not
     */
    public void setWaitForVideoKeyframe(boolean waitForVideoKeyframe) {
        log.debug("setWaitForVideoKeyframe: {}", waitForVideoKeyframe);
        this.waitForVideoKeyframe = waitForVideoKeyframe;
    }
    
    @Override
    public void setAudioDecoderConfiguration(AudioData audioConfig) {
        //obs推流没有这个
        log.info("{}", audioConfig);
    }
    
    @Override
    public void setVideoDecoderConfiguration(VideoData videoConfig) {
        //obs推流没有这个
        log.info("{}", videoConfig);
    }
    
    public AbstractFlvPublishListener setHeaderWriteTagCountThreshold(long headerWriteTagCountThreshold) {
        this.headerWriteTagCountThreshold = headerWriteTagCountThreshold;
        return this;
    }
}
