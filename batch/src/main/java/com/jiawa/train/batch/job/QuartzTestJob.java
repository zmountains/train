package com.jiawa.train.batch.job;

import cn.hutool.core.date.DateTime;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class QuartzTestJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        DateTime now = DateTime.now();
        System.out.println("Quartz跑批任务："+now.toString());
    }
}
