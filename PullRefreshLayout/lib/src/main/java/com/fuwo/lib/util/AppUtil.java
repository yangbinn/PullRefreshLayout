package com.fuwo.lib.util;

import android.content.res.Resources;
import android.util.DisplayMetrics;

public class AppUtil {

    public static final String TAG = "AppUtil";

    /**
     * 获取屏幕尺寸
     */
    public static DisplayMetrics getScreenSize() {
        return Resources.getSystem().getDisplayMetrics();
    }

    /**
     * 获取屏幕密度
     */
    public static float getDensity() {
        return getScreenSize().density;
    }

    /**
     * 获取屏幕高
     */
    public static int getHeight() {
        return getScreenSize().heightPixels;
    }

    /**
     * 获取屏幕宽
     */
    public static int getWidth() {
        return getScreenSize().widthPixels;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(float value) {
        return (int) (value * getDensity() + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(float value) {
        return (int) (value / getDensity() + 0.5f);
    }

}
