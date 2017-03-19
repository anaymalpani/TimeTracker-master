package com.alacritic.timetracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.alacritic.timetracker.domain.TimeRecord;
import com.alacritic.timetracker.util.TimeAndDurationService;

import org.apache.commons.lang3.time.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * Handles a request from the user to delete time records in a specific period
 */
public class DeleteTimeRecordsInPeriod {
    private final String LOG_TAG = this.getClass().getSimpleName();

    private final Activity activity;

    private final TimeRecordsDeletedListener timeRecordsDeletedListener;

    private final Calendar from;
    private final Calendar to;

    /**
     * Constructor
     */
    public DeleteTimeRecordsInPeriod(Activity activity, Calendar from, Calendar to, TimeRecordsDeletedListener timeRecordsDeletedListener) {
        this.activity = activity;
        this.timeRecordsDeletedListener = timeRecordsDeletedListener;
        this.from = from;
        this.to = to;
    }

    public void handleUserRequestToDeleteRecordsInPeriod() {
        List<TimeRecord> timeRecordsInPeriod = TimeAndDurationService.getTimeRecordsBetween(from, to);

        if (timeRecordsInPeriod.size() == 0) {
            if (TimeAndDurationService.isCheckedIn()) {
                new CheckedInOnDayDialog(activity).showCheckedInDialog();
            } else {
                nothingToDelete();
            }
        } else {
            DialogInterface.OnClickListener dialogClickListener = new DeleteConfirmationDialogOnClickListener();

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            String message = getDeleteConfirmationMessage();
            builder.setMessage(message)
                    .setTitle(R.string.confirm)
                    .setPositiveButton(android.R.string.yes, dialogClickListener)
                    .setNegativeButton(android.R.string.no, new DismissOnClickListener())
                    .show();
        }
    }

    private void nothingToDelete() {
        int message;
        if (DateUtils.isSameDay(from, to)) {
            message = R.string.no_records_to_delete_on_day;
        } else {
            message = R.string.no_records_to_delete_in_period;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message)
                .setNeutralButton(android.R.string.ok, new DismissOnClickListener())
                .show();
    }

    private String getDeleteConfirmationMessage() {
        String message;
        if (DateUtils.isSameDay(from, to)) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd-MM-yyyy");
            message = activity.getString(R.string.delete_all_on_day, sdf.format(from.getTime()));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd-MM-yyyy HH:mm:ss");
            message = activity.getString(R.string.delete_all_in_period, sdf.format(from.getTime()), sdf.format(to.getTime()));
        }
        return message;
    }

    public interface TimeRecordsDeletedListener {
        void recordDeleted();
    }

    private class DeleteConfirmationDialogOnClickListener implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    List<TimeRecord> timeRecordsOnDay = TimeAndDurationService.getTimeRecordsBetween(from, to);
                    for (TimeRecord recordToDelete : timeRecordsOnDay) {
                        TimeRecordDelete.run(recordToDelete, activity);
                    }
                    dialog.dismiss();
                    if (timeRecordsDeletedListener != null) {
                        timeRecordsDeletedListener.recordDeleted();
                    }
                    break;
                default:
                    dialog.dismiss();
                    break;
            }
        }
    }
}
