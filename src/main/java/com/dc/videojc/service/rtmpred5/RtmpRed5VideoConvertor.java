package com.dc.videojc.service.rtmpred5;

import com.dc.red5slim.server.stream.FlvPublishListener;
import com.dc.red5slim.server.stream.FlvPublishListenerFactory;
import com.dc.videojc.model.TaskContext;
import com.dc.videojc.model.VideoInfo;
import com.dc.videojc.service.ConvertService;
import com.dc.videojc.service.VideoConvertorTask;
import org.red5.server.api.scope.IScope;
import org.springframework.beans.factory.annotation.Autowired;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/9/15
 */
public class RtmpRed5VideoConvertor implements FlvPublishListenerFactory {
    @Autowired
    private ConvertService convertService;
    
    private boolean useDataQueue = false;
    
    @Override
    public FlvPublishListener create(IScope scope, String publishedName) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setFfmpeg(false);
        videoInfo.setTargetFormat("flv");
        videoInfo.setSource("rtmp-publish://" + scope.getName() + "/" + publishedName);
        TaskContext taskContext = new TaskContext();
        taskContext.setNotAutoClose(true);
        taskContext.setId(publishedName);
        taskContext.setSourceProtocol("rtmp-publish");
        taskContext.setCloseable(false);
        taskContext.setVideoInfo(videoInfo);
        final RtmpRed5VideoConvertorTask task = new RtmpRed5VideoConvertorTask(taskContext);
        task.setUseDataQueue(useDataQueue);
        return task;
    }
    
    @Override
    public void start(FlvPublishListener flvPublishListener) {
        convertService.registerUnCloseableTask((VideoConvertorTask) flvPublishListener);
    }
    
    
    public RtmpRed5VideoConvertor setUseDataQueue(boolean useDataQueue) {
        this.useDataQueue = useDataQueue;
        return this;
    }
}
