package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.SkToken;
import com.jiawa.train.business.domain.SkTokenExample;
import com.jiawa.train.business.enums.RedisKeyPreEnum;
import com.jiawa.train.business.mapper.SkTokenMapper;
import com.jiawa.train.business.mapper.cust.SkTokenMapperCust;
import com.jiawa.train.business.req.SkTokenQueryReq;
import com.jiawa.train.business.req.SkTokenSaveReq;
import com.jiawa.train.business.resp.SkTokenQueryResp;
import com.jiawa.train.common.exception.BussinessException;
import com.jiawa.train.common.exception.BussinessExceptionEnum;
import com.jiawa.train.common.resp.PageResp;
import com.jiawa.train.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SkTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(SkTokenService.class);
    @Resource
    private SkTokenMapper skTokenMapper;

    @Resource
    private SkTokenMapperCust skTokenMapperCust;

    @Resource
    private DailyTrainSeatService dailyTrainSeatService;

    @Resource
    private DailyTrainStationService dailyTrainStationService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void save(SkTokenSaveReq req){
        DateTime now = DateTime.now();
        SkToken skToken = BeanUtil.copyProperties(req, SkToken.class);
        if(ObjectUtil.isNull(skToken.getId())) {
            skToken.setId(SnowUtil.getSnowflakeNextId());
            skToken.setCreateTime(now);
            skToken.setUpdateTime(now);
            skTokenMapper.insert(skToken);
        }else{
            skToken.setUpdateTime(now);
            skTokenMapper.updateByPrimaryKey(skToken);
        }
    }

    public PageResp<SkTokenQueryResp> queryList(SkTokenQueryReq req){
        SkTokenExample skTokenExample = new SkTokenExample();
        skTokenExample.setOrderByClause("id desc");
        SkTokenExample.Criteria criteria = skTokenExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(),req.getSize());
        List<SkToken> skTokenList = skTokenMapper.selectByExample(skTokenExample);

        PageInfo<SkToken> pageInfo = new PageInfo<>(skTokenList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<SkTokenQueryResp> list = BeanUtil.copyToList(skTokenList, SkTokenQueryResp.class);

        PageResp<SkTokenQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return  pageResp;
    }

    public void delete(Long id){
        skTokenMapper.deleteByPrimaryKey(id);
    }

    @Transactional
    public void genDaily(Date date, String trainCode) {
        LOG.info("生成日期【{}】车次【{}】的令牌记录", DateUtil.formatDate(date), trainCode);

        // 删除某日某车次的令牌信息
        SkTokenExample skTokenExample = new SkTokenExample();
        skTokenExample.createCriteria()
                .andDateEqualTo(date)
                .andTrainCodeEqualTo(trainCode);
        skTokenMapper.deleteByExample(skTokenExample);

        DateTime now = DateTime.now();
        SkToken skToken = new SkToken();
        skToken.setId(SnowUtil.getSnowflakeNextId());
        skToken.setDate(date);
        skToken.setTrainCode(trainCode);
        skToken.setCreateTime(now);
        skToken.setUpdateTime(now);

        //该日期车次下所有的座位，包括所有类型的座位
        int seatCount = dailyTrainSeatService.countSeat(date, trainCode);

        //该日期车次下所有的站
        int stationCount = dailyTrainStationService.countStation(date, trainCode);

        //需要根据实际卖票比例来定，一趟火车最多可以卖（seatCount * stationCount）张火车票
        int count = seatCount * stationCount * 3/4;
        LOG.info("车次【{}】初始生成令牌数：{}", trainCode, count);
        skToken.setCount(count);

        skTokenMapper.insert(skToken);
    }

    public boolean validSkToken(Date date, String trainCode, Long memberId){
        LOG.info("会员【{}】获取日期【{}】车次【{}】的令牌开始", memberId, DateUtil.formatDate(date), trainCode);

        //先获取令牌锁，再校验令牌余量，防止机器人抢票，lockKey就是令牌，用来表示【谁能做什么】的一个凭证
        String lockKey = RedisKeyPreEnum.SK_TOKEN + "-" + DateUtil.formatDate(date) + "-" + trainCode + "-" + memberId;
        RLock lock = null;
        //使用redisson
        lock = redissonClient.getLock(lockKey);
       /* waitTime ——  the maximum time to acquire the lock 等待获取锁时间（最大尝试获得锁时间），超时返回false
        leaseTime ——  lease time 锁时长，即n秒后自动释放锁
        time unit ——  time unit 时间单位   */
        try {
            boolean tryLock = lock.tryLock(0, 5, TimeUnit.SECONDS);; //不带看门狗
            //boolean tryLock = lock.tryLock(0, TimeUnit.SECONDS); //带看门狗
            if(tryLock){
                LOG.info("恭喜，抢到令牌锁了！lockKey：{}",lockKey);
            } else {
                //只是没抢到锁，并不知道票抢完了没，所以提示稍后再试
                LOG.info("很遗憾，没抢到令牌锁");
                return false;
            }
        } catch (InterruptedException e) {
            LOG.error("购票异常",e);
        }

        //购票
        String skTokenCountKey = RedisKeyPreEnum.SK_TOKEN_COUNT + "-" + DateUtil.formatDate(date) + "-" + trainCode;
        Object skTokenCount = redisTemplate.opsForValue().get(skTokenCountKey);
        if(skTokenCount != null){
            LOG.info("缓存中有该车次令牌大闸的key：{}", skTokenCountKey);
            Long count = redisTemplate.opsForValue().decrement(skTokenCountKey, 1);
            if(count < 0L){
                LOG.error("获取令牌失败：{}", skTokenCountKey);
                throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_SK_TOKEN_FAIL);
            }else {
                LOG.info("获取令牌后，令牌余数：{}", count);
                redisTemplate.expire(skTokenCountKey, 60, TimeUnit.SECONDS);
                if(count % 5 == 0){
                    skTokenMapperCust.decrease(date, trainCode, 5);
                }
                return true;
            }
        }else {
            LOG.info("缓存中没有该车次令牌大闸的key：{}", skTokenCountKey);
            //检查是否还有令牌
            SkTokenExample skTokenExample = new SkTokenExample();
            skTokenExample.createCriteria()
                    .andDateEqualTo(date)
                    .andTrainCodeEqualTo(trainCode);
            List<SkToken> tokenCountList = skTokenMapper.selectByExample(skTokenExample);
            if(CollUtil.isEmpty(tokenCountList)){
                LOG.info("找不到日期【{}】车次【{}】的令牌记录", DateUtil.formatDate(date), trainCode);
                return false;
            }
            SkToken skToken = tokenCountList.get(0);
            if(skToken.getCount() <= 0){
                LOG.info("日期【{}】车次【{}】的令牌余量为0", DateUtil.formatDate(date), trainCode);
                return false;
            }

            //令牌还有余量
            //令牌余数-1
            Integer count = skToken.getCount() - 1;
            skToken.setCount(count);
            LOG.info("将该车次令牌大闸放入缓存中，key：{}，count：{}", skTokenCountKey, count);
            redisTemplate.opsForValue().set(skTokenCountKey, String.valueOf(count), 60, TimeUnit.SECONDS);
//            skTokenMapper.updateByPrimaryKey(skToken);
            return true;
        }
//
//        // 令牌约等于库存，令牌没有了，就不再卖票，不需要再进入购票主流程去判断库存，判断令牌肯定比判断库存效率高
//        int updateCount = skTokenMapperCust.decrease(date, trainCode);
//        if (updateCount > 0) {
//            return true;
//        } else {
//            return false;
//        }
    }
}
