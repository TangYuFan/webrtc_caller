package com.im.caller.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {

    private static Toast toast;

    // 显示短时间的 Toast 提示
    public static void showShort(Context context, String message) {
        if (toast != null) {
            toast.cancel();  // 取消上一个 Toast，避免重叠
        }
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    // 显示长时间的 Toast 提示
    public static void showLong(Context context, String message) {
        if (toast != null) {
            toast.cancel();  // 取消上一个 Toast，避免重叠
        }
        toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
    }

    // 立即取消当前显示的 Toast
    public static void cancelToast() {
        if (toast != null) {
            toast.cancel();
        }
    }
}
