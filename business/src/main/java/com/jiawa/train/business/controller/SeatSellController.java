package com.jiawa.train.business.controller;

import com.jiawa.train.business.req.SeatSellReq;
import com.jiawa.train.business.resp.SeatSellResp;
import com.jiawa.train.business.service.DailyTrainSeatService;
import com.jiawa.train.common.resp.CommonResp;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/seat-sell")
public class SeatSellController {
    @Resource
    private DailyTrainSeatService dailyTrainSeatService;

    @GetMapping ("/query")
    public CommonResp<List<SeatSellResp>> query(@Validated SeatSellReq req){
        List<SeatSellResp> list = dailyTrainSeatService.querySeatSell(req);
        return new CommonResp<>(list);
    }

}
