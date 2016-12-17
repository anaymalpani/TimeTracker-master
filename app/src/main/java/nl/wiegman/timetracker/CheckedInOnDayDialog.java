package nl.wiegman.timetracker;

import android.app.AlertDialog;
import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;

import nl.wiegman.timetracker.util.TimeAndDurationService;

public class CheckedInOnDayDialog {

    private static final SimpleDateFormat checkDateFormat = new SimpleDateFormat("EEEE dd-MM");
    private static final SimpleDateFormat checkTimeFormat = new SimpleDateFormat("HH:mm:ss");

    private final Context context;

    public CheckedInOnDayDialog(Context context) {
        this.context = context;
    }

    public void showCheckedInDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        Date checkInTimestamp = TimeAndDurationService.getCheckIn().getCheckIn().getTime();
        String message = context.getString(R.string.checked_in_at, checkDateFormat.format(checkInTimestamp), checkTimeFormat.format(checkInTimestamp));
        builder.setMessage(message)
                .setNeutralButton(android.R.string.ok, new DismissOnClickListener())
                .show();
    }

}
