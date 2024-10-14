package com.im.caller.page;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.im.caller.R;
import com.im.caller.bean.Server;
import com.im.caller.media.MediaCallback;
import com.im.caller.rtc.PeerService;
import com.im.caller.media.MediaVideoSink;
import com.im.caller.manager.ManagerImpl;
import com.im.caller.utils.PermissionUtil;
import com.im.caller.utils.ToastUtil;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

/**
 *   @desc : 单聊， 1V1 的音频、视频通话
 *   @auth : tyf
 *   @date : 2024-09-30 11:35:34
 */
public class ChatSingleActivity extends AppCompatActivity {

    private static String TAG = ChatSingleActivity.class.getName();

    // 本端和远端视频 ui 组件。在 SurfaceView 中显示视频流
    private SurfaceViewRenderer localView;
    private SurfaceViewRenderer remoteView;

    // 本端和远端视频渲染器，setTarget 函数设置 SurfaceView 来控制要显示的视频流
    private MediaVideoSink localRender;
    private MediaVideoSink remoteRender;

    // WebRTC 管理器单例模式
    private ManagerImpl managerImpl;

    // 是否开启视频
    private boolean videoEnable;

    // 控制和切换本端、远端视频的显示顺序
    private boolean isSwappedFeeds;

    // 用于 OpenGL 渲染
    private EglBase eglBase;


    // 进入单聊回话
    public static void callSingle(Activity activity,String wss, Server[] iceServers,int mediaType, String roomId){
        // ws 连接或失败回调
        Consumer<Void> success = Void -> ChatSingleActivity.openActivity(activity);
        Consumer<Void> fail = Void ->  ToastUtil.showShort(activity,"WS连接失败!");
        // 设置服务器地址以及连接回调函数，然后进行连接
        ManagerImpl.getInstance().setConfigWithCallBack(wss,iceServers,mediaType,roomId,success,fail);
        ManagerImpl.getInstance().connect();
    }


