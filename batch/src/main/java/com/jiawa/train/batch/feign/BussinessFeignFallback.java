package com.jiawa.train.batch.feign;

import com.jiawa.train.common.resp.CommonResp;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class BussinessFeignFallback implements BusinessFeign{
    @Override
    public String hello1() {
        return "fallback";
    }

    @Override
    public CommonResp<Object> queryList(Date date) {
        return null;
    }
}
