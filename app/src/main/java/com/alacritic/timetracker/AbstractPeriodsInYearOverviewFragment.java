package com.alacritic.timetracker;

import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.alacritic.timetracker.period.Period;
import com.alacritic.timetracker.period.Year;
import com.alacritic.timetracker.util.Formatting;
import com.alacritic.timetracker.util.FragmentHelper;
import com.alacritic.timetracker.util.PeriodicRunnableExecutor;
import com.alacritic.timetracker.util.TimeAndDurationService;

import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import butterknife.OnItemLongClick;

public abstract class AbstractPeriodsInYearOverviewFragment extends Fragment {
    public static final String INSTANCE_STATE_YEAR = "YEAR";
    protected static final String ARG_YEAR = "year";
    @BindView(R.id.title)
    TextView yearTextView;

    @BindView(R.id.periodsListView)
    ListView periodsListView;

    @BindView(R.id.totalBillableDurationColumn)
    TextView footerTotalTextView;

    private SwipeDetector listViewSwipeDetector;

    private TimeRecordsInPeriodAdapter listViewAdapter;

    private PeriodicRunnableExecutor checkedInTimeUpdaterExecutor;

    private int year;

    public AbstractPeriodsInYearOverviewFragment() {
        // Required empty public constructor
    }

    /**
     * To be overridden by subclasses.
     */
    protected abstract long getActualPeriodBillableDuration();

    protected abstract List<PeriodOverviewItem> getOverviewItems(int year);

