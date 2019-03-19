package com.coltennye.punctual;

import android.content.Context;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeConverter {

    private static java.text.DateFormat timeFormatter;
    private static boolean is24hr;

    public static boolean is24hr() {
        return is24hr;
    }

    static void init(Context context){
        is24hr = DateFormat.is24HourFormat(context);
        timeFormatter = java.text.DateFormat.getTimeInstance(
                java.text.DateFormat.SHORT
        );
    }

    public static int toMinutes(int hour, int minute){
        return (hour * 60) + minute;
    }

    public static int getHour(int minutes){
        int hour = (minutes / 60);
        if ((hour == 0) && !is24hr) hour = 12;
        return hour;
    }

    public static int getMinute(int minutes){
        return (minutes % 60);
    }

    public static String timeOfDayString(Date date){
        return timeFormatter.format(date);
    }

    public static String timeRemainingString(int minutes){
        if(minutes >= 60) {
            int mins = getMinute(minutes);
            int hours = (minutes / 60);
            if(mins == 0)
                return String.format(Locale.US, "%dh", hours);
            else
                return String.format(Locale.US, "%dh %dm", hours, mins);
        }
        else
            return String.format(Locale.US, "%dm", minutes);
    }

}
