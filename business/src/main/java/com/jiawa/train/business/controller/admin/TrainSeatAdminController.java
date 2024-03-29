package com.jiawa.train.business.controller.admin;

import com.jiawa.train.business.req.TrainSeatQueryReq;
import com.jiawa.train.business.req.TrainSeatSaveReq;
import com.jiawa.train.business.resp.TrainSeatQueryResp;
import com.jiawa.train.business.service.TrainSeatService;
import com.jiawa.train.common.resp.CommonResp;
import com.jiawa.train.common.resp.PageResp;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/train-seat")
public class TrainSeatAdminController {
    @Resource
    private TrainSeatService trainSeatService;

    @PostMapping ("/save")
    public CommonResp<Object> save(@RequestBody @Validated TrainSeatSaveReq req){
        trainSeatService.save(req);
        return new CommonResp<>();
    }

    @GetMapping ("/query-list")
    public CommonResp<PageResp<TrainSeatQueryResp>> queryList(@Validated TrainSeatQueryReq req){
        PageResp<TrainSeatQueryResp> list = trainSeatService.queryList(req);
        return new CommonResp<>(list);
    }

    @DeleteMapping ("/delete/{id}")
    public CommonResp<Object> queryList(@PathVariable Long id){
       trainSeatService.delete(id);
        return new CommonResp<>();
    }
}
