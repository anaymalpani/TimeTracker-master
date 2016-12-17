package nl.wiegman.timetracker.period;

import org.apache.commons.lang3.Range;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import nl.wiegman.timetracker.domain.TimeRecord;
import nl.wiegman.timetracker.util.TimeAndDurationService;

public abstract class AbstractPeriod implements Period {
    private Calendar from;
    private Calendar to;

    private Long billableDuration = null;

    public AbstractPeriod(Calendar from, Calendar to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public Calendar getFrom() {
        return from;
    }

    @Override
    public Calendar getTo() {
        return to;
    }

    @Override
    public long getBillableDuration() {

        if (billableDuration == null || isCurrentMomentInPeriod()) {
            long result = 0;
            List<TimeRecord> timeRecordsInPeriod = TimeAndDurationService.getTimeRecordsBetween(from, to);
            for (TimeRecord timeRecord : timeRecordsInPeriod) {
                result += timeRecord.getBillableDuration();
            }
            billableDuration = result;
        }

        return billableDuration;
    }

    private boolean isCurrentMomentInPeriod() {
        return Range.between(from.getTimeInMillis(), to.getTimeInMillis()).contains(new Date().getTime());
    }

    @Override
    public long getPreferredBillableDuration() {
        return 0;
    }
}
