package com.example.trafficpolice;

import java.text.SimpleDateFormat;
import java.util.Date;

public  class DateUtil {

    public  String getTimeId() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
        return sdf.format(date);
    }

    public static String getNowTime() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        return sdf.format(date);
    }

}
