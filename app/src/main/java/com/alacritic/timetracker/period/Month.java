package com.alacritic.timetracker.period;

import com.alacritic.timetracker.util.TimeAndDurationService;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Month extends AbstractPeriod {

    public Month(Calendar dayInMonth) {
        super(TimeAndDurationService.getStartOfMonth(dayInMonth), TimeAndDurationService.getEndOfMonth(dayInMonth));
    }

    @Override
    public String getTitle() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM");
        return StringUtils.capitalize(sdf.format(getFrom().getTime())) + " " + getFrom().get(Calendar.YEAR);
    }

    @Override
    public Period getPrevious() {
        Calendar previous = (Calendar) getFrom().clone();
        previous.add(Calendar.MONTH, -1);
        return new Month(previous);
    }

    @Override
    public Period getNext() {
        Calendar next = (Calendar) getFrom().clone();
        next.add(Calendar.MONTH, 1);
        return new Month(next);
    }
}
