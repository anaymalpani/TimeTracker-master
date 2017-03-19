package com.alacritic.timetracker.period;

import java.io.Serializable;
import java.util.Calendar;

public interface Period extends Serializable {
    Calendar getFrom();

    Calendar getTo();

    String getTitle();

    Period getNext();

    Period getPrevious();

    long getBillableDuration();

    long getPreferredBillableDuration();
}
