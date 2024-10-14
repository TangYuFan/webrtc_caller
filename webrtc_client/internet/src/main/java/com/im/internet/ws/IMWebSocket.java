package com.im.internet.ws;


import android.util.Log;
import com.alibaba.fastjson.JSONObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.util.function.Consumer;


/**
 * @desc : im 客户端
 * @auth : tyf
 * @date : 2024-10-14 09:58:37
*/
public class IMWebSocket extends WebSocketClient {

    private static String TAG = IMWebSocket.class.getName();

    public IMWebSocket(URI serverUri) {
        super(serverUri);
    }

    private static IMWebSocket instance;
    public static void newInstance(String server, String userId) {
        Log.d(TAG,"初始化IM客户端");
        URI uri = null;
        try {
            uri = new URI(server.replace("{userId}",String.valueOf(userId)));
            instance = new IMWebSocket(uri);
            instance.connect();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d(TAG,"服务器连接："+this.uri);
    }

    @Override
    public void onMessage(String message) {

        JSONObject recv = JSONObject.parseObject(message);
        String srcId = recv.getString("srcId") ;
        String dstId = recv.getString("dstId");
        String type = recv.getString("type");
        JSONObject data = recv.getJSONObject("data");

        Log.d(TAG,"[IM recv]"+recv.toJSONString());
        // 处理接收
        recvData(srcId,dstId,type,data);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG,"服务器断开："+this.uri);
    }

    @Override
    public void onError(Exception ex) {
        Log.d(TAG,"服务器错误："+this.uri+"，ex："+ex.getCause()+"|"+ex.getMessage());
    }


    // 通用的发送接口
    public static void sendData(String srcId, String dstId, String type, JSONObject data,Consumer<Boolean> callback){
        if(instance!=null&&instance.isOpen()){
            try {
                JSONObject send = new JSONObject();
                send.put("srcId",srcId);
                send.put("dstId",dstId);
                send.put("type",type);
                send.put("data",data);
                instance.send(send.toJSONString());
                Log.d(TAG,"[IM Send]"+send.toJSONString());
                callback.accept(true);// 发送成功
            }
            catch (Exception e){
                e.printStackTrace();
                callback.accept(false);// 发送失败
            }
        }else{
            Log.d(TAG,"IM服务器未连接,发送失败!");
            callback.accept(false);// 发送失败
        }
    }


    // 通用的接收接口
    public static void recvData(String srcId, String dstId, String type, JSONObject data){

        // 语音呼叫
        if(IMType.SEND_CALL.name().equals(type)){
            IMService.recvCall(dstId,srcId,data);
        }


    }

}
