package com.openclassrooms.go4lunch.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.openclassrooms.go4lunch.receivers.AlarmBroadcastReceiver;
import java.util.Calendar;

/**
 * Class is used to handle the AlarmManager and activate/deactivate an alarm.
 */
public class AlarmHandler {

    private final Context context;
    private final AlarmManager alarmManager;

    public AlarmHandler(Context context) {
        this.context = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Initializes an AlarmManager, according to the user-defined hour.
     */
    public void startAlarm(Calendar calendarAlarm) {
        Intent intent = new Intent(context, AlarmBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, 0);

        // If hour of the day has already passed, schedule for next day
        if (Calendar.getInstance().getTimeInMillis() - calendarAlarm.getTimeInMillis() > 0)
            calendarAlarm.add(Calendar.DAY_OF_YEAR, 1);

        // Configure alarm
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendarAlarm.getTimeInMillis(), pendingIntent);
    }

    /**
     * Cancels an AlarmManager, previously enabled by user.
     */
    public void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, 0);
        alarmManager.cancel(pendingIntent);
    }
}
