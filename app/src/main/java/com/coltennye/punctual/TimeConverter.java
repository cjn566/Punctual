package com.coltennye.punctual;

import java.util.Locale;

public class TimeConverter {
    public static int toMinutes(int hour, int minute){
        return (hour * 60) + minute;
    }

    public static int getHour(int minutes){
        return (minutes / 60);
    }

    public static int getMinute(int minutes){
        return (minutes % 60);
    }

    public static String timeRemainingString(int minutes){
        if(minutes >= 60) {
            int mins = getMinute(minutes);
            if(mins == 0)
                return String.format(Locale.US, "%dh", getHour(minutes));
            else
                return String.format(Locale.US, "%dh %dm", getHour(minutes), mins);
        }
        else
            return String.format(Locale.US, "%dm", minutes);
    }



}
