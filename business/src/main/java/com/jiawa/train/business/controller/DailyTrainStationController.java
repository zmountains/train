package com.jiawa.train.business.controller;

import com.jiawa.train.business.req.DailyTrainStationQueryAllReq;
import com.jiawa.train.business.resp.DailyTrainStationQueryResp;
import com.jiawa.train.business.service.DailyTrainStationService;
import com.jiawa.train.common.resp.CommonResp;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/daily-train-station")
public class DailyTrainStationController {
    @Resource
    private DailyTrainStationService dailyTrainStationService;


    @GetMapping ("/query-by-train-code")
    public CommonResp<List<DailyTrainStationQueryResp>> queryByTrain(@Validated DailyTrainStationQueryAllReq req){
        List<DailyTrainStationQueryResp> list = dailyTrainStationService.queryByTrain(req.getDate(), req.getTrainCode());
        return new CommonResp<>(list);
    }

}
