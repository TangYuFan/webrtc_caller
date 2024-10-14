package com.im.caller.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.core.util.Consumer;

import com.im.caller.media.MediaCallback;
import com.im.caller.media.MediaType;
import com.im.caller.bean.Server;
import com.im.caller.rtc.PeerService;
import com.im.caller.bean.Callback;
import com.im.caller.ws.WsService;
import com.im.caller.ws.WsServiceImpl;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;

import java.util.ArrayList;
import java.util.List;

/**
 * @desc : 控制信令和各种操作
 * @auth : tyf
 * @date : 2024-10-10 13:54:25
*/
public class ManagerImpl implements Manager {

    private final static String TAG = ManagerImpl.class.getName();

    // 单例
    private Handler handler = new Handler(Looper.getMainLooper());

    private static ManagerImpl wrManagerImpl = new ManagerImpl();
    public static ManagerImpl getInstance() {
        return wrManagerImpl;
    }

    // 服务器地址
    private String wss;
    private Server[] ice;

    // 连接ws成功或失败的回调函数
    private Callback callback;

    // websocket、webrtc
    private WsService wsService;
    private PeerService peerService;

    // 业务参数
    private String roomId;
    private int mediaType;
    private boolean videoEnable;
    public String getRoomId() { return roomId; }
    public int getMediaType() { return mediaType; }
    public boolean isVideoEnable() {return videoEnable; }


    // init address
    public void setConfigWithCallBack(String wss, Server[] iceServers,int mediaType, String roomId, Consumer<Void> success, Consumer<Void> fail) {
        this.wss = wss;
        this.ice = iceServers;
        this.mediaType = mediaType;
        this.videoEnable = (mediaType != MediaType.TYPE_AUDIO); // 除了1v1音频通话之外其他都是开启视频的
        this.roomId = roomId;
        this.callback = new Callback(success,fail);
    }

    // connect
    public void connect() {
        if (wsService == null) {
            // 初始化 ws 客户端，并调用其连接函数，内部会调用下面的 onWebSocketOpen 函数回调回来
            wsService = new WsServiceImpl(this);
            wsService.connect(wss);
            // 初始化 peer 工具
            peerService = new PeerService(wsService, ice);
        } else {
            // 正在通话中
            wsService.close();
            wsService = null;
            peerService = null;
        }

    }


    public void setViewCallback(MediaCallback viewCallback) {
        if (peerService != null) {
            peerService.setViewCallback(viewCallback);
        }
    }

    //===================================控制功能==============================================
    public void joinRoom(Context context, EglBase eglBase) {
        if (peerService != null) {
            peerService.initContext(context, eglBase);
        }
        if (wsService != null) {
            wsService.joinRoom(roomId);
        }
    }

    public void switchCamera() {
        if (peerService != null) {
            peerService.switchCamera();
        }
    }

    public void toggleMute(boolean enable) {
        if (peerService != null) {
            peerService.toggleMute(enable);
        }
    }

    public void toggleSpeaker(boolean enable) {
        if (peerService != null) {
            peerService.toggleSpeaker(enable);
        }
    }

    public void exitRoom() {
        if (peerService != null) {
            wsService = null;
            peerService.exitRoom();
        }
    }

    // ==================================信令回调===============================================
    @Override
    public void onWebSocketOpen() {
        handler.post(() -> {
            if (callback != null) {
                callback.onSuccess();
            }
        });

    }

    @Override
    public void onWebSocketOpenFailed(String msg) {
        handler.post(() -> {
            if (wsService != null && !wsService.isOpen()) {
                callback.onFailed(msg);
            } else {
                if (peerService != null) {
                    peerService.exitRoom();
                }
            }
        });

    }

    @Override
    public void onJoinToRoom(ArrayList<String> connections, String myId) {
        handler.post(() -> {
            if (peerService != null) {
                peerService.onJoinToRoom(connections, myId, videoEnable, mediaType);
                if (mediaType == MediaType.TYPE_VIDEO || mediaType == MediaType.TYPE_MEETING) {
                    toggleSpeaker(true);
                }
            }
        });

    }

    @Override
    public void onRemoteJoinToRoom(String socketId) {
        handler.post(() -> {
            if (peerService != null) {
                peerService.onRemoteJoinToRoom(socketId);
            }
        });

    }


    @Override
    public void onRemoteIceCandidate(String socketId, IceCandidate iceCandidate) {
        handler.post(() -> {
            if (peerService != null) {
                peerService.onRemoteIceCandidate(socketId, iceCandidate);
            }
        });

    }

    @Override
    public void onRemoteIceCandidateRemove(String socketId, List<IceCandidate> iceCandidates) {
        handler.post(() -> {
            if (peerService != null) {
                peerService.onRemoteIceCandidateRemove(socketId, iceCandidates);
            }
        });
    }

    @Override
    public void onRemoteOutRoom(String socketId) {
        handler.post(() -> {
            if (peerService != null) {
                peerService.onRemoteOutRoom(socketId);
            }
        });

    }

    @Override
    public void onReceiveOffer(String socketId, String sdp) {
        handler.post(() -> {
            if (peerService != null) {
                peerService.onReceiveOffer(socketId, sdp);
            }
        });

    }

    @Override
    public void onReceiverAnswer(String socketId, String sdp) {
        handler.post(() -> {
            if (peerService != null) {
                peerService.onReceiverAnswer(socketId, sdp);
            }
        });
    }


}
