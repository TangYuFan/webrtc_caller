package com.im.caller.utils;

import java.util.Random;

public class RandomUtil {

    private static Random random = new Random();

    // 生成随机的6位数字用户编号
    public static String generateUserId() {
        int userNumber = 100 + random.nextInt(900);
        return String.valueOf(userNumber);
    }

}
