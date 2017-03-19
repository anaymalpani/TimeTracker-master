package com.alacritic.timetracker.util;

import org.apache.commons.lang3.time.DurationFormatUtils;

public class Formatting {

    public static String formatDuration(long duration) {
        return DurationFormatUtils.formatDuration(Math.abs(duration), "H:mm:ss");
    }
}
