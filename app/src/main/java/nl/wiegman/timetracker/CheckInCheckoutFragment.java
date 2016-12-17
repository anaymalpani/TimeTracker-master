package nl.wiegman.timetracker;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import nl.wiegman.timetracker.export_import.XmlExport;
import nl.wiegman.timetracker.export_import.XmlImport;
import nl.wiegman.timetracker.period.Day;
import nl.wiegman.timetracker.period.Period;
import nl.wiegman.timetracker.period.Week;
import nl.wiegman.timetracker.util.Formatting;
import nl.wiegman.timetracker.util.FragmentHelper;
import nl.wiegman.timetracker.util.PeriodicRunnableExecutor;
import nl.wiegman.timetracker.util.TimeAndDurationService;
import nl.wiegman.timetracker.widget.CheckInCheckOutWidgetProvider;

public class CheckInCheckoutFragment extends Fragment {
    private final String LOG_TAG = this.getClass().getSimpleName();

    private static final int MENU_ITEM_WEEK_OVERVIEW_ID = 0;
    private static final int MENU_ITEM_MONTH_OVERVIEW_ID = 1;
    private static final int MENU_ITEM_EXPORT_XML = 2;

    private static final int MENU_ITEM_IMPORT_XML = 3;

    public static final int REQUEST_CODE_SELECT_BACKUP_FILE = 100;
    private static final int REQUEST_CODE_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 200;

    @BindView(R.id.todaysTotalTextView)
    TextView todaysTotalTextView;

    @BindView(R.id.thisWeeksTotalTextView)
    TextView thisWeeksTotalTextView;

    @BindView(R.id.pausePlayImageView)
    ImageView pausePlayImageView;

    private PeriodicRunnableExecutor checkedInTimeUpdaterExecutor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_checkin_checkout, container, false);
        ButterKnife.bind(this, rootView);

        setHasOptionsMenu(true);

        new IconUpdater().execute();
        new Updater().execute();

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_ITEM_MONTH_OVERVIEW_ID, 1, R.string.action_month_overview);
        menu.add(0, MENU_ITEM_WEEK_OVERVIEW_ID, 2, R.string.action_week_overview);
        menu.add(0, MENU_ITEM_EXPORT_XML, 3, R.string.create_backup);
        menu.add(0, MENU_ITEM_IMPORT_XML, 4, R.string.restore_backup);
    }

    @Override
    public void onStart() {
        super.onStart();
        activateCheckedInTimeUpdater();
    }

    @Override
    public void onPause() {
        super.onPause();
        deactivateCheckedInTimeUpdater();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        deactivateCheckedInTimeUpdater();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == MENU_ITEM_WEEK_OVERVIEW_ID) {
            showWeekOverview();
        } else if (id == MENU_ITEM_MONTH_OVERVIEW_ID) {
            showMonthOverview();
        } else if (id == MENU_ITEM_EXPORT_XML) {
            createBackup();
        } else if (id == MENU_ITEM_IMPORT_XML) {
            startBackupImport();
        }
        return super.onOptionsItemSelected(item);
    }

    private void startBackupImport() {
        askUserForFileaccess();
    }

    private void letTheUserSelectABackupFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_SELECT_BACKUP_FILE);
    }

    private void askUserForFileaccess() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

             requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

        } else {
            letTheUserSelectABackupFile();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_CODE_SELECT_BACKUP_FILE == requestCode) {
            if (data != null) {
                new XmlImport(getActivity()).execute(data.getData());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    letTheUserSelectABackupFile();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    private void createBackup() {
        new XmlExport(getActivity()).execute();
    }

    private void showWeekOverview() {
        WeeksOverviewFragment fragment = WeeksOverviewFragment.newInstance(Calendar.getInstance().get(Calendar.YEAR));
        FragmentHelper.showFragment(getActivity(), fragment);
    }

    private void showMonthOverview() {
        MonthsOverviewFragment fragment = MonthsOverviewFragment.newInstance(Calendar.getInstance().get(Calendar.YEAR));
        FragmentHelper.showFragment(getActivity(), fragment);
    }

    @OnClick(R.id.thisWeeksTotalTextView)
    public void thisWeeksTotalOnClick(View view) {
        Calendar today = Calendar.getInstance();
        Period period = new Week(today);
        DaysInPeriodFragment fragment = DaysInPeriodFragment.newInstance(period);
        FragmentHelper.showFragment(getActivity(), fragment);
    }

    @OnClick(R.id.todaysTotalTextView)
    public void todaysTotalOnClick(View view) {
        Calendar day = Calendar.getInstance();
        DayDetailsHelper dayDetailsHelper = new DayDetailsHelper(getActivity());
        dayDetailsHelper.showDetailsOrTimeRecordsOfDay(day);
    }

    @OnClick(R.id.pausePlayImageView)
    public void pausePlayOnClick(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        if (TimeAndDurationService.isCheckedIn()) {
            checkOut();
        } else {
            checkIn();
        }
    }

    private void checkIn() {
        TimeAndDurationService.checkIn();
        animateCheckInCheckOutButton(R.drawable.ic_av_pause_circle_outline_blue);
        updateWidget();
    }

    private void checkOut() {
        TimeAndDurationService.checkOut();
        animateCheckInCheckOutButton(R.drawable.ic_av_play_circle_outline_blue);
        updateWidget();
    }

    private void animateCheckInCheckOutButton(int newImageResourceId) {
        pausePlayImageView.setImageResource(newImageResourceId);
        YoYo.with(Techniques.Landing).playOn(pausePlayImageView);
    }


    private void updateWidget() {
        try {
            CheckInCheckOutWidgetProvider.getUpdateWidgetIntent(getActivity()).send();
        } catch (PendingIntent.CanceledException e) {
            Log.e(LOG_TAG, "Unable to update widget: " + e.getMessage());
        }
    }

    private void activateCheckedInTimeUpdater() {
        checkedInTimeUpdaterExecutor = new PeriodicRunnableExecutor(1000, new CheckedInTimeUpdater());
        checkedInTimeUpdaterExecutor.start();
    }

    private void deactivateCheckedInTimeUpdater() {
        if (checkedInTimeUpdaterExecutor != null) {
            checkedInTimeUpdaterExecutor.stop();
            checkedInTimeUpdaterExecutor = null;
        }
    }

    private class CheckedInTimeUpdater implements Runnable {
        @Override
        public void run() {
            new Updater().execute();
        }
    }

    private class Updater extends AsyncTask<Void, Void, String[]> {
        @Override
        protected String[] doInBackground(Void... voids) {
            Calendar now = Calendar.getInstance();

            Day today = new Day(now);
            long dayTotal = today.getBillableDuration();

            Week thisWeek = new Week(now);
            long weekTotal = thisWeek.getBillableDuration();

            return new String[] {
                                    Formatting.formatDuration(dayTotal),
                                    Formatting.formatDuration(weekTotal),
                                };
        }

        @Override
        protected void onPostExecute(String[] s) {
            super.onPostExecute(s);
            todaysTotalTextView.setText(s[0]);
            thisWeeksTotalTextView.setText(s[1]);
        }
    }

    private class IconUpdater extends AsyncTask<Void, Void, Boolean>  {

        @Override
        protected Boolean doInBackground(Void... voids) {
            return TimeAndDurationService.isCheckedIn();
        }

        @Override
        protected void onPostExecute(Boolean checkedIn) {
            if (checkedIn) {
                pausePlayImageView.setImageResource(R.drawable.ic_av_pause_circle_outline_blue);
            } else {
                pausePlayImageView.setImageResource(R.drawable.ic_av_play_circle_outline_blue);
            }
        }
    }

}
