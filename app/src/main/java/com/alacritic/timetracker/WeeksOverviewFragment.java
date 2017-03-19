package com.alacritic.timetracker;

import android.os.Bundle;

import com.alacritic.timetracker.period.Period;
import com.alacritic.timetracker.period.Week;
import com.alacritic.timetracker.util.TimeAndDurationService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WeeksOverviewFragment extends AbstractPeriodsInYearOverviewFragment {

    public WeeksOverviewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WeeksOverviewFragment.
     */
    public static WeeksOverviewFragment newInstance(int year) {
        WeeksOverviewFragment fragment = new WeeksOverviewFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_YEAR, year);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected long getActualPeriodBillableDuration() {
        return new Week(Calendar.getInstance()).getBillableDuration();
    }

    @Override
    protected Period getPeriod(long weekNumber, int year) {
        Calendar date = Calendar.getInstance();
        date.clear();
        date.set(Calendar.WEEK_OF_YEAR, (int) weekNumber);
        date.set(Calendar.YEAR, year);
        return new Week(date);
    }

    @Override
    protected List<PeriodOverviewItem> getOverviewItems(int year) {
        List<PeriodOverviewItem> periodOverviewItems = new ArrayList<>();

        int currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        Calendar dayInWeek = TimeAndDurationService.getFirstDayOfYear(year);
        while (dayInWeek.get(Calendar.YEAR) == year) {
            PeriodOverviewItem periodOverviewItem = new PeriodOverviewItem();

            int weekNumber = dayInWeek.get(Calendar.WEEK_OF_YEAR);
            boolean isCurrentPeriod = currentWeek == weekNumber && currentYear == year;

            periodOverviewItem.setPeriodId(weekNumber);
            periodOverviewItem.setPeriodName(Integer.toString(weekNumber));
            periodOverviewItem.setCurrentPeriod(isCurrentPeriod);

            long billableDurationInWeek = new Week(dayInWeek).getBillableDuration();
            periodOverviewItem.setBillableDuration(billableDurationInWeek);

            periodOverviewItems.add(periodOverviewItem);

            dayInWeek.add(Calendar.DAY_OF_MONTH, 7);
        }
        return periodOverviewItems;
    }
}
