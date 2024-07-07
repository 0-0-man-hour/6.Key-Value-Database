package com.zeromh.kvdb.server.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
    static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");


    public static String getNowDate() {
        return formatter.format(new Date());
    }

    public static long getTimeStamp() {
        Date date = new Date();
        return date.getTime();
    }

    public static String getDateTimeString(long timeStamp) {
        return formatter.format(new Date(timeStamp));
    }
}
