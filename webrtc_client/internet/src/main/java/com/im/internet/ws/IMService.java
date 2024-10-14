package com.im.internet.ws;


import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.im.caller.bean.Server;
import com.im.caller.media.MediaType;
import com.im.caller.page.ChatSingleActivity;
import com.im.caller.utils.AlertDialogUtil;
import com.im.caller.utils.ToastUtil;
import com.im.internet.config.ServerConfig;
import com.im.internet.manager.ImManager;

import java.util.function.Consumer;

/**
 * @desc : websocket 处理
 * @auth : tyf
 * @date : 2024-10-14 10:20:42
*/
public class IMService {

    private static String TAG = IMService.class.getName();

    // 向好友发送呼叫消息
    public static void sendCall(String selfId, String otherId,String roomId,int mediaType,Consumer<Boolean> callback){

        Log.d(TAG,"向用户发起呼叫："+otherId);

        // 拼接双方的用户编号作为房间号
        JSONObject data = new JSONObject();
        data.put("roomId",roomId);
        data.put("mediaType",mediaType);

        // 发送消息
        IMWebSocket.sendData(selfId,otherId,IMType.SEND_CALL.name(),data,callback);

    }


    // 接收好友的呼叫消息
    public static void recvCall(String selfId, String otherId,JSONObject data){

        Log.d(TAG,"接收到用户呼叫："+otherId);

        String wss = ServerConfig.WS_SIGNAL_SERVER.getUri() ;// 信令服务器
        Server[] ice = ServerConfig.ICE_SERVER ; // ice服务器
        String slefId = ServerConfig.USER_ID; // 呼叫方（自己编号）

        String roomId = data.getString("roomId");
        int mediaType = data.getIntValue("mediaType");// 仅仅音频通话或者音视频通话 TYPE_VIDEO、TYPE_AUDIO

        // 接收或者拒绝
        Consumer<Boolean> callback = accept ->{
            // 接收通话
            if(accept){
                String ws = wss.replace("{userId}",slefId);// 信令服务器
                ChatSingleActivity.callSingle(ImManager.activity,ws,ice,mediaType,roomId);// 跳转通话页面
            }
        };

        // 弹出呼叫窗
        ImManager.activity.runOnUiThread(()-> AlertDialogUtil.showCallerDialog(ImManager.activity,otherId,callback));


    }


}
