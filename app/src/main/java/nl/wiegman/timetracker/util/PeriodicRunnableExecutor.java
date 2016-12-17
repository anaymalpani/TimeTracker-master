package nl.wiegman.timetracker.util;

import android.util.Log;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PeriodicRunnableExecutor {
    private final String LOG_TAG = this.getClass().getSimpleName();

    private final long rateInMilliseconds;

    private ScheduledExecutorService executorService;

    private final Runnable runnable;

    public PeriodicRunnableExecutor(long rateInMilliseconds, Runnable runnable) {
        this.runnable = runnable;
        this.rateInMilliseconds = rateInMilliseconds;
    }

    public void start() {
        if (executorService == null) {
            executorService = new ScheduledThreadPoolExecutor(1);
            int startDelay = 0;
            executorService.scheduleWithFixedDelay(runnable, startDelay, rateInMilliseconds, TimeUnit.MILLISECONDS);
        }
    }

    public void stop() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
                executorService = null;
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Periodic updater was not stopped within the timeout period");
            }
        }
    }
}
