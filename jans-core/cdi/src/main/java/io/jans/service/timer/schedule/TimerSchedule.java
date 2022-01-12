/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.timer.schedule;

/**
 * @author Yuriy Movchan Date: 04/13/2017
 */
public class TimerSchedule {

    private int delay;
    private int interval;

    /**
     * @param delay
     *            the delay before the first event occurs
     * @param interval
     *            the period between the events
     */
    public TimerSchedule(int delay, int interval) {
        this.delay = delay;
        this.interval = interval;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

}
