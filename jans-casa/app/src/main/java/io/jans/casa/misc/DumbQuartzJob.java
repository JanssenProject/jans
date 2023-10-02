package io.jans.casa.misc;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * A job that does nothing ({@link #execute(JobExecutionContext) execute} method empty). Very useful...
 * @author jgomer
 */
public class DumbQuartzJob implements Job {

    public void execute(JobExecutionContext context) throws JobExecutionException { }

}
