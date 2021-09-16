package com.dc.red5slim.server.stream;

import org.red5.server.api.scope.IScope;

/***
 * 实现这个用于对接
 * @author Diamon.Cheng
 * @date 2021/9/14
 */
public interface FlvPublishListenerFactory {
    FlvPublishListener create(IScope scope, String publishedName);
    
    void start(FlvPublishListener flvPublishListener);
}
