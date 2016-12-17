package nl.wiegman.timetracker.period;

import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import nl.wiegman.timetracker.util.TimeAndDurationService;

public class Week extends AbstractPeriod implements Period {

    public Week(Calendar dayInWeek) {
        super(TimeAndDurationService.getStartOfWeek(dayInWeek), TimeAndDurationService.getEndOfWeek(dayInWeek));
    }

    @Override
    public String getTitle() {
        return String.format("Week %d - %4d", getFrom().get(Calendar.WEEK_OF_YEAR), getFrom().get(Calendar.YEAR));
    }

    @Override
    public Period getPrevious() {
        Calendar previous = (Calendar) getFrom().clone();
        previous.add(Calendar.WEEK_OF_YEAR, -1);
        return new Week(previous);
    }

    @Override
    public Period getNext() {
        Calendar next = (Calendar) getFrom().clone();
        next.add(Calendar.WEEK_OF_YEAR, 1);
        return new Week(next);
    }

    @Override
    public long getPreferredBillableDuration() {
        return TimeUnit.HOURS.toMillis(40);
    }

}
