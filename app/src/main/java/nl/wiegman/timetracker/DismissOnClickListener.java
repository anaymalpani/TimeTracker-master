package nl.wiegman.timetracker;

import android.content.DialogInterface;

/**
 * OnClickListener that dismisses the dialog when receiving an onClick event
 */
public class DismissOnClickListener implements DialogInterface.OnClickListener {

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        dialogInterface.dismiss();
    }
}