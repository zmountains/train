package com.jiawa.train.business.controller;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.service.BeforeConfirmOrderService;
import com.jiawa.train.business.service.ConfirmOrderService;
import com.jiawa.train.common.exception.BussinessExceptionEnum;
import com.jiawa.train.common.resp.CommonResp;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/confirm-order")
public class ConfirmOrderController {
    @Resource
    private BeforeConfirmOrderService beforeConfirmOrderService;

    @Resource
    private ConfirmOrderService confirmOrderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${spring.profiles.active}")
    private String env;

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);

    @SentinelResource(value = "doConfirmDo", blockHandler = "doConfirmBlock")
    @PostMapping ("/do")
    public CommonResp<Object> doConfirm(@RequestBody @Validated ConfirmOrderDoReq req){
        if (!env.equals("dev")) {
            //图形验证码校验
            String imageCodeToken = req.getImageCodeToken();
            String imageCode = req.getImageCode();
            String imageCodeRedis = stringRedisTemplate.opsForValue().get(imageCodeToken);
            LOG.info("从redis中获取到的验证码：{}",imageCodeRedis);
            if(ObjectUtil.isEmpty(imageCodeRedis)){
                return new CommonResp<>(false, "验证码已过期", null);
            }
            //验证码校验，大小写忽略，提升体验
            if(!imageCodeRedis.equalsIgnoreCase(imageCode)) {
                return new CommonResp<>(false, "验证码不正确", null);
            } else {
                //验证通过之后，移除验证码
                stringRedisTemplate.delete(imageCodeToken);
            }

        }
        Long id = beforeConfirmOrderService.beforeDoConfirm(req);
        return new CommonResp<>(String.valueOf(id));
    }

    @GetMapping("/query-line-count/{id}")
    public CommonResp<Integer> queryLineCount(@PathVariable Long id){
        Integer count = confirmOrderService.queryLineCount(id);
        return new CommonResp<>(count);
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
