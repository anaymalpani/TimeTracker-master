package nl.wiegman.timetracker;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.lang3.time.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.OnItemLongClick;
import nl.wiegman.timetracker.export_import.PdfExport;
import nl.wiegman.timetracker.period.Day;
import nl.wiegman.timetracker.period.Period;
import nl.wiegman.timetracker.util.Formatting;
import nl.wiegman.timetracker.util.PeriodicRunnableExecutor;
import nl.wiegman.timetracker.util.TimeAndDurationService;

/**
 * Shows a list of days within a given period.
 * For each dat the billable duration and difference with the preferred billable duration is shown.
 *
 * The timerecords on a day can be deleted (long press) or edited (on click).
 * A swipe to the left or right causes navigation to the previous or next period.
 */
public class DaysInPeriodFragment extends Fragment {
    public static final int MENU_ITEM_EXPORT_TO_PDF_ID = 0;

    private static final String ARG_PERIOD = "periodTitle";

    private Period period;

    @BindView(R.id.title)
    TextView titleTextView;

    @BindView(R.id.timeRecordsInPeriodListView)
    ListView billableDurationOnDayListView;

    @BindView(R.id.totalBillableDurationColumn)
    TextView footerTotalTextView;

    private SwipeDetector listViewSwipeDetector;

    private BillableHoursPerDayAdapter listViewAdapter;

