package com.alacritic.timetracker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.alacritic.timetracker.domain.TimeRecord;
import com.alacritic.timetracker.period.Period;
import com.alacritic.timetracker.util.Formatting;
import com.alacritic.timetracker.util.FragmentHelper;
import com.alacritic.timetracker.util.TimeAndDurationService;

import java.text.SimpleDateFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import butterknife.OnItemLongClick;

/**
 * Shows a list of timerecords within a given period.
 * The timerecords can be deleted or edited (which is actually processed in EditTimeRecordFragemnt)
 */
public class TimeRecordsInPeriodFragment extends Fragment {
    private static final String ARG_PERIOD = "period";
    @BindView(R.id.title)
    TextView titleTextView;
    @BindView(R.id.timeRecordsInPeriodListView)
    ListView timeRecordsListView;
    @BindView(R.id.totalBillableDurationColumn)
    TextView footerTotalTextView;
    private Period period;

    public TimeRecordsInPeriodFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TimeRecordsInPeriodFragment.
     */
    public static TimeRecordsInPeriodFragment newInstance(Period period) {
        TimeRecordsInPeriodFragment fragment = new TimeRecordsInPeriodFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PERIOD, period);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            period = (Period) getArguments().getSerializable(ARG_PERIOD);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_time_records_in_period, container, false);
        ButterKnife.bind(this, rootView);

        rootView.findViewById(R.id.previousImageView).setVisibility(View.GONE);
        rootView.findViewById(R.id.nextImageView).setVisibility(View.GONE);

        refreshData();

        return rootView;
    }

    public void deleteTimeRecordWhenConfirmed(final long timeRecordId) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        TimeRecord timeRecord = TimeRecord.findById(TimeRecord.class, timeRecordId);
                        TimeRecordDelete.run(timeRecord, getActivity());
                        refreshData();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.dismiss();
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.delete_this_row)
                .setTitle(R.string.confirm)
                .setPositiveButton(android.R.string.yes, dialogClickListener)
                .setNegativeButton(android.R.string.no, dialogClickListener)
                .show();
    }

    private void editTimeRecord(long timeRecordId) {
        EditTimeRecordFragment fragment = EditTimeRecordFragment.newInstance(timeRecordId);
        FragmentHelper.showFragment(getActivity(), fragment);
    }

    private void refreshData() {
        titleTextView.setText(period.getTitle());

        List<TimeRecord> timeRecordsInPeriod = TimeAndDurationService.getTimeRecordsBetween(period.getFrom(), period.getTo());
        if (timeRecordsInPeriod == null || timeRecordsInPeriod.isEmpty()) {
            // This can happen when the last item is deleted
            getFragmentManager().popBackStack();
        } else {
            TimeRecordsInPeriodAdapter timeRecordsInPeriodAdapter = new TimeRecordsInPeriodAdapter(timeRecordsInPeriod);
            timeRecordsListView.setAdapter(timeRecordsInPeriodAdapter);
            footerTotalTextView.setText(Formatting.formatDuration(getTotalBillableDuration(timeRecordsInPeriod)));
        }
    }

    private long getTotalBillableDuration(List<TimeRecord> timeRecordsInPeriod) {
        long result = 0;
        for (TimeRecord timeRecord : timeRecordsInPeriod) {
            result += timeRecord.getBillableDuration();
        }
        return result;
    }

    @OnItemClick(R.id.timeRecordsInPeriodListView)
    public void onItemClick(AdapterView<?> arg0, View view, int position, long timeRecordId) {
        editTimeRecord(timeRecordId);
    }

    @OnItemLongClick(R.id.timeRecordsInPeriodListView)
    public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long id) {
        deleteTimeRecordWhenConfirmed(id);
        return true;
    }

    private class TimeRecordsInPeriodAdapter extends BaseAdapter {

        private final List<TimeRecord> timeRecordsInPeriod;

        /**
         * Constructor
         */
        public TimeRecordsInPeriodAdapter(List<TimeRecord> timeRecordsInPeriod) {
            this.timeRecordsInPeriod = timeRecordsInPeriod;
        }

        @Override
        public int getCount() {
            return timeRecordsInPeriod.size();
        }

        @Override
        public Object getItem(int position) {
            return timeRecordsInPeriod.get(position);
        }

        @Override
        public long getItemId(int position) {
            return timeRecordsInPeriod.get(position).getId();
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            TimeRecord timeRecord = timeRecordsInPeriod.get(position);

            View row = View.inflate(getActivity(), R.layout.time_records_in_period_item, null);

            TextView dayColumn = (TextView) row.findViewById(R.id.dayOfWeekColumn);
            SimpleDateFormat dayInWeekFormat = new SimpleDateFormat("EEE");
            String formattedDay = dayInWeekFormat.format(timeRecord.getCheckIn().getTime());
            dayColumn.setText(formattedDay);

            TextView dateColumn = (TextView) row.findViewById(R.id.dateColumn);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM");
            String formattedDate = simpleDateFormat.format(timeRecord.getCheckIn().getTime());
            dateColumn.setText(formattedDate);

            TextView billableDurationColumn = (TextView) row.findViewById(R.id.totalBillableDurationColumn);
            String formattedBillableDuration = Formatting.formatDuration(timeRecord.getBillableDuration());
            billableDurationColumn.setText(formattedBillableDuration);

            TextView differenceFromBillableDurationColumn = (TextView) row.findViewById(R.id.differenceFromPreferredBillableDurationColumn);
            differenceFromBillableDurationColumn.setText("");

            return row;
        }
    }
}
