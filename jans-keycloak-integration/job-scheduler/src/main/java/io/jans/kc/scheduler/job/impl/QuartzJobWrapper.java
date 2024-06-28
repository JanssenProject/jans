package io.jans.kc.scheduler.job.impl;

import java.lang.reflect.Constructor;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.kc.scheduler.App;
import io.jans.kc.scheduler.job.ExecutionContext;
import io.jans.kc.scheduler.job.impl.QuartzExecutionContext;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class QuartzJobWrapper implements Job {
    
    public static final String JOB_NAME_ENTRY_KEY = "jans.job.name";
    public static final String JOB_CLASS_ENTRY_KEY = "jans.job.class";
    public static final String JOB_EXECUTION_CONTEXT_ENTRY_KEY = "jobexecutioncontext";

    private static final Logger log = LoggerFactory.getLogger(QuartzJobWrapper.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
    
        String jobname = context.getMergedJobDataMap().getString(JOB_NAME_ENTRY_KEY);
        String jobclassname = context.getMergedJobDataMap().getString(JOB_CLASS_ENTRY_KEY);

        try {
            
            Class<?> jobclass = Class.forName(jobclassname);
            Constructor<?> constructor = jobclass.getConstructor();
            io.jans.kc.scheduler.job.Job job = (io.jans.kc.scheduler.job.Job) constructor.newInstance();
            ExecutionContext effectivecontext = new QuartzExecutionContext(context.getMergedJobDataMap());
            job.run(effectivecontext);
        }catch(Exception e) {
            throw new JobExecutionException("Failed to run job " + jobname,e);
        }
    }
}
