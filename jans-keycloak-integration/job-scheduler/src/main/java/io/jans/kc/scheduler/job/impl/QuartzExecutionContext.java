package io.jans.kc.scheduler.job.impl;

import io.jans.kc.scheduler.job.ExecutionContext;
import org.quartz.JobDataMap;

public class QuartzExecutionContext implements ExecutionContext{
    
    private JobDataMap datamap;

    public QuartzExecutionContext(JobDataMap datamap) {

        this.datamap = datamap;
    }

    @Override
    public String getStringValue(String key) {

        return this.datamap.getString(key);
    }
}
