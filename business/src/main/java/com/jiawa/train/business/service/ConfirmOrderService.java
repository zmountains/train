package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.*;
import com.jiawa.train.business.dto.ConfirmOrderMQDto;
import com.jiawa.train.business.enums.ConfirmOrderStatusEnum;
import com.jiawa.train.business.enums.RedisKeyPreEnum;
import com.jiawa.train.business.enums.SeatColEnum;
import com.jiawa.train.business.enums.SeatTypeEnum;
import com.jiawa.train.business.mapper.ConfirmOrderMapper;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.req.ConfirmOrderQueryReq;
import com.jiawa.train.business.req.ConfirmOrderTicketReq;
import com.jiawa.train.business.resp.ConfirmOrderQueryResp;
import com.jiawa.train.common.exception.BussinessException;
import com.jiawa.train.common.exception.BussinessExceptionEnum;
import com.jiawa.train.common.resp.PageResp;
import com.jiawa.train.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);
    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    @Resource
    private DailyTrainTicketService dailyTrainTicketService;

    @Resource
    private DailyTrainCarriageService dailyTrainCarriageService;

    @Resource
    private DailyTrainSeatService dailyTrainSeatService;

    @Resource
    private AfterConfirmOrderService afterConfirmOrderService;

    @Resource
    private SkTokenService skTokenService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    public void save(ConfirmOrderDoReq req) {
        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = BeanUtil.copyProperties(req, ConfirmOrder.class);
        if (ObjectUtil.isNull(confirmOrder.getId())) {
            confirmOrder.setId(SnowUtil.getSnowflakeNextId());
            confirmOrder.setCreateTime(now);
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.insert(confirmOrder);
        } else {
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.updateByPrimaryKey(confirmOrder);
        }
    }

    public PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req) {
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
        confirmOrderExample.setOrderByClause("id desc");
        ConfirmOrderExample.Criteria criteria = confirmOrderExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<ConfirmOrder> confirmOrderList = confirmOrderMapper.selectByExample(confirmOrderExample);

        PageInfo<ConfirmOrder> pageInfo = new PageInfo<>(confirmOrderList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<ConfirmOrderQueryResp> list = BeanUtil.copyToList(confirmOrderList, ConfirmOrderQueryResp.class);

        PageResp<ConfirmOrderQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        confirmOrderMapper.deleteByPrimaryKey(id);
    }


    @Async
    //@SentinelResource("doConfirm")
    @SentinelResource(value = "doConfirm", blockHandler = "doConfirmBlock")
    public void doConfirm(ConfirmOrderMQDto dto) {
        MDC.put("LOG_ID",dto.getLogId());
        LOG.info("异步出票开始：{}", dto);
//        // 校验令牌余量
//        boolean validSkToken = skTokenService.validSkToken(req.getDate(), req.getTrainCode(), LoginMemberContext.getId());
//        if (validSkToken) {
//            LOG.info("令牌校验通过");
//        } else {
//            LOG.info("令牌校验不通过");
//            throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_SK_TOKEN_FAIL);
//        }
//
        String key = RedisKeyPreEnum.CONFIRM_ORDER + "-" + dto.getDate() + "-" + dto.getTrainCode();
        // Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(key, key, 5, TimeUnit.SECONDS);
        // if(lock){
        //     LOG.info("恭喜，抢到锁了！");
        // } else {
        //     LOG.info("很遗憾，没抢到锁");
        //     throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_LOCK_FAIL);
        // }

        RLock lock = null;
        try {
            //使用redisson，自带看门狗
            lock = redissonClient.getLock(key);
           /* waitTime ——  the maximum time to acquire the lock 等待获取锁时间（最大尝试获得锁时间），超时返回false
            leaseTime ——  lease time 锁时长，即n秒后自动释放锁
            time unit ——  time unit 时间单位   */
            // boolean tryLock = lock.tryLock(0, 10, TimeUnit.SECONDS); //不带看门狗
            boolean tryLock = lock.tryLock(0, TimeUnit.SECONDS); //带看门狗
            if (tryLock) {
                LOG.info("恭喜，抢到锁了！");
//                for (int i = 0; i < 30; i++) {
//                    Long expire = stringRedisTemplate.opsForValue().getOperations().getExpire(key);
//                    LOG.info("锁过期时间还有：{}", expire);
//                    Thread.sleep(1000);
//                }
            } else {
                //只是没抢到锁，并不知道票抢完了没，所以提示稍后再试
                //LOG.info("很遗憾，没抢到锁");
                //throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_LOCK_FAIL);

                LOG.info("没抢到锁，有其他消费线程正在出票，不做任何处理");
                return;
            }

            while (true) {
                //取出订单表记录，同日期车次，状态是INIT，分页处理，每次取N条
                ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
                confirmOrderExample.setOrderByClause("id asc");
                confirmOrderExample.createCriteria()
                        .andDateEqualTo(dto.getDate())
                        .andTrainCodeEqualTo(dto.getTrainCode())
                        .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
                PageHelper.startPage(1, 5);
                List<ConfirmOrder> list = confirmOrderMapper.selectByExampleWithBLOBs(confirmOrderExample);

                if (CollUtil.isEmpty(list)) {
                    LOG.info("没有需要处理的订单，结束循环");
                    break;
                } else {
                    LOG.info("本次处理{}条确认订单", list.size());
                }

                list.forEach(confirmOrder -> {
                    try {
                        sell(confirmOrder);
                    } catch (BussinessException e) {
                        if (e.getE().equals(BussinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR)) {
                            LOG.info("本订单余票不足，继续售卖下一个订单");
                            confirmOrder.setStatus(ConfirmOrderStatusEnum.EMPTY.getCode());
                            updateStatus(confirmOrder);
                        } else {
                            throw e;
                        }
                    }
                });
            }
        } catch (InterruptedException e) {
            LOG.error("购票异常", e);
        } finally {
            LOG.info("购票流程结束，释放锁！key:{}", key);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void updateStatus(ConfirmOrder confirmOrder) {
        ConfirmOrder confirmOrderForUpdate = new ConfirmOrder();
        confirmOrderForUpdate.setId(confirmOrder.getId());
        confirmOrderForUpdate.setStatus(confirmOrder.getStatus());
        confirmOrderForUpdate.setUpdateTime(new Date());
        confirmOrderMapper.updateByPrimaryKeySelective(confirmOrderForUpdate);
    }

    private void sell(ConfirmOrder confirmOrder) {
        //构造ConfirmOrderDoReq
        ConfirmOrderDoReq req = new ConfirmOrderDoReq();
        req.setMemberId(confirmOrder.getMemberId());
        req.setDate(confirmOrder.getDate());
        req.setTrainCode(confirmOrder.getTrainCode());
        req.setStart(confirmOrder.getStart());
        req.setEnd(confirmOrder.getEnd());
        req.setDailyTrainTicketId(confirmOrder.getDailyTrainTicketId());
        req.setTickets(JSON.parseArray(confirmOrder.getTickets(), ConfirmOrderTicketReq.class));
        req.setImageCode("");
        req.setImageCodeToken("");
        req.setLogId("");

        // 将订单设置成处理中，避免重复处理
        LOG.info("将确认订单更新成处理中，避免重复处理，confirm_order.id: {}", confirmOrder.getId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.PENDING.getCode());
        updateStatus(confirmOrder);

        // 省略业务数据校验，如：车次是否存在，余票是否存在，车次是否在有效期内，tickets条数>0，同乘客同车次是否已买过
        Date date = req.getDate();
        String trainCode = req.getTrainCode();
        // 查出余票记录，需要得到真实的库存
        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(req.getDate(), req.getTrainCode(), req.getStart(), req.getEnd());
        LOG.info("查出余票记录：{}", dailyTrainTicket);

        // 预扣减余票数量，并判断余票是否足够
        reduceTickets(req, dailyTrainTicket);

        //最终选座结果
        List<DailyTrainSeat> finalSeatList = new ArrayList<>();
        //计算相对第一个座位的偏移值
        //比如选择的是C1、D2，则偏移值是：[0,5]
        //比如选择的是A1,B1,C1，则偏移值是：[0,1,2]
        List<ConfirmOrderTicketReq> tickets = req.getTickets();
        String seat = tickets.get(0).getSeat();
        if (StrUtil.isNotBlank(seat)) {
            LOG.info("本次购票有选座");
            //查出本次选座的作为类型都有哪些，用于计算所选座位与第一个座位的偏移值
            List<SeatColEnum> seatColEnums = SeatColEnum.getColsByType(tickets.get(0).getSeatTypeCode());
            LOG.info("本次选座的座位类型包含的列：{}", seatColEnums);

            //组成和前端两排选座一样的列表，用于作参照的座位列表，例：referSeatList = {A1,C1,D1,F1,A2,C2,D2,F2}
            List<String> referSeatList = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                for (SeatColEnum seatColEnum : seatColEnums) {
                    referSeatList.add(seatColEnum.getCode() + i);
                }
            }
            LOG.info("用于作参照的两排座位：{}", referSeatList);

            //绝对偏移值, 即：在参照座位列表中的位置
            List<Integer> offsetList = new ArrayList<>();
            List<Integer> absoluteOffset = new ArrayList<>();
            for (ConfirmOrderTicketReq confirmOrderTicketReq : tickets) {
                absoluteOffset.add(referSeatList.indexOf(confirmOrderTicketReq.getSeat()));
            }
            LOG.info("计算得到所有座位的绝对偏移值：{}", absoluteOffset);
            for (Integer i : absoluteOffset) {
                offsetList.add(i - absoluteOffset.get(0));
            }
            LOG.info("计算得到所有座位的相对偏移值：{}", offsetList);
            getSeat(finalSeatList, date, trainCode, tickets.get(0).getSeatTypeCode(), seat.split("")[0], offsetList,
                    dailyTrainTicket.getStartIndex(), dailyTrainTicket.getEndIndex());
        } else {
            LOG.info("本次购票没有选座");
            for (ConfirmOrderTicketReq confirmOrderTicketReq : tickets) {
                getSeat(finalSeatList, date, trainCode, confirmOrderTicketReq.getSeatTypeCode(), null, null,
                        dailyTrainTicket.getStartIndex(), dailyTrainTicket.getEndIndex());
            }
        }
        ;

        LOG.info("最终选座：{}", finalSeatList);

        try {
            afterConfirmOrderService.afterDoConfirm(dailyTrainTicket, finalSeatList, tickets, confirmOrder);
        } catch (Exception e) {
            LOG.error("保存购票信息失败", e);
            throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_EXCEPTION);
        }
    }

    /**
     * 挑座位，如果有选座则一次性挑完，否则一个个挑
     *
     * @param date
     * @param trainCode
     * @param seatType
     * @param column
     * @param offsetList
     */
    public void getSeat(List<DailyTrainSeat> finalSeatList, Date date, String trainCode, String seatType,
                        String column, List<Integer> offsetList, Integer startIndex, Integer endIndex) {
        List<DailyTrainSeat> getSeatList = new ArrayList<>();
        List<DailyTrainCarriage> carriageList = dailyTrainCarriageService.selectBySeatType(date, trainCode, seatType);
        LOG.info("共查出{}个符合条件的车厢", carriageList.size());

        // 一个车箱一个车箱的获取座位数据
        for (DailyTrainCarriage dailyTrainCarriage : carriageList) {
            getSeatList = new ArrayList<>();
            LOG.info("开始从车厢{}选座", dailyTrainCarriage.getIndex());
            List<DailyTrainSeat> seatList = dailyTrainSeatService.selectByCarriage(date, trainCode, dailyTrainCarriage.getIndex());
            LOG.info("车厢{}的座位数：{}", dailyTrainCarriage.getIndex(), seatList.size());
            for (DailyTrainSeat dailyTrainSeat : seatList) {
                Integer seatIndex = dailyTrainSeat.getCarriageSeatIndex();
                String col = dailyTrainSeat.getCol();

                //判断当前座位不能被选中过
                boolean alreadyChooseFlag = false;
                for (DailyTrainSeat finalSeat : finalSeatList) {
                    if (finalSeat.getId().equals(dailyTrainSeat.getId())) {
                        alreadyChooseFlag = true;
                        break;
                    }
                }
                if (alreadyChooseFlag) {
                    LOG.info("座位{}被选中过，不能重复选中，继续判断下一个座位", seatIndex);
                    continue;
                }

                //判断column，有值的话要比对列号
                if (StrUtil.isBlank(column)) {
                    LOG.info("无选座");
                } else {
                    if (!column.equals(col)) {
                        LOG.info("座位{}列值不对，继续判断下一个座位，当前列值：{}，目标列值：{}", seatIndex, col, column);
                        continue;
                    }
                }

                boolean isChoose = calSell(dailyTrainSeat, startIndex, endIndex);
                if (isChoose) {
                    LOG.info("选中座位");
                    getSeatList.add(dailyTrainSeat);
                } else {
                    continue;
                }

                //根据offset选剩下的座位
                boolean isGetAllOffsetSeat = true;
                if (CollUtil.isNotEmpty(offsetList)) {
                    LOG.info("有偏移值：{}，校验偏移的座位是否可选", offsetList);
                    //从索引1开始，索引0就是当前已选中的票
                    for (int i = 1; i < offsetList.size(); i++) {
                        Integer offset = offsetList.get(i);
                        int nextIndex = seatIndex + offset - 1;

                        //有选座时，一定是同一个车厢
                        if (nextIndex >= seatList.size()) {
                            LOG.info("座位{}不可选，偏移后的索引超出了这个车厢的座位数", nextIndex);
                            isGetAllOffsetSeat = false;
                            break;
                        }

                        DailyTrainSeat nextDailyTrainSeat = seatList.get(nextIndex);
                        boolean isChooseNext = calSell(nextDailyTrainSeat, startIndex, endIndex);
                        if (isChooseNext) {
                            LOG.info("座位{}被选中", nextDailyTrainSeat.getCarriageSeatIndex());
                            getSeatList.add(nextDailyTrainSeat);
                        } else {
                            LOG.info("座位{}不可选", nextDailyTrainSeat.getCarriageSeatIndex());
                            isGetAllOffsetSeat = false;
                            break;
                        }
                    }
                }
                if (!isGetAllOffsetSeat) {
                    getSeatList = new ArrayList<>();
                    continue;
                }

                //保存选好的座位
                finalSeatList.addAll(getSeatList);
                return;
            }
        }
    }

    /**
     * 计算座位在区间内是否可卖
     * 例：sell=10001，本次购买区间站1~4，则区间已售000
     * 全部是0，表示这个区间可买；只要有1，就表示区间内已售过票
     * <p>
     * 选中后，要计算购票后的sell，比如原来是10001本次购买区间站1~4
     * 方案：构造本次购票造成的售卖信息01110，和原sell 10001按位或，最终得到11111
     */
    public boolean calSell(DailyTrainSeat dailyTrainSeat, Integer startIndex, Integer endIndex) {
        //10001
        String sell = dailyTrainSeat.getSell();
        //000
        String sellPart = sell.substring(startIndex, endIndex);
        if (Integer.parseInt(sellPart) > 0) {
            LOG.info("座位{}在本次车站区间{}~{}已售过票，不可选中该座位", dailyTrainSeat.getCarriageSeatIndex(), startIndex, endIndex);
            return false;
        } else {
            LOG.info("座位{}在本次车站区间{}~{}未售过票，可选中该座位", dailyTrainSeat.getCarriageSeatIndex(), startIndex, endIndex);
            //111
            String curSell = sellPart.replace("0", "1");
            curSell = StrUtil.fillBefore(curSell, '0', endIndex);
            curSell = StrUtil.fillAfter(curSell, '0', sell.length());

            //当前区间售票信息curSell与库里的已售信息sell按位或，即可得到该座位卖出此票后的售票详情
            int newSellInt = NumberUtil.binaryToInt(curSell) | NumberUtil.binaryToInt(sell);
            ///11111
            String newSell = NumberUtil.getBinaryStr(newSellInt);
            newSell = StrUtil.fillBefore(newSell, '0', sell.length());
            LOG.info("座位{}被选中，原售票信息：{}，车站区间：{}~{}，即：{}，最终售票信息：{}"
                    , dailyTrainSeat.getCarriageSeatIndex(), sell, startIndex, endIndex, curSell, newSell);
            dailyTrainSeat.setSell(newSell);
            return true;
        }
    }

    private void reduceTickets(ConfirmOrderDoReq req, DailyTrainTicket dailyTrainTicket) {
        for (ConfirmOrderTicketReq confirmOrderTicketReq : req.getTickets()) {
            SeatTypeEnum seatTypeEnum = EnumUtil.getBy(SeatTypeEnum::getCode, confirmOrderTicketReq.getSeatTypeCode());
            switch (seatTypeEnum) {
                case YDZ -> {
                    int countLeft = dailyTrainTicket.getYdz() - 1;
                    if (countLeft < 0) {
                        throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    } else {
                        dailyTrainTicket.setYdz(countLeft);
                    }
                }
                case EDZ -> {
                    int countLeft = dailyTrainTicket.getEdz() - 1;
                    if (countLeft < 0) {
                        throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    } else {
                        dailyTrainTicket.setEdz(countLeft);
                    }
                }
                case RW -> {
                    int countLeft = dailyTrainTicket.getRw() - 1;
                    if (countLeft < 0) {
                        throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    } else {
                        dailyTrainTicket.setRw(countLeft);
                    }
                }
                case YW -> {
                    int countLeft = dailyTrainTicket.getYw() - 1;
                    if (countLeft < 0) {
                        throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    } else {
                        dailyTrainTicket.setYw(countLeft);
                    }
                }
            }
        }
    }

    /**
     * 降级方法，需包含限流方法的所有参数和BlockException参数
     *
     * @param req
     * @param e
     */
    private void doConfirmBlock(ConfirmOrderDoReq req, BlockException e) {
        LOG.info("购票请求被限流：{}", req);
        throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }

    public Integer queryLineCount(Long id) {
        ConfirmOrder confirmOrder = confirmOrderMapper.selectByPrimaryKey(id);
        ConfirmOrderStatusEnum statusEnum = EnumUtil.getBy(ConfirmOrderStatusEnum::getCode, confirmOrder.getStatus());
        int result = switch (statusEnum) {
            case PENDING -> 0; //排队0
            case SUCCESS -> -1; //成功
            case FAILURE -> -2; //失败
            case EMPTY -> -3; //无票
            case CANCEL -> -4; //取消
            case INIT -> 999; //需要查表得到实际排队的数量
        };

        if (result == 999) {
            // 排在第几位，下面的写法：where a=1 and (b=1 or c=1) 等价于 where (a=1 and b=1) or (a=1 and c=1)
            ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
            confirmOrderExample.or().andDateEqualTo(confirmOrder.getDate())
                    .andTrainCodeEqualTo(confirmOrder.getTrainCode())
                    .andCreateTimeLessThan(confirmOrder.getCreateTime())
                    .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
            confirmOrderExample.or().andDateEqualTo(confirmOrder.getDate())
                    .andTrainCodeEqualTo(confirmOrder.getTrainCode())
                    .andCreateTimeLessThan(confirmOrder.getCreateTime())
                    .andStatusEqualTo(ConfirmOrderStatusEnum.PENDING.getCode());
            return Math.toIntExact(confirmOrderMapper.countByExample(confirmOrderExample));
        }else {
            return result;
        }
    }

    public Integer cancle(Long id){
       ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
       confirmOrderExample.createCriteria()
               .andIdEqualTo(id)
               .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
       ConfirmOrder confirmOrder = new ConfirmOrder();
       confirmOrder.setStatus(ConfirmOrderStatusEnum.CANCEL.getCode());
       return confirmOrderMapper.updateByExampleSelective(confirmOrder,confirmOrderExample);
    }
}
