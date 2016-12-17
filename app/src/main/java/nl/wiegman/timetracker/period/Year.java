package nl.wiegman.timetracker.period;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import nl.wiegman.timetracker.util.TimeAndDurationService;

public class Year extends AbstractPeriod {

    public Year(Calendar dayInYear) {
        super(TimeAndDurationService.getFirstDayOfYear(dayInYear.get(Calendar.YEAR)), TimeAndDurationService.getLastDayOfYear(dayInYear.get(Calendar.YEAR)));
    }

    @Override
    public String getTitle() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        return StringUtils.capitalize(sdf.format(getFrom().getTime())) + " " + getFrom().get(Calendar.YEAR);
    }

    @Override
    public Period getPrevious() {
        Calendar previous = (Calendar) getFrom().clone();
        previous.add(Calendar.YEAR, -1);
        return new Year(previous);
    }

    @Override
    public Period getNext() {
        Calendar next = (Calendar) getFrom().clone();
        next.add(Calendar.YEAR, 1);
        return new Year(next);
    }
}
