package org.gluu.service.timer.schedule;

import org.quartz.JobDetail;
import org.quartz.Trigger;

/**
 * @author Yuriy Movchan Date: 04/04/2017
 */
public class JobShedule {

    private JobDetail jobDetail;
    private Trigger trigger;

    public JobShedule(JobDetail jobDetail, Trigger trigger) {
        this.jobDetail = jobDetail;
        this.trigger = trigger;
    }

    public JobDetail getJobDetail() {
        return jobDetail;
    }

    public void setJobDetail(JobDetail jobDetail) {
        this.jobDetail = jobDetail;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

}
