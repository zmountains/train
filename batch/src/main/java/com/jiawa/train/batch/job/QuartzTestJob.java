package com.jiawa.train.batch.job;

import cn.hutool.core.date.DateTime;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


@DisallowConcurrentExecution
public class QuartzTestJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        DateTime begin = DateTime.now();
        System.out.println("Quartz跑批任务开始："+begin.toString());

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        DateTime end = DateTime.now();
        System.out.println("Quartz跑批任务结束："+end.toString());
    }
}
