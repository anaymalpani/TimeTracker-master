package nl.wiegman.timetracker;

import android.os.Bundle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nl.wiegman.timetracker.period.Month;
import nl.wiegman.timetracker.period.Period;
import nl.wiegman.timetracker.util.TimeAndDurationService;

public class MonthsOverviewFragment extends AbstractPeriodsInYearOverviewFragment {

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MonthsOverviewFragment.
     */
    public static MonthsOverviewFragment newInstance(int year) {
        MonthsOverviewFragment fragment = new MonthsOverviewFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_YEAR, year);
        fragment.setArguments(args);
        return fragment;
    }

    public MonthsOverviewFragment() {
        // Required empty public constructor
    }

    @Override
    protected long getActualPeriodBillableDuration() {
        return new Month(Calendar.getInstance()).getBillableDuration();
    }

    @Override
    protected Period getPeriod(long month, int year) {
        Calendar date = Calendar.getInstance();
        date.clear();
        date.set(Calendar.MONTH, (int)month);
        date.set(Calendar.YEAR, year);
        return new Month(date);
    }

    @Override
    protected List<PeriodOverviewItem> getOverviewItems(int year) {
        List<PeriodOverviewItem> periodOverviewItems = new ArrayList<>();

        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        Calendar dayInMonth = TimeAndDurationService.getFirstDayOfYear(year);
        while (dayInMonth.get(Calendar.YEAR) == year) {
            long billableDurationInMonth = new Month(dayInMonth).getBillableDuration();
            int month = dayInMonth.get(Calendar.MONTH);
            boolean isCurrentPeriod = month==currentMonth && year==currentYear;

            PeriodOverviewItem periodOverviewItem = new PeriodOverviewItem();
            periodOverviewItem.setPeriodId(month);
            periodOverviewItem.setPeriodName(new SimpleDateFormat("MMM").format(dayInMonth.getTime()));
            periodOverviewItem.setCurrentPeriod(isCurrentPeriod);
            periodOverviewItem.setBillableDuration(billableDurationInMonth);

            periodOverviewItems.add(periodOverviewItem);

            dayInMonth.add(Calendar.MONTH, 1);
        }
        return periodOverviewItems;
    }
}
