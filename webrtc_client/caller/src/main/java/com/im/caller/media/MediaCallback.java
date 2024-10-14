package com.im.caller.media;

import org.webrtc.MediaStream;


/**
 *   @desc : 接收远端、本端流媒体事件
 *   @auth : tyf
 *   @date : 2024-10-08 11:30:21
*/
public interface MediaCallback {

    // 本地视频流设置：用户打开摄像头时，本地视频流准备好，会调用 onSetLocalStream()，将流传递给界面进行渲染。
    void onSetLocalStream(MediaStream stream, String socketId);

    // 远程用户加入：远程用户加入房间时，远程媒体流到达，onAddRemoteStream() 被调用，界面更新，显示该远程用户的视频。
    void onAddRemoteStream(MediaStream stream, String socketId);

    // 用户退出房间：某个用户离开房间或连接断开时，onCloseWithId() 被调用，界面根据 socketId 关闭对应的音视频显示。
    void onCloseWithId(String socketId);

}
