package com.alacritic.timetracker.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.alacritic.timetracker.R;
import com.alacritic.timetracker.domain.TimeRecord;
import com.alacritic.timetracker.util.TimeAndDurationService;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CheckInCheckOutWidgetProvider extends AppWidgetProvider {
    public static final String INTENT_ACTION_TOGGLE_CHECKIN_CHECKOUT = "nl.timetracker.intent.action.toggle_checkin_checkout";
    public static final String INTENT_ACTION_UPDATE_WIDGET = "nl.timetracker.intent.action.update_widget";
    private final String LOG_TAG = this.getClass().getSimpleName();
    private final SimpleDateFormat checkDateFormat = new SimpleDateFormat("EEEE dd-MM");
    private final SimpleDateFormat checkTimeFormat = new SimpleDateFormat("HH:mm:ss");

    public static PendingIntent getUpdateWidgetIntent(Context context) {
        Intent intent = new Intent(INTENT_ACTION_UPDATE_WIDGET);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(LOG_TAG, "onUpdate");

        PendingIntent updateIntent = getCheckinCheckoutIntent(context);

        boolean checkedIn = TimeAndDurationService.isCheckedIn();
        int iconDrawableId;
        String text;
        if (checkedIn) {
            iconDrawableId = R.drawable.ic_widget_pause;
            text = context.getString(R.string.checkout);
        } else {
            iconDrawableId = R.drawable.ic_widget_play;
            text = context.getString(R.string.checkin);
        }

        for (int appWidgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_checkin_checkout);
            remoteViews.setOnClickPendingIntent(R.id.checkinCheckoutWidgetIconImageView, updateIntent);
            remoteViews.setImageViewResource(R.id.checkinCheckoutWidgetIconImageView, iconDrawableId);
            remoteViews.setTextViewText(R.id.checkinCheckoutWidgetTextView, text);
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    private PendingIntent getCheckinCheckoutIntent(Context context) {
        Intent intent = new Intent(INTENT_ACTION_TOGGLE_CHECKIN_CHECKOUT);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d(LOG_TAG, "Received intent:" + intent.toString());

        if (intent.getAction().equals(INTENT_ACTION_TOGGLE_CHECKIN_CHECKOUT)) {
            toggleCheck(context);
            forceUpdateOfWidget(context);
        } else if (intent.getAction().equals(INTENT_ACTION_UPDATE_WIDGET)) {
            forceUpdateOfWidget(context);
        }
    }

    private void forceUpdateOfWidget(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), CheckInCheckOutWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void toggleCheck(Context context) {
        String message;
        if (TimeAndDurationService.isCheckedIn()) {
            TimeRecord checkOutTimeStamp = TimeAndDurationService.checkOut();
            Date checkOutTime = checkOutTimeStamp.getCheckOut().getTime();
            message = context.getString(R.string.checked_out_at, checkDateFormat.format(checkOutTime), checkTimeFormat.format(checkOutTime));
        } else {
            TimeRecord checkIn = TimeAndDurationService.checkIn();
            Date checkInTime = checkIn.getCheckIn().getTime();
            message = context.getString(R.string.checked_in_at, checkDateFormat.format(checkInTime), checkTimeFormat.format(checkInTime));
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
