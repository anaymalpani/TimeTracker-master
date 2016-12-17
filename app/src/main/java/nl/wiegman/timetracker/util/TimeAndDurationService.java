package nl.wiegman.timetracker.util;

import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.List;

import nl.wiegman.timetracker.domain.TimeRecord;
import nl.wiegman.timetracker.period.Period;

public class TimeAndDurationService {
    public static boolean isCheckedIn() {
        return getCheckIn() != null;
    }

    public static TimeRecord getCheckIn() {
        List<TimeRecord> checkIn = TimeRecord.find(TimeRecord.class, "check_out is null");
        TimeRecord result = null;
        if (!checkIn.isEmpty()) {
            result = checkIn.get(0);
        }
        return result;
    }

    public static List<TimeRecord> getTimeRecordsBetween(Calendar from, Calendar to) {
        String fromTimeInMillis = Long.toString(from.getTimeInMillis());
        String toTimeInMillis = Long.toString(to.getTimeInMillis());
        return TimeRecord.find(TimeRecord.class, "check_in > ? AND check_in < ? order by check_in", fromTimeInMillis, toTimeInMillis);
    }

    public static TimeRecord checkIn() {
        TimeRecord timeRecord = new TimeRecord();
        Calendar checkIn = Calendar.getInstance();
        checkIn.set(Calendar.MILLISECOND, 0);
        timeRecord.setCheckIn(checkIn);
        timeRecord.save();
        return timeRecord;
    }

    public static TimeRecord checkOut() {
        TimeRecord timeRecord = TimeAndDurationService.getCheckIn();

        Calendar checkOutTimestamp = Calendar.getInstance();
        checkOutTimestamp.set(Calendar.MILLISECOND, 0);

        timeRecord.setCheckOut(checkOutTimestamp);

        if (!timeRecord.hasBreak()) {
            timeRecord.setDefaultBreak();
        }
        timeRecord.save();

        return timeRecord;
    }

    public static Calendar getStartOfWeek(Calendar dayInWeek) {
        Calendar startOfMonday = DateUtils.iterator(dayInWeek, DateUtils.RANGE_WEEK_MONDAY).next();
        startOfMonday = TimeAndDurationService.getStartOfDay(startOfMonday);
        return startOfMonday;
    }

    public static Calendar getEndOfWeek(Calendar dayInWeek) {
        Calendar endOfSunday = (Calendar) getStartOfWeek(dayInWeek).clone();
        endOfSunday.add(Calendar.WEEK_OF_YEAR, 1);
        endOfSunday.add(Calendar.MILLISECOND, -1);
        return endOfSunday;
    }

    public static Calendar getStartOfMonth(Calendar dayInMonth) {
        Calendar start = ((Calendar)dayInMonth.clone());
        start = DateUtils.truncate(start, Calendar.MONTH);
        return start;
    }

    public static Calendar getEndOfMonth(Calendar dayInMonth) {
        Calendar start = getStartOfMonth(dayInMonth);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        end.add(Calendar.MILLISECOND, -1);
        return end;
    }

    public static Calendar getStartOfDay(Calendar day) {
        Calendar startOfDay = (Calendar) day.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        return startOfDay;
    }

    public static Calendar getEndOfDay(Calendar day) {
        Calendar endOfDay = (Calendar) day.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 0);
        return endOfDay;
    }

    public static Calendar getFirstDayOfYear(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.YEAR, year);
        return calendar;
    }

    public static Calendar getLastDayOfYear(int year) {
        Calendar calendar = (Calendar) getFirstDayOfYear(year).clone();
        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        return calendar;
    }

}
