package nl.wiegman.timetracker.domain;

import com.orm.SugarRecord;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TimeRecord extends SugarRecord {

    private static final long DEFAULT_BREAK_DURATION = TimeUnit.MINUTES.toMillis(30);
    private static final long DEFAULT_BREAK_AFTER = TimeUnit.HOURS.toMillis(5) + TimeUnit.MINUTES.toMillis(30);

    private Calendar checkIn;
    private Calendar checkOut;
    private Long breakInMilliseconds;
    private String note;

    public String getNote() {
        return note;
    }

    public TimeRecord setNote(String note) {
        this.note = note;
        return this;
    }

    public Calendar getCheckIn() {
        return checkIn;
    }

    public TimeRecord setCheckIn(Calendar checkIn) {
        this.checkIn = checkIn;
        return this;
    }

    public Calendar getCheckOut() {
        return checkOut;
    }

    public TimeRecord setCheckOut(Calendar checkOut) {
        this.checkOut = checkOut;
        return this;
    }

    public Long getBreakInMilliseconds() {
        return breakInMilliseconds;
    }

    public TimeRecord setBreakInMilliseconds(Long breakInMilliseconds) {
        this.breakInMilliseconds = breakInMilliseconds;
        return this;
    }

    public long getDuration() {
        long result;

        if (isCheckIn()) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MILLISECOND, 0);
            result = calendar.getTimeInMillis() - checkIn.getTimeInMillis();
        } else {
            result = checkOut.getTimeInMillis() - checkIn.getTimeInMillis();
        }

        return result;
    }

    public long getBillableDuration() {
        long duration = getDuration();

        Long breakInMilliseconds = getBreakInMilliseconds();
        if (isCheckIn() && breakInMilliseconds == null) {
            duration = duration - getDefaultBreakDuration(duration);
        } else if (breakInMilliseconds != null) {
            duration = duration - breakInMilliseconds;
        }
        return duration;
    }

    public boolean isCheckIn() {
        return checkOut == null;
    }

    public long getDefaultBreakDuration() {
        return getDefaultBreakDuration(getDuration());
    }

    public long getDefaultBreakDuration(long totalCheckedInDuration) {
        long result = 0;

        if (totalCheckedInDuration > DEFAULT_BREAK_AFTER) {
            long i = totalCheckedInDuration - DEFAULT_BREAK_AFTER;
            if (i > DEFAULT_BREAK_DURATION) {
                result = DEFAULT_BREAK_DURATION;
            } else {
                result = i;
            }
        }
        return result;
    }

    public boolean hasBreak() {
        return getBreakInMilliseconds() != null;
    }

    public void setDefaultBreak() {
        this.breakInMilliseconds = getDefaultBreakDuration();
    }

    @Override
    public String toString() {
        return "From: " + checkIn.getTime() + " to: " + checkOut.getTime();
    }

}
