package com.jiawa.train.business.mq;

import com.alibaba.fastjson.JSON;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.service.ConfirmOrderService;
import jakarta.annotation.Resource;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(consumerGroup = "default", topic = "CONFIRM_ORDER")
public class ConfirmOrderConsumer implements RocketMQListener<MessageExt> {

    @Resource
    private ConfirmOrderService confirmOrderService;

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderConsumer.class);

    @Override
    public void onMessage(MessageExt messageExt) {
        byte[] body = messageExt.getBody();
        LOG.info("ROCKETMQ收到消息：{}", new String(body));

        ConfirmOrderDoReq req = JSON.parseObject(new String(body), ConfirmOrderDoReq.class);
        confirmOrderService.doConfirm(req);
    }
}

