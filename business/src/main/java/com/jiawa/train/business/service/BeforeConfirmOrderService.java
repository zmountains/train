package com.jiawa.train.business.service;

import cn.hutool.core.date.DateTime;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.jiawa.train.business.domain.ConfirmOrder;
import com.jiawa.train.business.enums.ConfirmOrderStatusEnum;
import com.jiawa.train.business.enums.RocketMQTopicEnum;
import com.jiawa.train.business.mapper.ConfirmOrderMapper;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.common.context.LoginMemberContext;
import com.jiawa.train.common.exception.BussinessException;
import com.jiawa.train.common.exception.BussinessExceptionEnum;
import com.jiawa.train.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class BeforeConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(BeforeConfirmOrderService.class);

    @Resource
    public RocketMQTemplate rocketMQTemplate;

    @Resource
    private SkTokenService skTokenService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;


    //@SentinelResource("doConfirm")
    @SentinelResource(value = "beforeDoConfirm", blockHandler = "beforeDoConfirmBlock")
    public void beforeDoConfirm(ConfirmOrderDoReq req){
        // 校验令牌余量
        boolean validSkToken = skTokenService.validSkToken(req.getDate(), req.getTrainCode(), LoginMemberContext.getId());
        if (validSkToken) {
            LOG.info("令牌校验通过");
        } else {
            LOG.info("令牌校验不通过");
            throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_SK_TOKEN_FAIL);
        }

//        String key = RedisKeyPreEnum.CONFIRM_ORDER + "-" + req.getDate() + "-" + req.getTrainCode();
        // Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(key, key, 5, TimeUnit.SECONDS);
        // if(lock){
        //     LOG.info("恭喜，抢到锁了！");
        // } else {
        //     LOG.info("很遗憾，没抢到锁");
        //     throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_LOCK_FAIL);
        // }
//        RLock lock = null;
//        try {
//            //使用redisson，自带看门狗
//            lock = redissonClient.getLock(key);
//           /* waitTime ——  the maximum time to acquire the lock 等待获取锁时间（最大尝试获得锁时间），超时返回false
//            leaseTime ——  lease time 锁时长，即n秒后自动释放锁
//            time unit ——  time unit 时间单位   */
//            // boolean tryLock = lock.tryLock(0, 10, TimeUnit.SECONDS); //不带看门狗
//            boolean tryLock = lock.tryLock(0, TimeUnit.SECONDS); //带看门狗
//            if(tryLock){
//                LOG.info("恭喜，抢到锁了！");
//                for (int i = 0; i < 30; i++) {
//                    Long expire = stringRedisTemplate.opsForValue().getOperations().getExpire(key);
//                    LOG.info("锁过期时间还有：{}", expire);
//                    Thread.sleep(1000);
//                }
//            } else {
//                //只是没抢到锁，并不知道票抢完了没，所以提示稍后再试
//                LOG.info("很遗憾，没抢到锁");
//                throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_LOCK_FAIL);
//            }
        Date date = req.getDate();
        String trainCode = req.getTrainCode();
        // 保存确认订单表，状态初始
        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = new ConfirmOrder();
        confirmOrder.setId(SnowUtil.getSnowflakeNextId());
        confirmOrder.setMemberId(LoginMemberContext.getId());
        confirmOrder.setDate(date);
        confirmOrder.setTrainCode(trainCode);
        confirmOrder.setStart(req.getStart());
        confirmOrder.setEnd(req.getEnd());
        confirmOrder.setDailyTrainTicketId(req.getDailyTrainTicketId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
        confirmOrder.setCreateTime(now);
        confirmOrder.setUpdateTime(now);
        confirmOrder.setTickets(JSON.toJSONString(req.getTickets()));

        confirmOrderMapper.insert(confirmOrder);

        // 发送MQ排队购票
        req.setMemberId(LoginMemberContext.getId());
        String reqJson = JSON.toJSONString(req);
        LOG.info("排队购票，发送mq开始，消息：{}", reqJson);
        rocketMQTemplate.convertAndSend(RocketMQTopicEnum.CONFIRM_ORDER.getCode(), reqJson);
        LOG.info("排队购票，发送mq结束");

//        } catch (InterruptedException e) {
//            LOG.error("购票异常",e);
//        }
    }

    /**
     * 降级方法，需包含限流方法的所有参数和BlockException参数
     * @param req
     * @param e
     */
    private void beforeDoConfirmBlock(ConfirmOrderDoReq req, BlockException e){
        LOG.info("购票请求被限流：{}", req);
        throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }
}
