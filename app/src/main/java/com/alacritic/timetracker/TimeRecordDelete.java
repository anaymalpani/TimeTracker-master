package com.alacritic.timetracker;

import android.app.Activity;
import android.app.PendingIntent;
import android.util.Log;

import com.alacritic.timetracker.domain.TimeRecord;
import com.alacritic.timetracker.widget.CheckInCheckOutWidgetProvider;

public class TimeRecordDelete {
    private static final String LOG_TAG = TimeRecordDelete.class.getSimpleName();

    public static void run(TimeRecord recordToDelete, Activity activity) {
        recordToDelete.delete();

        try {
            CheckInCheckOutWidgetProvider.getUpdateWidgetIntent(activity).send();
        } catch (PendingIntent.CanceledException e) {
            Log.e(LOG_TAG, "Unable to update widget: " + e.getMessage());
        }
    }
}
