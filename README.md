# videojc

## References

https://gitee.com/52jian/EasyMedia.git

## Usage

* http://localhost:8080/live?source=rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov
* http://localhost:8080/live?source=rtsp://2.35.252.200/stream0
* http://localhost:8080/live?source=rtmp://2.35.253.47:1925/live/livestream
* ws://localhost:8080/live?source=rtmp://2.35.253.47:1925/live/livestream&ffmpeg=true

```
### add stable
POST http://localhost:8080/livem
Content-Type: application/json

{
"source": "rtsp://2.35.252.200/stream0"
}

### get list
GET http://localhost:8080/livem

### delete stable
DELETE http://localhost:8080/livem/9ac86af9be2fc782ba356b7b5b56ccac

### restart
POST http://localhost:8080/livem/9ac86af9be2fc782ba356b7b5b56ccac/restart
```

## TODO

*[x] test...
*[x] addFfmpegSupport
*[x] 添加WS支持
*[x] 添加长时间转换支持(监控进程停止就要重启| 一键重启的功能)
*[ ] 添加hls转换
*[ ] 添加 接收RTMP推流转发
*[ ] 尝试优化内存拷贝-- 看起来不需要优化
*[ ] 录制功能?
