package com.xhy.utils;

import com.xhy.core.enums.LogLevel;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author xhy
 * @date 2021/5/27 23:10
 */
public class LogUtils {
    /**
     * 默认可以打印
     */
    private static boolean enable = true;
    /**
     * 默认打印所有级别日志
     */
    private static LogLevel minLevel = LogLevel.ALL;
    /**
     * 日期显示格式
     */
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");

    /**
     * <pre>
     * 设置是否开启打印
     * </pre>
     *
     * @param enable
     * @author ygr
     * @date 2018年4月9日 上午9:54:46
     */
    public static void setEnable(boolean enable) {
        LogUtils.enable = enable;
    }

    /**
     * <pre>
     * 设置日志打印级别
     * </pre>
     *
     * @param level
     * @author ygr
     * @date 2018年4月9日 上午9:42:09
     */
    public static void setLogLevel(LogLevel level) {
        LogUtils.minLevel = level;
    }

    /**
     * <pre>
     * 打印消息级别日志
     * </pre>
     *
     * @param msg 待打印消息
     * @author ygr
     * @date 2018年4月9日 上午9:42:59
     */
    public static void info(String msg) {
        finalPrint(LogLevel.INFO, msg);
    }

    /**
     * <pre>
     * 打印警告级别日志
     * </pre>
     *
     * @param msg 待打印消息
     * @author ygr
     * @date 2018年4月9日 上午9:42:59
     */
    public static void warn(String msg) {
        finalPrint(LogLevel.WARN, msg);
    }

    /**
     * <pre>
     * 打印错误级别日志
     * </pre>
     *
     * @param msg 待打印消息
     * @author ygr
     * @date 2018年4月9日 上午9:42:59
     */
    public static void error(String msg) {
        finalPrint(LogLevel.ERROR, msg);
    }

    /**
     * <pre>
     * 最终打印日志
     * </pre>
     *
     * @param logLevel 日志级别
     * @param msg      待打印消息
     * @author ygr
     * @date 2018年4月9日 上午9:50:21
     */
    private static void finalPrint(LogLevel logLevel, String msg) {
        if (!enable) {
            return;
        }
        if (logLevel.isAllow(minLevel)) {
            System.out.printf("%s %s %s\n", formatCurrentTime(), logLevel.getName(), msg);
        }
    }

    /**
     * <pre>
     * 获取当前时间
     * </pre>
     *
     * @return
     * @author ygr
     * @date 2018年4月9日 上午9:49:00
     */
    private static String formatCurrentTime() {
        return sdf.format(new Date());
    }
}
