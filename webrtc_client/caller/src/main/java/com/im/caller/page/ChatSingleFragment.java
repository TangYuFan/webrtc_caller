package com.im.caller.page;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.im.caller.R;
import com.im.caller.utils.DpPxUtil;

/**
 *   @desc : 1V1 语音和视频通话，（静音、挂断、免提、切换摄像头等按钮）页面按钮处理
 *   @auth : tyf
 *   @date : 2024-09-30 14:11:02
 */
public class ChatSingleFragment extends Fragment {

    // 父页面
    private ChatSingleActivity activity;

    // 主 ui 容器
    public View rootView;

    // 静音、挂断、切换摄像头、挂断按钮
    private TextView wr_switch_mute; // 静音（切换麦克风状态）
    private TextView wr_switch_hang_up; //  挂断
    private TextView wr_switch_camera; // 切换前后摄像头
    private TextView wr_hand_free; // 免提（切换扬声器状态）

    // 记录麦克风状态（静音）
    private boolean enableMic = true;
    // 记录扬声器状态（免提）
    private boolean enableSpeaker = false;
    // 记录是否开启视频
    private boolean videoEnable;

    // fragment 添加到 activity 时触发
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (ChatSingleActivity) getActivity();
        Bundle bundle = getArguments();
        if (bundle != null) {
            // 获取是否开启视频
            videoEnable = bundle.getBoolean("videoEnable");
        }
    }


    // fragment 初始化时启动，初始化这些 ui 组件
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            // 主 view
            rootView = inflater.inflate(R.layout.wr_fragment_room_control_single, container, false);
            // 静音
            wr_switch_mute = rootView.findViewById(R.id.wr_switch_mute);
            // 挂断按钮
            wr_switch_hang_up = rootView.findViewById(R.id.wr_switch_hang_up);
            // 切换摄像头按钮
            wr_switch_camera = rootView.findViewById(R.id.wr_switch_camera);
            // 免提
            wr_hand_free = rootView.findViewById(R.id.wr_hand_free);
            // 如果开启视频聊天（显示切换摄像头按钮）
            if (videoEnable) {
                wr_switch_camera.setVisibility(View.VISIBLE);
            }
            // 如果未开启视频聊天（不显示切换摄像头）
            else {
                wr_switch_camera.setVisibility(View.GONE);
            }
            // 给静音、挂断、切换摄像头、挂断按钮等按钮设置点击事件
            initListener();
        }
        return rootView;
    }

    // 给静音、挂断、切换摄像头、挂断按钮等按钮设置点击事件
    private void initListener() {
        // 是否开启麦克风
        wr_switch_mute.setOnClickListener(v -> {
            enableMic = !enableMic;
            if (enableMic) {
                Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.webrtc_mute_default);
                if (drawable != null) {
                    drawable.setBounds(0, 0, DpPxUtil.dip2px(activity, 60), DpPxUtil.dip2px(activity, 60));
                }
                wr_switch_mute.setCompoundDrawables(null, drawable, null, null);
            } else {
                Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.webrtc_mute);
                if (drawable != null) {
                    drawable.setBounds(0, 0, DpPxUtil.dip2px(activity, 60), DpPxUtil.dip2px(activity, 60));
                }
                wr_switch_mute.setCompoundDrawables(null, drawable, null, null);
            }
            activity.toggleMic(enableMic);
        });
        // 挂断
        wr_switch_hang_up.setOnClickListener(v -> activity.hangUp());
        // 切换摄像头
        wr_switch_camera.setOnClickListener(v -> activity.switchCamera());
        // 是否开启扬声器
        wr_hand_free.setOnClickListener(v -> {
            enableSpeaker = !enableSpeaker;
            if (enableSpeaker) {
                Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.webrtc_hands_free);
                if (drawable != null) {
                    drawable.setBounds(0, 0, DpPxUtil.dip2px(activity, 60), DpPxUtil.dip2px(activity, 60));
                }
                wr_hand_free.setCompoundDrawables(null, drawable, null, null);
            } else {
                Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.webrtc_hands_free_default);
                if (drawable != null) {
                    drawable.setBounds(0, 0, DpPxUtil.dip2px(activity, 60), DpPxUtil.dip2px(activity, 60));
                }
                wr_hand_free.setCompoundDrawables(null, drawable, null, null);
            }
            activity.toggleSpeaker(enableSpeaker);
        });
    }

}
