package com.im.caller.bean;

import androidx.core.util.Consumer;

/**
 *   @desc : 设置 ws 链接成功或者失败的回调函数
 *   @auth : tyf
 *   @date : 2024-10-08 14:22:57
*/
public class Callback {

    // 成功或失败
    private Consumer<Void> success;
    private Consumer<Void> fail;

    public Callback(Consumer<Void> success, Consumer<Void> fail) {
        this.success = success;
        this.fail = fail;
    }

    public void onSuccess(){
        success.accept(null);
    }

    public void onFailed(String msg){
        fail.accept(null);
    }
}