    protected abstract Period getPeriod(long periodId, int year);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            year = getArguments().getInt(ARG_YEAR);
        }
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(INSTANCE_STATE_YEAR)) {
                year = savedInstanceState.getInt(INSTANCE_STATE_YEAR);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_period_overview, container, false);
        ButterKnife.bind(this, rootView);

        listViewSwipeDetector = new SwipeDetector();

        periodsListView.setOnTouchListener(listViewSwipeDetector);

        View previous = rootView.findViewById(R.id.previousImageView);
        previous.setOnClickListener(new PreviousOnClickListener());

        View next = rootView.findViewById(R.id.nextImageView);
        next.setOnClickListener(new NextOnClickListener());

        refreshData();

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(INSTANCE_STATE_YEAR, year);
    }

    private void refreshData() {
        yearTextView.setText(Integer.toString(year));
        new RefreshListViewData().execute();
        boolean showProgressDialog = true;
        new RefreshTotal(showProgressDialog).execute();
    }

    @Override
    public void onStart() {
        super.onStart();
        activateRecalculateCurrentPeriodUpdater();
    }

    @Override
    public void onStop() {
        super.onStop();
        deactivateCheckedInTimeUpdater();
    }

    private void activateRecalculateCurrentPeriodUpdater() {
        checkedInTimeUpdaterExecutor = new PeriodicRunnableExecutor(1000, new CheckedInTimeUpdater());
        checkedInTimeUpdaterExecutor.start();
    }

    private void deactivateCheckedInTimeUpdater() {
        if (checkedInTimeUpdaterExecutor != null) {
            checkedInTimeUpdaterExecutor.stop();
            checkedInTimeUpdaterExecutor = null;
        }
    }

    private void previousYear() {
        year -= 1;
        refreshData();
    }

    private void nextYear() {
        year += 1;
        refreshData();
    }

    private Integer getPositionOfCurrentItem(List<PeriodOverviewItem> periodOverviewItems) {
        Integer result = null;
        for (int position = 0; position < periodOverviewItems.size(); position++) {
            PeriodOverviewItem period = periodOverviewItems.get(position);
            if (period.isCurrentPeriod()) {
                result = position;
                break;
            }
        }
        return result;
    }

    private void showTimeRecordsInPeriod(int periodId, int year) {
        Period period = getPeriod(periodId, year);
        DaysInPeriodFragment fragment = DaysInPeriodFragment.newInstance(period);
        FragmentHelper.showFragment(getActivity(), fragment);
    }

    @OnItemClick(R.id.periodsListView)
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (listViewSwipeDetector.swipeDetected()) {
            handleSwipe();
        } else {
            handleClick((int) id);
        }
    }

    private void handleClick(int id) {
        showTimeRecordsInPeriod(id, year);
    }

    @OnItemLongClick(R.id.periodsListView)
    public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long periodId) {
        boolean result;
        if (listViewSwipeDetector.swipeDetected()) {
            result = false;
            handleSwipe();
        } else {
            result = true;
            handleLongCLick(periodId);
        }
        return result;
    }

    private void handleLongCLick(long periodId) {
        Period period = getPeriod(periodId, year);

        DeleteTimeRecordsInPeriod.TimeRecordsDeletedListener timeRecordsDeletedListener = new DeleteTimeRecordsInPeriod.TimeRecordsDeletedListener() {
            @Override
            public void recordDeleted() {
                refreshData();
            }
        };
        DeleteTimeRecordsInPeriod deleteTimeRecordsInPeriod = new DeleteTimeRecordsInPeriod(getActivity(), period.getFrom(), period.getTo(), timeRecordsDeletedListener);
        deleteTimeRecordsInPeriod.handleUserRequestToDeleteRecordsInPeriod();
    }

    private void handleSwipe() {
        if (listViewSwipeDetector.getAction() == SwipeDetector.Action.RL) {
            nextYear();
        } else if (listViewSwipeDetector.getAction() == SwipeDetector.Action.LR) {
            previousYear();
        }
    }

    private Long getTotalBillableDuration() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        return new Year(calendar).getBillableDuration();
    }

    private ProgressDialog openProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getActivity().getString(R.string.loading_data));
        dialog.show();
        return dialog;
    }

    private void closeProgressDialog(ProgressDialog dialog) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private class PreviousOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            previousYear();
        }
    }

    private class NextOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            nextYear();
        }
    }

    private class CheckedInTimeUpdater implements Runnable {
        @Override
        public void run() {
            if (TimeAndDurationService.isCheckedIn()) {
                new Updater().execute();
            }
        }
    }

    private class Updater extends AsyncTask<Void, Void, String[]> {

        @Override
        protected String[] doInBackground(Void... voids) {
            long currentPeriodBillableDuration = getActualPeriodBillableDuration();
            long totalBillableDuration = getTotalBillableDuration();
            return new String[]{
                    Formatting.formatDuration(currentPeriodBillableDuration),
                    Formatting.formatDuration(totalBillableDuration),
            };
        }

        @Override
        protected void onPostExecute(String[] formattedBillableDuration) {
            super.onPostExecute(formattedBillableDuration);

            Integer positionOfCurrentItem = getPositionOfCurrentItem(listViewAdapter.getOverviewItems());
            if (positionOfCurrentItem != null) {
                int index = positionOfCurrentItem - periodsListView.getFirstVisiblePosition();
                if (index >= 0) {
                    View periodOverviewItem = periodsListView.getChildAt(index);
                    if (periodOverviewItem != null) {
                        TextView billableDurationColumnTextView = (TextView) periodOverviewItem.findViewById(R.id.totalBillableDurationColumn);
                        billableDurationColumnTextView.setText(formattedBillableDuration[0]);
                    }
                }
            }
            footerTotalTextView.setText(formattedBillableDuration[1]);
        }
    }

    private class TimeRecordsInPeriodAdapter extends BaseAdapter {
        private final List<PeriodOverviewItem> overviewItems;

        /**
         * Constructor
         */
        public TimeRecordsInPeriodAdapter(List<PeriodOverviewItem> overviewItems) {
            this.overviewItems = overviewItems;
        }

        @Override
        public int getCount() {
            return overviewItems.size();
        }

        @Override
        public Object getItem(int position) {
            return overviewItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return overviewItems.get(position).getPeriodId();
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            PeriodOverviewItem periodOverviewItem = overviewItems.get(position);

            View row = View.inflate(getActivity(), R.layout.period_overview_item, null);

            TextView periodNumberTextView = (TextView) row.findViewById(R.id.periodNumberColumn);
            periodNumberTextView.setText(periodOverviewItem.getPeriodName());

            TextView billableDurationColumn = (TextView) row.findViewById(R.id.totalBillableDurationColumn);
            long billableDuration = periodOverviewItem.getBillableDuration();

            String formattedBillableDuration = Formatting.formatDuration(billableDuration);
            billableDurationColumn.setText(formattedBillableDuration);

            if (periodOverviewItem.isCurrentPeriod()) {
                billableDurationColumn.setTypeface(Typeface.DEFAULT_BOLD);
                periodNumberTextView.setTypeface(Typeface.DEFAULT_BOLD);
            } else if (billableDuration == 0) {
                billableDurationColumn.setEnabled(false);
                periodNumberTextView.setEnabled(false);
            }

            return row;
        }

        public List<PeriodOverviewItem> getOverviewItems() {
            return overviewItems;
        }
    }

    protected class PeriodOverviewItem {
        private long periodId;
        private boolean isCurrentPeriod;
        private String periodName;
        private long billableDuration;

        public String getPeriodName() {
            return periodName;
        }

        public void setPeriodName(String periodName) {
            this.periodName = periodName;
        }

        public long getBillableDuration() {
            return billableDuration;
        }

        public void setBillableDuration(long billableDuration) {
            this.billableDuration = billableDuration;
        }

        public long getPeriodId() {
            return periodId;
        }

        public void setPeriodId(long periodId) {
            this.periodId = periodId;
        }

        public boolean isCurrentPeriod() {
            return isCurrentPeriod;
        }

        public void setCurrentPeriod(boolean isCurrentPeriod) {
            this.isCurrentPeriod = isCurrentPeriod;
        }
    }

    private class RefreshListViewData extends AsyncTask<Void, Void, List<PeriodOverviewItem>> {

        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = openProgressDialog();
        }

        @Override
        protected List<PeriodOverviewItem> doInBackground(Void... voids) {
            return getOverviewItems(year);
        }

        @Override
        protected void onPostExecute(List<PeriodOverviewItem> periodOverviewItems) {
            listViewAdapter = new TimeRecordsInPeriodAdapter(periodOverviewItems);
            periodsListView.setAdapter(listViewAdapter);

            Integer positionOfCurrentItem = getPositionOfCurrentItem(periodOverviewItems);
            if (positionOfCurrentItem != null) {
                periodsListView.setSelection(positionOfCurrentItem);
            }
            closeProgressDialog(dialog);
        }
    }

    private class RefreshTotal extends AsyncTask<Void, Void, Long> {

        private final boolean showProgressDialog;
        private ProgressDialog dialog;

        public RefreshTotal(boolean showProgressDialog) {
            this.showProgressDialog = showProgressDialog;
        }

        @Override
        protected void onPreExecute() {
            if (showProgressDialog) {
                dialog = openProgressDialog();
            }
        }

        @Override
        protected Long doInBackground(Void... voids) {
            return getTotalBillableDuration();
        }

        @Override
        protected void onPostExecute(Long totalBillableDuration) {
            footerTotalTextView.setText(Formatting.formatDuration(totalBillableDuration));
            if (showProgressDialog) {
                closeProgressDialog(dialog);
            }
        }
    }
}
