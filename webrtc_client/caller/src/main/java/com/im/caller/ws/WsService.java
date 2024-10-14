package com.im.caller.ws;

import org.webrtc.IceCandidate;

/**
 * @desc : 处理 ws 信令服务器的发送和接收
 * @auth : tyf
 * @date : 2024-10-10 14:30:59
*/
public interface WsService {


    void connect(String wss);

    boolean isOpen();

    void close();

    //处理回调消息
    void handleMessage(String message);

    // 加入房间
    void joinRoom(String room);

    void sendIceCandidate(String socketId, IceCandidate iceCandidate);

    void sendAnswer(String socketId, String sdp);

    void sendOffer(String socketId, String sdp);
}
