package com.jiawa.train.business.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.service.ConfirmOrderService;
import com.jiawa.train.common.exception.BussinessExceptionEnum;
import com.jiawa.train.common.resp.CommonResp;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/confirm-order")
public class ConfirmOrderController {
    @Resource
    private ConfirmOrderService confirmOrderService;

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);

    @SentinelResource(value = "doConfirmDo", blockHandler = "doConfirmBlock")
    @PostMapping ("/do")
    public CommonResp<Object> doConfirm(@RequestBody @Validated ConfirmOrderDoReq req){
        confirmOrderService.doConfirm(req);
        return new CommonResp<>();
    }

    /**
     * 降级方法，需包含限流方法的所有参数和BlockException参数
     * @param req
     * @param e
     */
    private CommonResp<Object> doConfirmBlock(ConfirmOrderDoReq req, BlockException e){
        LOG.info("ConfirmOrderController购票请求被限流：{}", req);
        CommonResp<Object> commonResp = new CommonResp<>();
        commonResp.setSuccess(false);
        commonResp.setMessage(BussinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION.getDesc());
        return commonResp;
    }
}
