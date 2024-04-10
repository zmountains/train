package com.jiawa.train.batch.feign;

import com.jiawa.train.common.resp.CommonResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Date;

@FeignClient(value = "business", fallback = BussinessFeignFallback.class)
//@FeignClient(name = "business", url = "http://127.0.0.1:8090/business")
public interface BusinessFeign {

    @GetMapping("/business/hello")
    String hello1();

    @GetMapping ("/business/admin/daily-train/gen-daily/{date}")
    CommonResp<Object> queryList(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date);
}
