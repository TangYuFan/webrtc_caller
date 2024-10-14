package com.im.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import com.im.internet.config.ServerConfig;
import com.im.caller.utils.PermissionUtil;
import com.im.caller.utils.ToastUtil;
import com.im.internet.manager.ImManager;
import com.im.caller.utils.AlertDialogUtil;
import com.webrtc.app.databinding.ActivityMainBinding;

import java.util.function.Consumer;


/**
 * @desc : 程序主入口
 * @auth : tyf
 * @date : 2024-10-09 14:13:23
*/
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 显示用户编号
        binding.labelUserId.append(ServerConfig.USER_ID);

        // 即使通讯初始化
        ImManager.init(this,ServerConfig.WS_IM_SERVER.getUri(),ServerConfig.USER_ID);

        // 权限申请
        binding.buttonRequestPermission.setOnClickListener(l->{
            if(!PermissionUtil.isNeedRequestPermission(MainActivity.this)) {
                ToastUtil.showShort(MainActivity.this,"已经拥有权限!");
            }
        });

        // 进入单聊，输入对方的用户编号
        binding.buttonPrivateChat.setOnClickListener(l->{
            // 获取到用户编号则发起呼叫
            Consumer<String> call = ImManager::callSingle;
            AlertDialogUtil.alertInputDialog(this,"发起呼叫","输入好友编号",call,null);
        });

        // 进入群聊，输入群聊房间编号
        binding.buttonGroupChat.setOnClickListener(l->{
            // 获取到房间号则进入房间
            Consumer<String> call = ImManager::callGroup;
            AlertDialogUtil.alertInputDialog(this,"进入群聊","输入房间编号",call,null);
        });

    }

}