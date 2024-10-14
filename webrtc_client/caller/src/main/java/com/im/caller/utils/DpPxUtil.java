package com.im.caller.utils;

import android.content.Context;

/**
 *   @desc : dp、px转换
 *   @auth : tyf
 *   @date : 2024-10-08 10:47:15
*/
public class DpPxUtil {

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
