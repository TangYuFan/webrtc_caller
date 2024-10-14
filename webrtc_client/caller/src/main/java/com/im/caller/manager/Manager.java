package com.im.caller.manager;

import org.webrtc.IceCandidate;

import java.util.ArrayList;
import java.util.List;

/**
 *   @desc : 和信令服务器 ws 的交互以及业务处理
 *   @auth : tyf
 *   @date : 2024-10-08 14:24:14
*/
public interface Manager {

    // webSocket连接成功
    void onWebSocketOpen();

    // webSocket连接失败
    void onWebSocketOpenFailed(String msg);

    // 进入房间
    void onJoinToRoom(ArrayList<String> connections, String myId);

    // 有新人进入房间
    void onRemoteJoinToRoom(String socketId);

    void onRemoteIceCandidate(String socketId, IceCandidate iceCandidate);

    void onRemoteIceCandidateRemove(String socketId, List<IceCandidate> iceCandidates);


    void onRemoteOutRoom(String socketId);

    void onReceiveOffer(String socketId, String sdp);

    void onReceiverAnswer(String socketId, String sdp);

}
