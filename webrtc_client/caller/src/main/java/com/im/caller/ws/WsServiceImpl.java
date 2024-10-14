package com.im.caller.ws;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.im.caller.manager.Manager;
import com.im.caller.utils.X509TrustUtil;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.webrtc.IceCandidate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;


/**
 *   @desc : 处理信令服务器 ws 交互，发送和接收的处理
 *   @auth : tyf
 *   @date : 2024-09-25 22:11:27
*/
public class WsServiceImpl implements WsService {

    private final static String TAG = WsServiceImpl.class.getName();

    // 这个就是 ws 消息发送和接收的客户端
    private WebSocketClient mWebSocketClient;

    // 事件处理器 WebRTCManager
    private Manager events;

    //是否连接成功过
    private boolean isOpen;

    public WsServiceImpl(Manager events) {
        this.events = events;
    }

    /**
     *   @desc : 链接服务器
     *   @auth : tyf
     *   @date : 2024-09-25 22:13:04
    */
    @Override
    public void connect(String wss) {
        URI uri;
        try {
            uri = new URI(wss);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        if (mWebSocketClient == null) {
            // 创建 ws 连接
            mWebSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    isOpen = true;
                    events.onWebSocketOpen();
                }
                // 所有服务端来的消息都走这里
                @Override
                public void onMessage(String message) {
                    isOpen = true;
                    Log.d(TAG, message);
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (events != null) {
                        events.onWebSocketOpenFailed(reason);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, ex.toString());
                    if (events != null) {
                        events.onWebSocketOpenFailed(ex.toString());
                    }
                }
            };
        }
        // 如果是 wss 需要使用 tls 以及证书
        if (wss.startsWith("wss")) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                if (sslContext != null) {
                    sslContext.init(null, new TrustManager[]{new X509TrustUtil()}, new SecureRandom());
                }
                SSLSocketFactory factory = null;
                if (sslContext != null) {
                    factory = sslContext.getSocketFactory();
                }

                if (factory != null) {
                    mWebSocketClient.setSocket(factory.createSocket());
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 链接
        mWebSocketClient.connect();
    }


    @Override
    public boolean isOpen() {
        return isOpen;
    }

    public void close() {
        if (mWebSocketClient != null) {
            mWebSocketClient.close();
        }

    }


    //============================需要发送的=====================================
    @Override
    public void joinRoom(String room) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__join");
        Map<String, String> childMap = new HashMap<>();
        childMap.put("room", room);
        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        mWebSocketClient.send(jsonString);
    }

    public void sendAnswer(String socketId, String sdp) {
        Map<String, Object> childMap1 = new HashMap();
        childMap1.put("type", "answer");
        childMap1.put("sdp", sdp);
        HashMap<String, Object> childMap2 = new HashMap();
        childMap2.put("socketId", socketId);
        childMap2.put("sdp", childMap1);
        HashMap<String, Object> map = new HashMap();
        map.put("eventName", "__answer");
        map.put("data", childMap2);
        JSONObject object = new JSONObject(map);
        String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        mWebSocketClient.send(jsonString);
    }


    public void sendOffer(String socketId, String sdp) {
        HashMap<String, Object> childMap1 = new HashMap();
        childMap1.put("type", "offer");
        childMap1.put("sdp", sdp);
        HashMap<String, Object> childMap2 = new HashMap();
        childMap2.put("socketId", socketId);
        childMap2.put("sdp", childMap1);

        HashMap<String, Object> map = new HashMap();
        map.put("eventName", "__offer");
        map.put("data", childMap2);
        JSONObject object = new JSONObject(map);
        String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        mWebSocketClient.send(jsonString);

    }

    public void sendIceCandidate(String socketId, IceCandidate iceCandidate) {
        HashMap<String, Object> childMap = new HashMap();
        childMap.put("id", iceCandidate.sdpMid);
        childMap.put("label", iceCandidate.sdpMLineIndex);
        childMap.put("candidate", iceCandidate.sdp);
        childMap.put("socketId", socketId);
        HashMap<String, Object> map = new HashMap();
        map.put("eventName", "__ice_candidate");
        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        mWebSocketClient.send(jsonString);
    }
    //============================需要发送的=====================================


    //============================需要接收的=====================================
    @Override
    public void handleMessage(String message) {
        Log.d(TAG, "recv-->" + message);
        Map map = JSON.parseObject(message, Map.class);
        String eventName = (String) map.get("eventName");
        if (eventName == null) return;
        if (eventName.equals("_peers")) {
            handleJoinToRoom(map);
        }
        if (eventName.equals("_new_peer")) {
            handleRemoteInRoom(map);
        }
        if (eventName.equals("_ice_candidate")) {
            handleRemoteCandidate(map);
        }
        if (eventName.equals("_remove_peer")) {
            handleRemoteOutRoom(map);
        }
        if (eventName.equals("_offer")) {
            handleOffer(map);
        }
        if (eventName.equals("_answer")) {
            handleAnswer(map);
        }
    }

    // 自己进入房间
    private void handleJoinToRoom(Map map) {
        Map data = (Map) map.get("data");
        JSONArray arr;
        if (data != null) {
            arr = (JSONArray) data.get("connections");
            String js = JSONObject.toJSONString(arr, SerializerFeature.WriteClassName);
            ArrayList<String> connections = (ArrayList<String>) JSONObject.parseArray(js, String.class);
            String myId = (String) data.get("you");
            events.onJoinToRoom(connections, myId);
        }
    }

    // 自己已经在房间，有人进来
    private void handleRemoteInRoom(Map map) {
        Map data = (Map) map.get("data");
        String socketId;
        if (data != null) {
            socketId = (String) data.get("socketId");
            events.onRemoteJoinToRoom(socketId);
        }
    }

    // 处理交换信息
    private void handleRemoteCandidate(Map map) {
        Map data = (Map) map.get("data");
        String socketId;
        if (data != null) {
            socketId = (String) data.get("socketId");
            String sdpMid = (String) data.get("id");
            sdpMid = (null == sdpMid) ? "video" : sdpMid;
            int label = (int) Double.parseDouble(String.valueOf(data.get("label")));
            String candidate = (String) data.get("candidate");
            IceCandidate iceCandidate = new IceCandidate(sdpMid, label, candidate);
            events.onRemoteIceCandidate(socketId, iceCandidate);
        }


    }

    // 有人离开了房间
    private void handleRemoteOutRoom(Map map) {
        Map data = (Map) map.get("data");
        String socketId;
        if (data != null) {
            socketId = (String) data.get("socketId");
            events.onRemoteOutRoom(socketId);
        }

    }

    // 处理Offer
    private void handleOffer(Map map) {
        Map data = (Map) map.get("data");
        Map sdpDic;
        if (data != null) {
            sdpDic = (Map) data.get("sdp");
            String socketId = (String) data.get("socketId");
            String sdp = (String) sdpDic.get("sdp");
            events.onReceiveOffer(socketId, sdp);
        }

    }

    // 处理Answer
    private void handleAnswer(Map map) {
        Map data = (Map) map.get("data");
        Map sdpDic;
        if (data != null) {
            sdpDic = (Map) data.get("sdp");
            String socketId = (String) data.get("socketId");
            String sdp = (String) sdpDic.get("sdp");
            events.onReceiverAnswer(socketId, sdp);
        }

    }
    //============================需要接收的=====================================



}
