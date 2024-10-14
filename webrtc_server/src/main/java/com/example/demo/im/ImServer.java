package com.example.demo.im;


import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *   @desc : im 服务器
 *   @auth : tyf
 *   @date : 2024-10-12 16:49:30
*/
@ServerEndpoint("/im/{userId}") // 客户端URI访问的路径
@Component
public class ImServer {

    // 缓存所有链接 ws 的客户端
    private static List<Session> sockets = new ArrayList<>();

    // 缓存用户id 和客户端链接映射
    private static ConcurrentHashMap<String, Session> sessionMap1 = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Session, String> sessionMap2 = new ConcurrentHashMap<>();
    


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
            System.out.println("客户端链接："+userId);
            sockets.add(session);
            sessionMap1.put(userId,session);
            sessionMap2.put(session,userId);
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
            System.out.println("客户端断开："+userId);
            sockets.remove(session);
            sessionMap1.remove(userId);
            sessionMap2.remove(session);
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
            System.out.println("客户端消息："+message);
            try {
                handleMessage(message,session);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        });
    }


    /**
     *   @desc : 处理客户端上行的数据
     *   @auth : tyf
     *   @date : 2024-09-25 15:59:15
     */
    private void handleMessage(String message, Session session) throws IOException {

        System.out.println("---------------------------------------");

        // 客户端消息
        JSONObject recv = JSONObject.parseObject(message);
        String srcId = recv.getString("srcId") ;
        String dstId = recv.getString("dstId");
        String type = recv.getString("type");
        JSONObject data = recv.getJSONObject("data");

        // 直接透传消息
        Session dstSession = sessionMap1.get(dstId);
        System.out.println("IM消息 srcId："+srcId+"，dstId："+dstId+"，type："+type+"，data："+data+"，目标回话："+dstSession);
        if(dstSession!=null){
            dstSession.getAsyncRemote().sendText(message);
        }

    }




}
