package com.im.caller.page;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.fragment.app.FragmentManager;

import com.im.caller.R;
import com.im.caller.bean.Server;
import com.im.caller.media.MediaCallback;
import com.im.caller.rtc.PeerService;
import com.im.caller.media.MediaVideoSink;
import com.im.caller.manager.ManagerImpl;
import com.im.caller.bean.User;
import com.im.caller.utils.PermissionUtil;
import com.im.caller.utils.ToastUtil;
import com.im.caller.utils.WidthUtil;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *   @desc : 群聊界面 （支持 9 路同时通信）
 *   @auth : tyf
 *   @date : 2024-10-08 10:48:43
*/
public class ChatGroupActivity extends AppCompatActivity{

    private static String TAG = ChatGroupActivity.class.getName();

    // WebRTC 管理器单例模式
    private ManagerImpl managerImpl;

    // 用于 OpenGL 渲染
    private EglBase eglBase;

    // 保存房间内所有用户的视频 ui 组件。在 SurfaceView 中显示视频流
    private Map<String, SurfaceViewRenderer> videoViews = new HashMap<>();

    // 保存房间内所有用户的视频渲染器，setTarget 函数设置 SurfaceView 来控制要显示的视频流
    private Map<String, MediaVideoSink> sinks = new HashMap<>();

    // 保存房间内所有用户的信息
    private List<User> infos = new ArrayList<>();

    // 显示多录视频的 UI 空间
    private FrameLayout wrVideoView;

    // 屏幕的宽，后面动态计算多路视频的显示宽度
    private int mScreenWidth;

    // 本地视频流
    private VideoTrack localVideoTrack;



    // 进入群聊回话
    public static void callGroup(Activity activity,String wss, Server[] iceServers,int mediaType, String roomId){

        // ws 连接或失败回调
        Consumer<Void> success = Void -> ChatGroupActivity.openActivity(activity);
        Consumer<Void> fail = Void -> ToastUtil.showShort(activity,"WS连接失败!");

        // 设置服务器地址以及连接回调函数，然后进行连接
        ManagerImpl.getInstance().setConfigWithCallBack(wss,iceServers,mediaType,roomId,success,fail);
        ManagerImpl.getInstance().connect();
    }