    private PeriodicRunnableExecutor checkedInTimeUpdaterExecutor;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TimeRecordsInPeriodFragment.
     */
    public static DaysInPeriodFragment newInstance(Period period) {
        DaysInPeriodFragment fragment = new DaysInPeriodFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PERIOD, period);
        fragment.setArguments(args);
        return fragment;
    }

    public DaysInPeriodFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        if (getArguments() != null) {
            period = (Period) getArguments().getSerializable(ARG_PERIOD);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_time_records_in_period, container, false);
        ButterKnife.bind(this, rootView);

        listViewSwipeDetector = new SwipeDetector();
        billableDurationOnDayListView.setOnTouchListener(listViewSwipeDetector);

        refreshData();

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(0, MENU_ITEM_EXPORT_TO_PDF_ID, 0, R.string.export_to_pdf);
    }

    @Override
    public void onStart() {
        super.onStart();
        activateRecalculateCurrentDayUpdater();
    }

    @Override
    public void onStop() {
        super.onStop();
        deactivateCheckedInTimeUpdater();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == MENU_ITEM_EXPORT_TO_PDF_ID) {
            new PdfExport(getActivity(), period).execute();
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.previousImageView)
    public void previousOnClick(View view) {
        previousPeriod();
    }

    @OnClick(R.id.nextImageView)
    public void nextOnClick(View view) {
        nextPeriod();
    }


    private void activateRecalculateCurrentDayUpdater() {
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
            if (TimeAndDurationService.isCheckedIn()) {
                new PeriodicUpdate().execute();
            }
        }
    }

    private class PeriodicUpdate extends AsyncTask<Void, Void, CurrentItemUpdateData> {

        @Override
        protected CurrentItemUpdateData doInBackground(Void... voids) {
            CurrentItemUpdateData data = new CurrentItemUpdateData();
            Day today = new Day(Calendar.getInstance());
            data.currentDayFormattedBillableDuration = today.getBillableDuration();
            data.currentDayFormattedDifferenceFromBillableDuration = today.getBillableDuration() - today.getPreferredBillableDuration();
            data.total = period.getBillableDuration();
            return data;
        }

        @Override
        protected void onPostExecute(CurrentItemUpdateData updatedData) {
            List<BillableDurationOnDay> billableDurationOnDays = listViewAdapter.getBillableDurationOnDays();
            Integer positionOfCurrentItem = getPositionOfCurrentItem(billableDurationOnDays);
            if (positionOfCurrentItem != null) {
                int index = positionOfCurrentItem - billableDurationOnDayListView.getFirstVisiblePosition();
                if (index >= 0) {
                    View dayItemView = billableDurationOnDayListView.getChildAt(index);
                    if (dayItemView != null) {
                        TextView billableDurationColumnTextView = (TextView) dayItemView.findViewById(R.id.totalBillableDurationColumn);
                        billableDurationColumnTextView.setText(Formatting.formatDuration(updatedData.currentDayFormattedBillableDuration));

                        TextView differenceWithBillableDurationColumnTextView = (TextView) dayItemView.findViewById(R.id.differenceFromPreferredBillableDurationColumn);
                        configureDifferenceFromPreferredBillableDurationTextView(differenceWithBillableDurationColumnTextView, updatedData.currentDayFormattedBillableDuration, updatedData.currentDayFormattedDifferenceFromBillableDuration);
                    }
                }
            }
            footerTotalTextView.setText(Formatting.formatDuration(updatedData.total));
        }
    }

    private class CurrentItemUpdateData {
        private long total;
        private long currentDayFormattedBillableDuration;
        private long currentDayFormattedDifferenceFromBillableDuration;
    }

    private Integer getPositionOfCurrentItem(List<BillableDurationOnDay> billableHoursOnDays) {
        Integer result = null;
        for (int position = 0; position < billableHoursOnDays.size(); position++) {
            BillableDurationOnDay billableHoursOnDay = billableHoursOnDays.get(position);
            if (isCurrentDay(billableHoursOnDay)) {
                result = position;
                break;
            }
        }
        return result;
    }

    private boolean isCurrentDay(BillableDurationOnDay billableHoursOnDay) {
        Calendar today = Calendar.getInstance();
        return DateUtils.isSameDay(billableHoursOnDay.getDay(), today);
    }

    private void refreshData() {
        titleTextView.setText(period.getTitle());
        new RefreshData().execute();
    }

    private List<BillableDurationOnDay> getBillableHoursOnDays() {
        List<BillableDurationOnDay> days = new ArrayList<>();

        Calendar calendarOfDay = (Calendar) period.getFrom().clone();

        while (calendarOfDay.getTimeInMillis() < period.getTo().getTimeInMillis()) {
            BillableDurationOnDay billableHoursOnDay = new BillableDurationOnDay();
            billableHoursOnDay.setDay((Calendar)calendarOfDay.clone());

            Day day = new Day(calendarOfDay);
            billableHoursOnDay.setBillableDuration(day.getBillableDuration());
            billableHoursOnDay.setDifferenceFromPreferredBillableDuration(day.getBillableDuration() - day.getPreferredBillableDuration());

            days.add(billableHoursOnDay);

            calendarOfDay.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    private class BillableHoursPerDayAdapter extends BaseAdapter {
        private final List<BillableDurationOnDay> billableDurationOnDays;

        /**
         * Constructor
         */
        public BillableHoursPerDayAdapter(List<BillableDurationOnDay> billableDurationOnDays) {
            this.billableDurationOnDays = billableDurationOnDays;
        }

        @Override
        public int getCount() {
            return billableDurationOnDays.size();
        }

        @Override
        public Object getItem(int position) {
            return billableDurationOnDays.get(position);
        }

        @Override
        public long getItemId(int position) {
            return billableDurationOnDays.get(position).getDay().getTimeInMillis();
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            BillableDurationOnDay billableDurationOnDay = billableDurationOnDays.get(position);

            Date day = billableDurationOnDay.getDay().getTime();

            View row = View.inflate(getActivity(), R.layout.time_records_in_period_item, null);

            TextView dayColumn = getDayOfWeekColumnTextView(day, row);
            TextView dateColumn = getDateColumnTextView(day, row);
            TextView billableDurationColumn = getBillableDurationTextView(row, billableDurationOnDay.getBillableDuration());
            TextView differenceColumn = getDifferenceFromPreferredBillableDurationTextView(row, billableDurationOnDay);

            if (isCurrentDay(billableDurationOnDay)) {
                dayColumn.setTypeface(Typeface.DEFAULT_BOLD);
                dateColumn.setTypeface(Typeface.DEFAULT_BOLD);
                billableDurationColumn.setTypeface(Typeface.DEFAULT_BOLD);
                differenceColumn.setTypeface(Typeface.DEFAULT_BOLD);
            } else if (billableDurationOnDay.getBillableDuration() == 0) {
                dayColumn.setEnabled(false);
                dateColumn.setEnabled(false);
                billableDurationColumn.setEnabled(false);
                differenceColumn.setEnabled(false);
            }
            return row;
        }

        private TextView getDayOfWeekColumnTextView(Date day, View row) {
            TextView dayColumn = (TextView) row.findViewById(R.id.dayOfWeekColumn);
            SimpleDateFormat dayInWeekFormat = new SimpleDateFormat("EEE");
            String formattedDay = dayInWeekFormat.format(day);
            dayColumn.setText(formattedDay);
            return dayColumn;
        }

        private TextView getDateColumnTextView(Date day, View row) {
            TextView dateColumn = (TextView) row.findViewById(R.id.dateColumn);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM");
            String formattedDate = simpleDateFormat.format(day);
            dateColumn.setText(formattedDate);
            return dateColumn;
        }

        private TextView getBillableDurationTextView(View row, long billableDuration) {
            TextView billableHoursColumn = (TextView) row.findViewById(R.id.totalBillableDurationColumn);
            String formattedBillableDuration = Formatting.formatDuration(billableDuration);
            billableHoursColumn.setText(formattedBillableDuration);
            return billableHoursColumn;
        }

        private TextView getDifferenceFromPreferredBillableDurationTextView(View row, BillableDurationOnDay billableDurationOnDay) {
            TextView differenceColumn = (TextView) row.findViewById(R.id.differenceFromPreferredBillableDurationColumn);
            configureDifferenceFromPreferredBillableDurationTextView(differenceColumn, billableDurationOnDay.getBillableDuration(), billableDurationOnDay.getDifferenceFromPreferredBillableDuration());
            return differenceColumn;
        }

        public List<BillableDurationOnDay> getBillableDurationOnDays() {
            return billableDurationOnDays;
        }
    }

    private void configureDifferenceFromPreferredBillableDurationTextView(TextView differenceColumn, long billableDuration, long differenceFromPreferredBillableDuration) {
        if (billableDuration > 0) {
            String formattedDifference = Formatting.formatDuration(differenceFromPreferredBillableDuration);
            if (differenceFromPreferredBillableDuration == 0) {
                differenceColumn.setTextColor(Color.parseColor("#006400"));
            } else if (differenceFromPreferredBillableDuration > 0) {
                formattedDifference = "+" + formattedDifference;
                differenceColumn.setTextColor(Color.parseColor("#006400"));
            } else {
                formattedDifference = "-" + formattedDifference;
                differenceColumn.setTextColor(Color.RED);
            }
            differenceColumn.setText(formattedDifference);
        } else {
            differenceColumn.setText("");
        }
    }

    private class BillableDurationOnDay {
        private Calendar day;
        private long billableDuration;
        private long differenceFromPreferredBillableDuration;

        public Calendar getDay() {
            return day;
        }

        public void setDay(Calendar day) {
            this.day = day;
        }

        public long getBillableDuration() {
            return billableDuration;
        }

        public void setBillableDuration(long billableDuration) {
            this.billableDuration = billableDuration;
        }

        public long getDifferenceFromPreferredBillableDuration() {
            return differenceFromPreferredBillableDuration;
        }

        public void setDifferenceFromPreferredBillableDuration(long differenceFromPreferredBillableDuration) {
            this.differenceFromPreferredBillableDuration = differenceFromPreferredBillableDuration;
        }
    }

    @OnItemClick(R.id.timeRecordsInPeriodListView)
    public void onItemClick(AdapterView<?> arg0, View view, int position, long timestamp) {
        if(listViewSwipeDetector.swipeDetected()) {
            handleSwipe();
        } else {
            showDetails(timestamp);
        }
    }

    @OnItemLongClick(R.id.timeRecordsInPeriodListView)
    public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long timestamp) {
        boolean result;
        if(listViewSwipeDetector.swipeDetected()) {
            result = false;
            handleSwipe();
        } else {
            result = true;
            delete(timestamp);
        }
        return result;
    }

    private void showDetails(long timestamp) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(timestamp);
        DayDetailsHelper dayDetailsHelper = new DayDetailsHelper(getActivity());
        dayDetailsHelper.showDetailsOrTimeRecordsOfDay(day);
    }

    private void delete(long timestamp) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(timestamp);

        Calendar startOfDay = TimeAndDurationService.getStartOfDay(day);
        Calendar endOfDay = TimeAndDurationService.getEndOfDay(day);

        DeleteTimeRecordsInPeriod.TimeRecordsDeletedListener timeRecordsDeletedListener = new DeleteTimeRecordsInPeriod.TimeRecordsDeletedListener() {
            @Override
            public void recordDeleted() {
                refreshData();
            }
        };
        DeleteTimeRecordsInPeriod deleteTimeRecordsInPeriod = new DeleteTimeRecordsInPeriod(getActivity(), startOfDay, endOfDay, timeRecordsDeletedListener);
        deleteTimeRecordsInPeriod.handleUserRequestToDeleteRecordsInPeriod();
    }

    private void handleSwipe() {
        if(listViewSwipeDetector.getAction() == SwipeDetector.Action.RL) {
            nextPeriod();
        } else if (listViewSwipeDetector.getAction() == SwipeDetector.Action.LR) {
            previousPeriod();
        }
    }

    private void previousPeriod() {
        period = period.getPrevious();
        refreshData();
    }

    private void nextPeriod() {
        period = period.getNext();
        refreshData();
    }

    private class RefreshData extends AsyncTask<Void, Void, List<BillableDurationOnDay>> {

        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            openProgressDialog();
        }

        @Override
        protected List<BillableDurationOnDay> doInBackground(Void... voids) {
            return getBillableHoursOnDays();
        }

        @Override
        protected void onPostExecute(List<BillableDurationOnDay> billableHoursOnDays) {
            listViewAdapter = new BillableHoursPerDayAdapter(billableHoursOnDays);
            billableDurationOnDayListView.setAdapter(listViewAdapter);

            Integer positionOfCurrentItem = getPositionOfCurrentItem(billableHoursOnDays);
            if (positionOfCurrentItem != null) {
                billableDurationOnDayListView.setSelection(positionOfCurrentItem);
            }

            setTotalInFooter(billableHoursOnDays);
            closeProgressDialog();
        }

        private void setTotalInFooter(List<BillableDurationOnDay> billableHoursOnDays) {
            long total = 0;
            for (BillableDurationOnDay billableHoursOnDay : billableHoursOnDays) {
                total += billableHoursOnDay.getBillableDuration();
            }
            footerTotalTextView.setText(Formatting.formatDuration(total));
        }

        private void openProgressDialog() {
            dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getActivity().getString(R.string.loading_data));
            dialog.show();
        }

        private void closeProgressDialog() {
            if (dialog.isShowing()) {
                dialog.dismiss();
                dialog = null;
            }
        }
    }
}
