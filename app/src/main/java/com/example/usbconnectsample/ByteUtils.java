package com.example.usbconnectsample;

import java.io.UnsupportedEncodingException;

/**
 * 字节转换类，koltin 没有该方法
 */
public class ByteUtils {

    /**
     * 将字符串转换成字节
     */
    public static byte[] getBytes(String str) {
        try {
            return str.getBytes("gb2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

}
