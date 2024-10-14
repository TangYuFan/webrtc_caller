package com.im.internet.manager;


import android.app.Activity;

import com.im.caller.bean.Server;
import com.im.caller.media.MediaType;
import com.im.caller.page.ChatGroupActivity;
import com.im.caller.page.ChatSingleActivity;
import com.im.caller.utils.ToastUtil;
import com.im.internet.config.ServerConfig;
import com.im.internet.ws.IMService;
import com.im.internet.ws.IMWebSocket;

import java.util.function.Consumer;

/**
 * @desc : 处理通讯
 * @auth : tyf
 * @date : 2024-10-12 17:09:34
*/
public class ImManager {


    // 即使通讯初始化
    public static Activity activity;
    public static void init(Activity activity,String wss,String slefId){
        // 登录im服务器
        ImManager.activity = activity;
        IMWebSocket.newInstance(wss,slefId);
    }


    // 单聊呼叫
    public static void callSingle(String otherId){

        String wss = ServerConfig.WS_SIGNAL_SERVER.getUri() ;// 信令服务器
        Server[] ice = ServerConfig.ICE_SERVER ; // ice服务器
        String slefId = ServerConfig.USER_ID; // 呼叫方（自己编号）
        String roomId = slefId + "_" + otherId; // 拼接双方的用户编号作为房间号
        int mediaType = MediaType.TYPE_VIDEO;// 仅仅音频通话或者音视频通话 TYPE_VIDEO、TYPE_AUDIO

        // 发送呼叫消息，发送成功则进入单聊页面
        Consumer<Boolean> callback = send ->{
            if(send){
                String ws = wss.replace("{userId}",slefId);// 信令服务器
                ChatSingleActivity.callSingle(activity,ws,ice,mediaType,roomId);// 跳转通话页面
            }
            else{
                ToastUtil.showShort(activity,"发起呼叫失败！");
            }
        };

        // 向好友发起呼叫
        IMService.sendCall(slefId,otherId,roomId,mediaType,callback);

    }


    // 群聊呼叫
    public static void callGroup(String roomId){

        String wss = ServerConfig.WS_SIGNAL_SERVER.getUri() ;// 信令服务器
        Server[] ice = ServerConfig.ICE_SERVER ; // ice服务器
        String slefId = ServerConfig.USER_ID; // 呼叫方（自己编号）

        int mediaType = MediaType.TYPE_MEETING;// 群聊音视频通话
            String ws = wss.replace("{userId}",slefId);// 信令服务器
            ChatGroupActivity.callGroup(activity,ws,ice,mediaType,roomId);// 跳转
    }


}
