package com.alacritic.timetracker.export_import;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

/**
 * Template for exporting data to a single file.
 * Opens a progress dialog while exporting and
 * shows a toast with the location of the exported file when the export is completed.
 */
abstract class AbstractExport extends AsyncTask<Void, Void, File> {
    final String LOG_TAG = this.getClass().getSimpleName();

    final Context context;

    private ProgressDialog dialog;

    AbstractExport(Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        dialog = new ProgressDialog(context);
        dialog.setMessage(context.getString(getResourceIdMessageExportInProgress()));
        dialog.show();
    }

    @Override
    public File doInBackground(Void... voids) {
        return doExport();
    }

    @Override
    protected void onPostExecute(File exportedFile) {
        closeProgressDialog();

        if (exportedFile != null && exportedFile.exists() && exportedFile.canRead()) {
            showFileLocationToast(exportedFile);
            openExportedFile(exportedFile);
        }
    }

    protected abstract int getResourceIdMessageExportInProgress();

    protected abstract int getResourceIdMessageExportCompleted();

    protected abstract File doExport();

    void openExportedFile(File exportedFile) {
        // No implementation by default, but this can be overridden by subclasses
    }

    private void showFileLocationToast(File file) {
        int duration = Toast.LENGTH_LONG;
        try {
            Toast toast = Toast.makeText(context, context.getString(getResourceIdMessageExportCompleted(), file.getCanonicalPath()), duration);
            toast.show();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private void closeProgressDialog() {
        if (dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }

    /**
     * Checks if external storage is available for read and write
     */
    boolean isExternalStorageWritable() {
        boolean result = false;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            result = true;
        }
        return result;
    }

}