    public static void openActivity(Activity activity) {
        Log.d(TAG,"ChatRoomActivity.openActivity");
        Intent intent = new Intent(activity, ChatGroupActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"ChatRoomActivity.onCreate");
        // 设置 activity 不显示 title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 设置窗口显示
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON // 屏幕常亮
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD // 解锁后显示 activity
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED // 锁屏后显示 activity
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON); // 打开屏幕时启动 activity
        super.onCreate(savedInstanceState);
        // 设置主 UI
        setContentView(R.layout.wr_activity_chat_room);
        // 设置显示多路视频的 UI
        wrVideoView = findViewById(R.id.wr_video_view);
        // 用于 OpenGL 渲染
        eglBase = EglBase.create();
        // 设置多路视频 UI 宽度等于屏幕宽度，也就是全屏
        initVar();
        // 控制按钮 UI 组件替换（包含静音、挂断、免提、切换摄像头、开启或停止摄像头等按钮）
        replaceFragment();
        // 初始化 webrtc 管理器，并设置本端、远端流媒体事件回调
        initWebrtcManager();
        // 初始化 webrtc 管理器，并开启通话（自己进入聊天）
        startCall();
    }


    // 设置多路视频 UI 宽度等于屏幕宽度，也就是全屏
    private void initVar() {
        Log.d(TAG,"ChatRoomActivity.initVar");
        // 获取屏幕宽度
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (manager != null) {
            mScreenWidth = manager.getDefaultDisplay().getWidth();
        }
        // 设置多路视频 UI 宽度等于屏幕宽度，也就是全屏
        wrVideoView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mScreenWidth));
    }

    // 初始化 webrtc 管理器，并开启通话（自己进入聊天）
    private void startCall() {
        Log.d(TAG,"ChatRoomActivity.startCall");
        if (!PermissionUtil.isNeedRequestPermission(ChatGroupActivity.this)) {
            managerImpl.joinRoom(getApplicationContext(), eglBase);
        }
    }

    // 设置本端、远端流媒体事件回调
    private void initWebrtcManager(){
        Log.d(TAG,"ChatRoomActivity.initWebrtcManager");
        managerImpl = ManagerImpl.getInstance();
        // 设置回调
        managerImpl.setViewCallback(new MediaCallback() {
            // 设置本地流创建成功的回调函数，将本地流视频添加到视频渲染器中
            @Override
            public void onSetLocalStream(MediaStream stream, String userId) {
                List<VideoTrack> videoTracks = stream.videoTracks;
                if (videoTracks.size() > 0) {
                    localVideoTrack = videoTracks.get(0);
                }
                // 新增视频UI
                runOnUiThread(() -> {
                    addView(userId, stream);
                });
            }
            // 设置远端流创建成功的回调函数，将远端流视频添加到视频渲染器中
            @Override
            public void onAddRemoteStream(MediaStream stream, String userId) {
                // 新增视频UI
                runOnUiThread(() -> {
                    addView(userId, stream);
                });
            }
            // 某个用户离开、流媒体 remove 时回调这里，释放 Surface 和视频渲染器
            @Override
            public void onCloseWithId(String userId) {
                // 移除视频UI
                runOnUiThread(() -> {
                    removeView(userId);
                });
            }
        });
    }

    // 在 UI 上创建视频渲染器（SurfaceViewRenderer）并将其与相应的视频流绑定
    private void addView(String id, MediaStream stream) {
        Log.d(TAG,"ChatRoomActivity.addView");
        // 创建视频显示UI
        SurfaceViewRenderer renderer = new SurfaceViewRenderer(ChatGroupActivity.this);
        renderer.init(eglBase.getEglBaseContext(), null);
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        renderer.setMirror(true);
        // 创建视频渲染器
        MediaVideoSink sink = new MediaVideoSink();
        sink.setTarget(renderer);
        if (stream.videoTracks.size() > 0) {
            stream.videoTracks.get(0).addSink(sink);
        }
        // 缓存到 SurfaceView 列表
        videoViews.put(id, renderer);
        // 缓存到 ProxyVideoSink 列表
        sinks.put(id, sink);
        // 缓存到 user 列表
        infos.add(new User(id));
        // SurfaceView 添加到 UI 上
        wrVideoView.addView(renderer);
        // 遍历 user 列表，为每个 user 动态调整 SurfaceView 的布局位置
        int size = infos.size();
        for (int i = 0; i < size; i++) {
            User user = infos.get(i);
            SurfaceViewRenderer renderer1 = videoViews.get(user.getId());
            if (renderer1 != null) {
                // 传入当前用户数、屏幕总宽度，动态计算当前 SurfaceView 宽高以及位置
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.height = WidthUtil.getWidth(mScreenWidth,size);
                layoutParams.width = WidthUtil.getWidth(mScreenWidth,size);
                layoutParams.leftMargin = WidthUtil.getX(mScreenWidth,size, i);
                layoutParams.topMargin = WidthUtil.getY(mScreenWidth,size, i);
                renderer1.setLayoutParams(layoutParams);
            }
        }
    }


    // 从 UI 中移除指定用户的视频渲染器，并释放相应的资源。
    private void removeView(String userId) {
        Log.d(TAG,"ChatRoomActivity.removeView");
        // 移除 ProxyVideoSink
        MediaVideoSink sink = sinks.get(userId);
        // 移除 SurfaceView
        SurfaceViewRenderer renderer = videoViews.get(userId);
        if (sink != null) {
            sink.setTarget(null);
        }
        if (renderer != null) {
            renderer.release();
        }
        // 移除缓存
        sinks.remove(userId);
        videoViews.remove(userId);
        infos.remove(new User(userId));
        wrVideoView.removeView(renderer);
        // 遍历 user 列表，为每个 user 动态调整 SurfaceView 的布局位置
        int size = infos.size();
        for (int i = 0; i < infos.size(); i++) {
            User user = infos.get(i);
            SurfaceViewRenderer renderer1 = videoViews.get(user.getId());
            if (renderer1 != null) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.height = WidthUtil.getWidth(mScreenWidth,size);
                layoutParams.width = WidthUtil.getWidth(mScreenWidth,size);
                layoutParams.leftMargin = WidthUtil.getX(mScreenWidth,size, i);
                layoutParams.topMargin = WidthUtil.getY(mScreenWidth,size, i);
                renderer1.setLayoutParams(layoutParams);
            }
        }
    }

    // 重写返回键的监听，这样返回键没用了（屏蔽返回键）
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG,"ChatRoomActivity.onKeyDown");
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"ChatRoomActivity.onDestroy");
        exit();
        super.onDestroy();
    }

    // 控制按钮 UI 组件替换（包含静音、挂断、免提、切换摄像头、开启或停止摄像头等按钮）
    private void replaceFragment() {
        Log.d(TAG,"ChatRoomActivity.replaceFragment");
        ChatGroupFragment fragment = new ChatGroupFragment();
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.wr_container, fragment)
                .commit();
    }

    // 切换摄像头
    public void switchCamera() {
        Log.d(TAG,"ChatRoomActivity.switchCamera");
        managerImpl.switchCamera();
    }

    // 挂断
    public void hangUp() {
        Log.d(TAG,"ChatRoomActivity.hangUp");
        exit();
        this.finish();
    }

    // 静音
    public void toggleMic(boolean enable) {
        Log.d(TAG,"ChatRoomActivity.toggleMic");
        managerImpl.toggleMute(enable);
    }

    // 免提
    public void toggleSpeaker(boolean enable) {
        Log.d(TAG,"ChatRoomActivity.toggleSpeaker");
        managerImpl.toggleSpeaker(enable);
    }

    // 打开关闭摄像头
    public void toggleCamera(boolean enableCamera) {
        Log.d(TAG,"ChatRoomActivity.toggleCamera");
        if (localVideoTrack != null) {
            localVideoTrack.setEnabled(enableCamera);
        }
    }

    // 退出房间（释放资源）
    private void exit() {
        Log.d(TAG,"ChatRoomActivity.exit");
        managerImpl.exitRoom();
        for (SurfaceViewRenderer renderer : videoViews.values()) {
            renderer.release();
        }
        for (MediaVideoSink sink : sinks.values()) {
            sink.setTarget(null);
        }
        videoViews.clear();
        sinks.clear();
        infos.clear();
    }


    // 权限都通过则加入房间
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG,"ChatRoomActivity.onRequestPermissionsResult");
        for (int i = 0; i < permissions.length; i++) {
            Log.i(PeerService.TAG, "[Permission] " + permissions[i] + " is " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                finish();
                break;
            }
        }
        // 权限都通过则加入房间
        managerImpl.joinRoom(getApplicationContext(), eglBase);
    }
}
