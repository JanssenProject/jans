/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.timer.event;

import java.lang.annotation.Annotation;

import io.jans.service.timer.schedule.TimerSchedule;

/**
 * @author Yuriy Movchan Date: 04/04/2017
 */
public final class TimerEvent {

    private final Object targetEvent;
    private final Annotation[] qualifiers;
    private TimerSchedule schedule;

    public TimerEvent(TimerSchedule schedule, Object targetEvent, Annotation... qualifiers) {
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule must not be null");
        }

        if (targetEvent == null) {
            throw new IllegalArgumentException("Target event must not be null");
        }

        this.schedule = schedule;
        this.targetEvent = targetEvent;
        this.qualifiers = qualifiers;
    }

    public TimerSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(TimerSchedule schedule) {
        this.schedule = schedule;
    }

    public Object getTargetEvent() {
        return targetEvent;
    }

    public Annotation[] getQualifiers() {
        return qualifiers;
    }

}