    // 跳转 ChatSingleActivity
    public static void openActivity(Activity activity) {
        Log.d(TAG,"ChatSingleActivity.openActivity");
        Intent intent = new Intent(activity, ChatSingleActivity.class);
        activity.startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 设置 activity 不显示 title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 设置窗口显示
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON // 屏幕常亮
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD // 解锁后显示 activity
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED // 锁屏后显示 activity
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON); // 打开屏幕时启动 activity
        super.onCreate(savedInstanceState);
        // 设置 UI
        setContentView(R.layout.wr_activity_chat_single);
        // 初始化（如果开启视频，初始化本端、远端视频显示组件）
        initVar();
        // 监听 local_view（小窗口视频）的触摸事件，实现悬浮可移动的效果
        initListener();
    }

    // 初始化（如果开启视频，初始化本端、远端视频显示组件）
    private void initVar() {

        Log.d(TAG,"ChatSingleActivity.initVar");

        // 是否开启视频
        videoEnable = ManagerImpl.getInstance().isVideoEnable();

        // 创建 ChatSingleFragment 并填充到 ui 中 wr_container 的容器中进行显示
        // 包含静音、挂断、免提、切换摄像头等按钮
        ChatSingleFragment chatSingleFragment = new ChatSingleFragment();
        replaceFragment(chatSingleFragment, videoEnable);

        // 初始化 EglBase 用于 OpenGL 渲染
        eglBase = EglBase.create();

        // 如果开启视频
        if (videoEnable) {
            // 显示本端视频、远端视频的 ui 组件
            localView = findViewById(R.id.local_view_render);
            remoteView = findViewById(R.id.remote_view_render);
            // 本地图像初始化
            localView.init(eglBase.getEglBaseContext(), null); // 视频里显示 SurfaceView 初始化
            localView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT); // 设置为按比例缩放
            localView.setZOrderMediaOverlay(true); // 设置本端在其他层之上显示
            localView.setMirror(true); // 设置为镜像显示
            localRender = new MediaVideoSink(); // 视频渲染器
            //远端图像初始化
            remoteView.init(eglBase.getEglBaseContext(), null); // 视频里显示 SurfaceView 初始化
            remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED); // 设置为按比例缩放
            remoteView.setMirror(true); // 设置为镜像显示
            remoteRender = new MediaVideoSink(); // 视频渲染器
            // 交换本端、远端视频显示顺序
            setSwappedFeeds(true);
            // 点击本端视频时交换本端和远端视频显示顺序
            localView.setOnClickListener(v -> setSwappedFeeds(!isSwappedFeeds));
        }
        // 初始化 WebRTC 管理器，设置本地、远端流创建时的回调函数（将视频流设置到渲染器中）
        initWebrtcManager();
        // 然后本端加入房间
        startCall();
    }


    // 监听 local_view（小窗口视频）的触摸事件，实现悬浮可移动的效果
    private int previewX, previewY;
    private int moveX, moveY;
    @SuppressLint("ClickableViewAccessibility")
    private void initListener() {
        if (videoEnable) {
            // 设置小视频可以移动
            localView.setOnTouchListener((view, motionEvent) -> {
                switch (motionEvent.getAction()) {
                    // 按下时不断获取触摸的位置，将 previewX、previewY 设置为触摸的位置
                    case MotionEvent.ACTION_DOWN:
                        previewX = (int) motionEvent.getX();
                        previewY = (int) motionEvent.getY();
                        break;
                    // 移动时计算新的位置并更新视图的布局参数，使其移动到新的坐标位置
                    case MotionEvent.ACTION_MOVE:
                        int x = (int) motionEvent.getX();
                        int y = (int) motionEvent.getY();
                        moveX = (int) motionEvent.getX();
                        moveY = (int) motionEvent.getY();
                        // 使用 RelativeLayout.LayoutParams 进行位置调整，并清除对齐规则，以防止其他布局属性影响位置。
                        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) localView.getLayoutParams();
                        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0); // Clears the rule, as there is no removeRule until API 17.
                        lp.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
                        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                        lp.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
                        int left = lp.leftMargin + (x - previewX);
                        int top = lp.topMargin + (y - previewY);
                        lp.leftMargin = left;
                        lp.topMargin = top;
                        view.setLayoutParams(lp);
                        break;
                    // 抬起时，如果没有移动实际上就是单击，此时触发 performClick 点击事件去切换远端、本端显示顺序
                    case MotionEvent.ACTION_UP:
                        if (moveX == 0 && moveY == 0) {
                            view.performClick();
                        }
                        moveX = 0;
                        moveY = 0;
                        break;
                }
                return true;
            });
        }
    }

    // 交换本端和远端视频显示顺序
    // isSwappedFeeds = true： 右上角小屏幕显示自己，全部显示别人
    // isSwappedFeeds = false：右上角小屏幕显示别人，全部显示自己
    private void setSwappedFeeds(boolean isSwappedFeeds) {
        Log.d(TAG,"ChatSingleActivity.setSwappedFeeds isSwappedFeeds："+isSwappedFeeds);
        this.isSwappedFeeds = isSwappedFeeds;
        localRender.setTarget(isSwappedFeeds ? remoteView : localView);
        remoteRender.setTarget(isSwappedFeeds ? localView : remoteView);
    }



    // 初始化 WebRTC 管理器，设置本地、远端流创建时的回调函数（将视频流设置到渲染器中），
    private void initWebrtcManager(){
        // 获取 WebRTC 管理器的单例实例
        managerImpl = ManagerImpl.getInstance();
        // 设置本端、远端流媒体事件回调
        managerImpl.setViewCallback(new MediaCallback() {
            // 设置本地流创建成功的回调函数，将本地流视频添加到视频渲染器中
            @Override
            public void onSetLocalStream(MediaStream stream, String socketId) {
                Log.d(TAG,"ChatSingleActivity.onSetLocalStream");
                // 获取 MediaStream 中第一个视频轨道进行添加
                if (stream.videoTracks.size() > 0) {
                    stream.videoTracks.get(0).addSink(localRender);
                }
                // 获取 MediaStream 中第一个视频轨道进行启用
                if (videoEnable) {
                    stream.videoTracks.get(0).setEnabled(true);
                }
            }
            // 设置远端流创建成功的回调函数，将远端流视频添加到视频渲染器中
            @Override
            public void onAddRemoteStream(MediaStream stream, String socketId) {
                Log.d(TAG,"ChatSingleActivity.onAddRemoteStream");
                // 获取 MediaStream 中第一个视频轨道进行添加
                if (stream.videoTracks.size() > 0) {
                    stream.videoTracks.get(0).addSink(remoteRender);
                }
                // 获取 MediaStream 中第一个视频轨道进行启用
                if (videoEnable) {
                    stream.videoTracks.get(0).setEnabled(true);
                    // 切换为远端优先显示，本端显示在右上角
                    runOnUiThread(() -> setSwappedFeeds(false));
                }
            }
            // 1V1断开、流 remove 时回调这里，释放 Surface 和视频渲染器
            @Override
            public void onCloseWithId(String socketId) {
                Log.d(TAG,"ChatSingleActivity.onCloseWithId");
                runOnUiThread(() -> {
                    disConnect();
                    ChatSingleActivity.this.finish();
                });
            }
        });
    }


    // 然后本端加入房间
    private void startCall() {
        Log.d(TAG,"ChatSingleActivity.startCall");
        // 如果有权限直接键入房间，权限申请通过也会自动加入房间
        if (!PermissionUtil.isNeedRequestPermission(ChatSingleActivity.this)) {
            // 调用管理器加入房间，后续就是 signal 服务器通讯的事情了
            managerImpl.joinRoom(getApplicationContext(), eglBase);
        }
    }


    // 将 fragment 替换 ui 中 wr_container 显示
    // xml 中定义的 wr_container 是个空的，用 Fragment 进行填充
    private void replaceFragment(Fragment fragment, boolean videoEnable) {
        Log.d(TAG,"ChatSingleActivity.replaceFragment");
        Bundle bundle = new Bundle();
        bundle.putBoolean("videoEnable", videoEnable);
        fragment.setArguments(bundle);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.wr_container, fragment)
                .commit();
    }


    // 重写返回键的监听，这样返回键没用了
    // 按了不回退出页面，只有挂断主动调用 finish() 才会退出页面
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG,"ChatSingleActivity.onKeyDown");
        // 如果按下的键是返回键（KeyEvent.KEYCODE_BACK），则返回 true，表示事件已被处理，不再向上抛出。
        // 否则，调用父类的 onKeyDown 方法，处理其他按键事件，并返回其结果。
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
    }


    // 切换前后摄像头
    public void switchCamera() {
        Log.d(TAG,"ChatSingleActivity.switchCamera");
        managerImpl.switchCamera();
    }

    // 挂断（只有挂断才会退出当前 activity）
    public void hangUp() {
        Log.d(TAG,"ChatSingleActivity.hangUp");
        disConnect();
        // 结束当前页面返回主页面或上个页面
        this.finish();
    }

    // 麦克风的启用或者禁用（自己静音）
    public void toggleMic(boolean enable) {
        Log.d(TAG,"ChatSingleActivity.toggleMic");
        managerImpl.toggleMute(enable);
    }

    // 使用扬声器播放（免提模式）
    public void toggleSpeaker(boolean enable) {
        Log.d(TAG,"ChatSingleActivity.toggleSpeaker");
        managerImpl.toggleSpeaker(enable);
    }

    @Override
    protected void onDestroy() {
        disConnect();
        super.onDestroy();
    }

    // 1V1断开、流 remove 时回调这里，释放 Surface 和视频渲染器
    private void disConnect() {
        Log.d(TAG,"ChatSingleActivity.disConnect");
        managerImpl.exitRoom();
        if (localRender != null) {
            localRender.setTarget(null);
            localRender = null;
        }
        if (remoteRender != null) {
            remoteRender.setTarget(null);
            remoteRender = null;
        }

        if (localView != null) {
            localView.release();
            localView = null;
        }
        if (remoteView != null) {
            remoteView.release();
            remoteView = null;
        }
    }


    // 权限都通过则加入房间
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG,"ChatSingleActivity.onRequestPermissionsResult");
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
