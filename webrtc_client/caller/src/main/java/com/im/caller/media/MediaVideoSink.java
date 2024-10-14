package com.im.caller.media;

import org.webrtc.Logging;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

/**
 *   @desc : 继承 webrtc 的 VideoSink 接口，用于接收视频帧并传给 webrtc 的 VideoSink
 *   @auth : tyf
 *   @date : 2024-10-08 09:53:03
*/
public class MediaVideoSink implements VideoSink {

    private static final String TAG = MediaVideoSink.class.getName();

    // 目标 SurfaceViewRenderer 对象
    private VideoSink target;

    @Override
    synchronized public void onFrame(VideoFrame frame) {
        if (target == null) {
            Logging.d(TAG, "Dropping frame in proxy because target is null.");
            return;
        }
        target.onFrame(frame);
    }

    // 设置目标 SurfaceViewRenderer 对象
    synchronized public void setTarget(VideoSink target) {
        this.target = target;
    }

}