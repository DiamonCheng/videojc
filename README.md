# videojc

## Desc

Flash 禁用之后, 网页端对于RTMP与RTSP协议的播放解决方案

### 在Flash禁用以前,通常网页端播放实时视频流使用以下几套方案

1. -[rtsp]->FFMPEG-[rtmp]->NGINX(rtmp-module)-[rtmp]->BROWSER(Flash)
2. -[rtsp]->SRS(ffmpeg)-[rtmp]->BROWSER(Flash)

### 禁用Flash以后,可以使用 webRTC/http-flv,我这里主要使用http-flv,有以下几套方案

3. -[rtsp]->FFMPEG-[rtmp]->NGINX(flv-module)-[http-flv]->BROWSER(flv.js)
4. -[rtsp]->SRS(ffmpeg)-[http-flv]->BROWSER(flv.js)

### 现在问题是,以上方案不能动态添加转换链接, 对于 Windows 十分不友好, 对于 Java 也十分不友好, 所以有了这个

#### 以下是videojc方案

5. \#client方案\# -[rtsp]->videojc(打包为windows服务安装在客户机)-[http-flv]->BROWSER(flv.js)
6. \#server方案\# -[rtsp]->videojc(Jar包运行在服务器)->-[http-flv]->BROWSER(flv.js)

## Feature

1. 一站式部署  
   一个Jar包就能解决网页端对于RTSP与RTMP播放的转换问题
2. 集成ffmpeg与javacv
3. 可以做成服务端,也可以作为客户端
4. 可以动态添加视频流转换,可以在一个TCP连接(WS/HTTP)之中直接做到添加拉流转换+写流
5. 直接可以RTMP推流转换成FLV, 相当于一个直播服务器!
## References

* https://gitee.com/52jian/EasyMedia.git (抄了别人的代码,重构了一下,增加健壮性)
* https://github.com/Red5/

## Usage

* http://localhost:8080/live?source=rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov
* http://localhost:8080/live?source=rtsp://2.35.252.200/stream0
* http://localhost:8080/live?source=rtmp://2.35.253.47:1925/live/livestream
* ws://localhost:8080/live?source=rtmp://2.35.253.47:1925/live/livestream&ffmpeg=true

* http://localhost:8080/live/{taskId}

### 播放器选择

可以在使用vlc,potplayer,或者输入网址 http://bilibili.github.io/flv.js/demo/

## 一些管理功能

```
### add stable
POST http://localhost:8080/livem
Content-Type: application/json

{
"source": "rtsp://2.35.252.200/stream0"
}

### play by id
GET http://localhost:8080/live/9ac86af9be2fc782ba356b7b5b56ccac

### get list
GET http://localhost:8080/livem

### delete stable
DELETE http://localhost:8080/livem/9ac86af9be2fc782ba356b7b5b56ccac

### restart
POST http://localhost:8080/livem/9ac86af9be2fc782ba356b7b5b56ccac/restart
```

## RTMP直播服务器

在 application.properties 中设置 videojc.rtmp-red5.enabled=true  
启用之后,启动服务器之后会监听1935 端口, 默认开放了一个开放的rtmp推流地址 rtmp://localhost:1935/live/{scope}  
推流之后,可以使用播放器播放

1. rtmp://localhost:1935/live/{scope} (FLV in RTMP)
2. http://localhost:8080/live/{scope} (FLV)

并且 http://localhost:8080/livem 接口中能够看到正在推流的地址列表

## TODO

*[x] test...
*[x] addFfmpegSupport
*[x] 添加WS支持
*[x] 添加长时间转换支持(监控进程停止就要重启| 一键重启的功能)
*[ ] 添加hls转换
*[x] 添加 接收RTMP推流转发
*[ ] 尝试优化内存拷贝-- 看起来不需要优化
*[ ] 录制功能?
