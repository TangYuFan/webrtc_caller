package com.example.demo.webrtc;


import com.google.gson.Gson;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 *   @desc : webrtc 信令服务器处理
 *   @auth : tyf
 *   @date : 2024-10-12 16:47:49
*/
@ServerEndpoint("/ws/{userId}") // 客户端URI访问的路径
@Component
public class SignallingServer {

    // 缓存所有链接 ws 的客户端
    private static List<Session> sockets = new ArrayList<>();

    // 保存每个房间里面所有客户端链接
    // key = 房间号
    // value=客户端ws集合
    private static ConcurrentHashMap<String, List<Session>> rooms = new ConcurrentHashMap<>();

    // 保存每个客户端链接所在的房间号
    // key=客户端ws
    // value=房间号
    private static ConcurrentHashMap<String, String> roomList = new ConcurrentHashMap<>();

    // 缓存用户id 和客户端链接映射
    private static ConcurrentHashMap<String, Session> userIds = new ConcurrentHashMap<>();


    // 使用单线程处理所有客户端请求
    public static final ExecutorService runner = Executors.newSingleThreadExecutor();


    /**
     *   @desc : 客户端建立连接
     *   @auth : tyf
     *   @date : 2024-09-25 15:45:47
    */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        runner.submit(()->{
            System.out.println("---------------------------------------");
            // 添加到会话集合
            addSocket(session,userId);
            System.out.println("客户端链接 userId="+userId+",sessionId="+session.getId());
        });
    }


    /**
     *   @desc : 客户端链接断开
     *   @auth : tyf
     *   @date : 2024-09-25 15:46:28
    */
    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {

        runner.submit(()->{
            System.out.println("---------------------------------------");
            System.out.println("客户端断开 userId="+userId+",sessionId="+session.getId());
            // 当前客户端所在的房间号
            String room = roomList.get(session.getId());
            List<Session> curRoom;
            if (room != null) {
                // 当前客户端所在的房间里面的所有客户端，给他们广播有人退出房间的消息
                curRoom = rooms.get(room);
                // 变量房间里面的每个人
                System.out.println("所在房间："+room+"，当前在线人数："+curRoom.size());
                for (Session aCurRoom : curRoom) {
                    // 跳过当前断开的用户
                    if (aCurRoom.getId().equals(session.getId())) {
                        continue;
                    }
                    // 有人退出房间的消息
                    SignallingData send = new SignallingData();
                    send.setEventName("_remove_peer");
                    Map<String, Object> map = new HashMap<>();
                    map.put("socketId", session.getId());
                    send.setData(map);
                    // 给这个人发送当前用户退出的消息
                    aCurRoom.getAsyncRemote().sendText(new Gson().toJson(send));
                    System.out.println("通知"+aCurRoom.getId()+"："+send);
                }

            }
            // 删除客户端链接
            removeSocket(session);

        });

    }


    /**
     *   @desc : 接收到客户端上行的消息
     *   @auth : tyf
     *   @date : 2024-09-25 15:58:30
    */
    @OnMessage
    public void onMessage(String message, Session session) {
        runner.submit(()->{
            // 处理客户端上行数据
            if (message.contains("eventName")) {
                handleMessage(message, session);
            } else {
                System.out.println("接收到来自" + session.getId() + "的新消息：" + message);
            }
        });
    }


    /**
     *   @desc : 处理客户端上行的数据
     *   @auth : tyf
     *   @date : 2024-09-25 15:59:15
    */
    private void handleMessage(String message, Session session) {
        System.out.println("---------------------------------------");
        Gson gson = new Gson();
        SignallingData signallingData = gson.fromJson(message, SignallingData.class);
        switch (signallingData.getEventName()) {
            // 用户请求加入房间
            case "__join":
                join(signallingData.getData(), session);
                break;
            // A 将 ice 信息发送给 B
            // B 将 ice 信息发送给 A
            case "__ice_candidate":
                iceCandidate(signallingData.getData(), session);
                break;
            // A 将 sdp 信息发送给 B
            case "__offer":
                offer(signallingData.getData(), session);
                break;
            // B 将 sdp 信息发送给 A
            case "__answer":
                answer(signallingData.getData(), session);
                break;
            default:
                break;
        }

    }


    /**
     *   @desc : 客户端断开时，删除客户端链接相关的东西
     *   @auth : tyf
     *   @date : 2024-09-25 16:00:45
    */
    private void removeSocket(Session session) {

        String room = roomList.get(session.getId());
        // 删除session
        sockets.remove(session);
        // 从房间列表删除
        rooms.get(room).remove(session);
        // 从sessionid 对应房间的列表删除
        roomList.remove(session.getId());

    }


    /**
     *   @desc : 客户端链接时，添加到缓存集合
     *   @auth : tyf
     *   @date : 2024-09-25 16:01:16
    */
    private void addSocket(Session session,String userId) {
        if (sockets.indexOf(session) == -1) {
            sockets.add(session);
        }
        userIds.put(userId,session);
    }

    /**
     *   @desc : 获取某个客户端链接
     *   @auth : tyf
     *   @date : 2024-09-25 16:01:28
    */
    private Session getSocket(String socketId) {
        if (sockets.size() > 0) {
            for (int i = 0; i < sockets.size(); i++) {
                if (socketId.equals(sockets.get(i).getId())) {
                    return sockets.get(i);
                }
            }
        }
        return null;
    }


    /**
     *   @desc : 客户端申请加入房间
     *   @auth : tyf
     *   @date : 2024-09-25 16:01:41
    */
    private void join(Map<String, Object> data, Session socket) {

        System.out.println("处理 join 请求：sessionId="+socket.getId());

        // 获得房间号
        String room = data.get("room") == null ? "__default" : data.get("room").toString();

        // 获取对应房间的客户端列表
        List<Session> curRoom = rooms.get(room);

        // 如果首次有人加入，则新建一个房间
        if (curRoom == null) {
            curRoom = new ArrayList<>();
        }

        System.out.println("目标房间："+room+"，当前在线人数："+curRoom.size());


        // 保存房间里面已有的其他客户端，需要返回给当前申请进入房间的客户端
        List<String> ids = new ArrayList<>();

        // 遍历当前房间里面每个人，广播有人加入房间的消息
        for (Session curSocket : curRoom) {
            // 跳过自己
            if (socket.getId().equals(curSocket.getId())) {
                continue;
            }
            // 缓存房间里面已有的客户端
            ids.add(curSocket.getId());
            // 广播有新客户端进入房间的消息
            SignallingData send = new SignallingData();
            send.setEventName("_new_peer");
            Map<String, Object> map = new HashMap<>();
            map.put("socketId", socket.getId());
            send.setData(map);
            curSocket.getAsyncRemote().sendText(new Gson().toJson(send));
            System.out.println("通知房间内用户"+curSocket.getId()+"："+send);
        }

        // 缓存当前进入房间的人
        curRoom.add(socket);
        rooms.put(room, curRoom);

        // 缓存当前客户端进入的房间号
        roomList.put(socket.getId(), room);

        // 将房间里面已经有的人返回给当前申请进入房间的客户端
        SignallingData send = new SignallingData();
        send.setEventName("_peers");
        Map<String, Object> map = new HashMap<>();
        String[] connections = new String[ids.size()];
        ids.toArray(connections);
        map.put("connections", connections);
        map.put("you", socket.getId());
        send.setData(map);
        socket.getAsyncRemote().sendText(new Gson().toJson(send));

        System.out.println("通知"+socket.getId()+"："+send);

    }


    /**
     *   @desc : 将当前客户端的 ice 信息转发给目标用户
     *   @auth : tyf
     *   @date : 2024-09-25 16:20:54
    */
    private void iceCandidate(Map<String, Object> data, Session socket) {

        System.out.println("处理 iceCandidate 请求：sessionId="+socket.getId());

        // 获取目标客户端
        Session session = getSocket(data.get("socketId").toString());
        if (session == null) {
            return;
        }
        // 将自己的 ice 信息发送给目标客户端
        SignallingData send = new SignallingData();
        send.setEventName("_ice_candidate");
        Map<String, Object> map = data;
        map.put("id", data.get("id"));
        map.put("label", data.get("label"));
        map.put("candidate", data.get("candidate"));
        map.put("socketId", socket.getId()); // 自己的客户端编号
        send.setData(map);
        session.getAsyncRemote().sendText(new Gson().toJson(send));

        System.out.println("通知目标用户"+socket.getId()+"："+send);
    }


    /**
     *   @desc : 将当前客户端的 sdp 信息转发给目标用户
     *   @auth : tyf
     *   @date : 2024-09-25 16:20:54
     */
    private void offer(Map<String, Object> data, Session socket) {

        System.out.println("处理 offer 请求：sessionId="+socket.getId());


        // 获取目标客户端
        Session session = getSocket(data.get("socketId").toString());
        if (session == null) {
            return;
        }
        SignallingData send = new SignallingData();
        send.setEventName("_offer");
        // 将自己的 sdp 信息发送给目标客户端
        Map<String, Object> map = data;
        map.put("sdp", data.get("sdp"));
        map.put("socketId", socket.getId());
        send.setData(map);
        session.getAsyncRemote().sendText(new Gson().toJson(send));

        System.out.println("通知目标用户"+socket.getId()+"："+send);

    }

    /**
     *   @desc : 将当前客户端的 sdp 信息转发给目标用户
     *   @auth : tyf
     *   @date : 2024-09-25 16:20:54
     */
    private void answer(Map<String, Object> data, Session socket) {

        System.out.println("处理 answer 请求：sessionId="+socket.getId() +"，目标");

        // 获取目标客户端
        Session session = getSocket(data.get("socketId").toString());
        if (session == null) {
            return;
        }

        SignallingData send = new SignallingData();
        send.setEventName("_answer");
        // 将自己的 sdp 信息恢复给目标客户端
        Map<String, Object> map = data;
        map.put("sdp", data.get("sdp"));
        map.put("socketId", socket.getId());
        send.setData(map);
        session.getAsyncRemote().sendText(new Gson().toJson(send));

        System.out.println("通知目标用户"+socket.getId()+"："+send);

    }


}