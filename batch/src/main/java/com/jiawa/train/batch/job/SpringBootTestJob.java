package com.jiawa.train.batch.job;

import cn.hutool.core.date.DateTime;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class SpringBootTestJob {

    @Scheduled(cron = "0/5 * * * * ?")
    private void test() {
        DateTime now = DateTime.now();
        System.out.println("跑批任务："+now.toString());
    }
}