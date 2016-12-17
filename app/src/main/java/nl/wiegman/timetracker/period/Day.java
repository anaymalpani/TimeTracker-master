package nl.wiegman.timetracker.period;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import nl.wiegman.timetracker.util.TimeAndDurationService;

public class Day extends AbstractPeriod {

    public Day(Calendar day) {
        super(TimeAndDurationService.getStartOfDay(day), TimeAndDurationService.getEndOfDay(day));
    }

    @Override
    public String getTitle() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd-MM-yyyy");
        return sdf.format(getFrom().getTime());
    }

    @Override
    public Period getPrevious() {
        Calendar previous = (Calendar) getFrom().clone();
        previous.add(Calendar.DAY_OF_MONTH, -1);
        return new Day(previous);
    }

    @Override
    public Period getNext() {
        Calendar next = (Calendar) getFrom().clone();
        next.add(Calendar.DAY_OF_MONTH, 1);
        return new Day(next);
    }

    @Override
    public long getPreferredBillableDuration() {
        int dayOfWeek = getFrom().get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return 0;
        } else {
            return TimeUnit.HOURS.toMillis(8);
        }
    }
}
