# 录屏

这个仓库做为一个module，可以依赖它进行录制视频和音频最后生成MP4文件。

功能
--------
1. 录制音视频
2. 暂停录制，恢复录制
3. 配置视频分辨率

相关API
--------
1. MediaProjection（需要API>=21）
2. MediaCodec，MediaMuxer
3. OpenGL

使用下面代码进入用来测试的Activity进行测试
通过Intent调用
```java
Intent intent = new Intent(context,RecordExampleActivity.class);
startActivity(intent);
```
通过Binder调用
```java
Intent intent = new Intent(context,RecordBinderExampleActivity.class);
startActivity(intent);
```
