package com.jiawa.train.business.controller.admin;

import com.jiawa.train.business.req.DailyTrainQueryReq;
import com.jiawa.train.business.req.DailyTrainSaveReq;
import com.jiawa.train.business.resp.DailyTrainQueryResp;
import com.jiawa.train.business.service.DailyTrainService;
import com.jiawa.train.common.resp.CommonResp;
import com.jiawa.train.common.resp.PageResp;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/admin/daily-train")
public class DailyTrainAdminController {
    @Resource
    private DailyTrainService dailyTrainService;

    @PostMapping ("/save")
    public CommonResp<Object> save(@RequestBody @Validated DailyTrainSaveReq req){
        dailyTrainService.save(req);
        return new CommonResp<>();
    }

    @GetMapping ("/query-list")
    public CommonResp<PageResp<DailyTrainQueryResp>> queryList(@Validated DailyTrainQueryReq req){
        PageResp<DailyTrainQueryResp> list = dailyTrainService.queryList(req);
        return new CommonResp<>(list);
    }

    @DeleteMapping ("/delete/{id}")
    public CommonResp<Object> queryList(@PathVariable Long id){
       dailyTrainService.delete(id);
        return new CommonResp<>();
    }

    @GetMapping ("/gen-daily/{date}")
    public CommonResp<Object> queryList(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date){
        dailyTrainService.genDaily(date);
        return new CommonResp<>();
    }
}
