package com.im.internet.config;


import com.im.caller.bean.Server;
import com.im.caller.utils.RandomUtil;

/**
 * @desc : 配置服务端地址
 * @auth : tyf
 * @date : 2024-10-10 09:39:12
*/
public class ServerConfig {


    // ws im服务器地址（长连接用户客户端消息透传）
    public static Server WS_IM_SERVER = new Server("ws://172.16.8.223:5000/im/{userId}");

    // ws 信令服务器地址（短链接仅在音视频通话期间传输信令）
    public static Server WS_SIGNAL_SERVER = new Server("ws://172.16.8.223:5000/ws/{userId}");

    // turn and stun
    public static Server[] ICE_SERVER = {
            new Server("stun:172.16.8.223:3478?transport=udp"),
            new Server("turn:172.16.8.223:3478?transport=udp","wzp","123456"),
            new Server("turn:172.16.8.223:3478?transport=tcp","wzp","123456"),
    };

    // 用户编号
    public static String USER_ID = RandomUtil.generateUserId();

}
