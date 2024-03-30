package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.ConfirmOrder;
import com.jiawa.train.business.domain.ConfirmOrderExample;
import com.jiawa.train.business.domain.DailyTrainTicket;
import com.jiawa.train.business.enums.ConfirmOrderStatusEnum;
import com.jiawa.train.business.enums.SeatColEnum;
import com.jiawa.train.business.enums.SeatTypeEnum;
import com.jiawa.train.business.mapper.ConfirmOrderMapper;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.req.ConfirmOrderQueryReq;
import com.jiawa.train.business.req.ConfirmOrderTicketReq;
import com.jiawa.train.business.resp.ConfirmOrderQueryResp;
import com.jiawa.train.common.context.LoginMemberContext;
import com.jiawa.train.common.exception.BussinessException;
import com.jiawa.train.common.exception.BussinessExceptionEnum;
import com.jiawa.train.common.resp.PageResp;
import com.jiawa.train.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);
    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    @Resource
    private DailyTrainTicketService dailyTrainTicketService;

    public void save(ConfirmOrderDoReq req){
        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = BeanUtil.copyProperties(req, ConfirmOrder.class);
        if(ObjectUtil.isNull(confirmOrder.getId())) {
            confirmOrder.setId(SnowUtil.getSnowflakeNextId());
            confirmOrder.setCreateTime(now);
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.insert(confirmOrder);
        }else{
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.updateByPrimaryKey(confirmOrder);
        }
    }

    public PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req){
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
        confirmOrderExample.setOrderByClause("id desc");
        ConfirmOrderExample.Criteria criteria = confirmOrderExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(),req.getSize());
        List<ConfirmOrder> confirmOrderList = confirmOrderMapper.selectByExample(confirmOrderExample);

        PageInfo<ConfirmOrder> pageInfo = new PageInfo<>(confirmOrderList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<ConfirmOrderQueryResp> list = BeanUtil.copyToList(confirmOrderList, ConfirmOrderQueryResp.class);

        PageResp<ConfirmOrderQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return  pageResp;
    }

    public void delete(Long id){
        confirmOrderMapper.deleteByPrimaryKey(id);
    }


    public void doConfirm(ConfirmOrderDoReq req){
        // 省略业务数据校验，如：车次是否存在，余票是否存在，车次是否在有效期内，tickets条数>0，同乘客同车次是否已买过

        // 保存确认订单表，状态初始
        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = new ConfirmOrder();
        confirmOrder.setId(SnowUtil.getSnowflakeNextId());
        confirmOrder.setMemberId(LoginMemberContext.getId());
        confirmOrder.setDate(req.getDate());
        confirmOrder.setTrainCode(req.getTrainCode());
        confirmOrder.setStart(req.getStart());
        confirmOrder.setEnd(req.getEnd());
        confirmOrder.setDailyTrainTicketId(req.getDailyTrainTicketId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
        confirmOrder.setCreateTime(now);
        confirmOrder.setUpdateTime(now);
        confirmOrder.setTickets(JSON.toJSONString(req.getTickets()));

        confirmOrderMapper.insert(confirmOrder);

        // 查出余票记录，需要得到真实的库存
        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(req.getDate(), req.getTrainCode(), req.getStart(), req.getEnd());
        LOG.info("查出余票记录：{}",dailyTrainTicket);

        // 扣减余票数量，并判断余票是否足够
        for(ConfirmOrderTicketReq confirmOrderTicketReq : req.getTickets()){
            SeatTypeEnum seatTypeEnum = EnumUtil.getBy(SeatTypeEnum::getCode, confirmOrderTicketReq.getSeatTypeCode());
            switch (seatTypeEnum){
                case YDZ -> {
                    int countLeft = dailyTrainTicket.getYdz()-1;
                    if(countLeft < 0){
                        throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }else {
                        dailyTrainTicket.setYdz(countLeft);
                    }
                }
                case EDZ -> {
                    int countLeft = dailyTrainTicket.getEdz()-1;
                    if(countLeft < 0){
                        throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }else {
                        dailyTrainTicket.setEdz(countLeft);
                    }
                }
                case RW -> {
                    int countLeft = dailyTrainTicket.getRw()-1;
                    if(countLeft < 0){
                        throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }else {
                        dailyTrainTicket.setRw(countLeft);
                    }
                }
                case YW -> {
                    int countLeft = dailyTrainTicket.getYw()-1;
                    if(countLeft < 0){
                        throw new BussinessException(BussinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }else {
                        dailyTrainTicket.setYw(countLeft);
                    }
                }
            }
        }

        //计算相对第一个座位的偏移值
        //比如选择的是C1、D2，则偏移值是：[0,5]
        //比如选择的是A1,B1,C1，则偏移值是：[0,1,2]
        List<ConfirmOrderTicketReq> tickets = req.getTickets();
        String seat = tickets.get(0).getSeat();
        if(StrUtil.isNotBlank(seat)){
            LOG.info("本次购票有选座");
            //查出本次选座的作为类型都有哪些，用于计算所选座位与第一个座位的偏移值
            List<SeatColEnum> seatColEnums = SeatColEnum.getColsByType(tickets.get(0).getSeatTypeCode());
            LOG.info("本次选座的座位类型包含的列：{}",seatColEnums);

            //组成和前端两排选座一样的列表，用于作参照的座位列表，例：referSeatList = {A1,C1,D1,F1,A2,C2,D2,F2}
            List<String>  referSeatList = new ArrayList<>();
            for(int i = 1; i <= 2; i++){
                for(SeatColEnum seatColEnum : seatColEnums){
                    referSeatList.add(seatColEnum.getCode()+i);
                }
            }
            LOG.info("用于作参照的两排座位：{}",referSeatList);

            //绝对偏移值, 即：在参照座位列表中的位置
            List<Integer> offsetList = new ArrayList<>();
            List<Integer> absoluteOffset = new ArrayList<>();
            for(ConfirmOrderTicketReq confirmOrderTicketReq : tickets){
                absoluteOffset.add(referSeatList.indexOf(confirmOrderTicketReq.getSeat()));
            }
            LOG.info("计算得到所有座位的绝对偏移值：{}",absoluteOffset);
            for(Integer i : absoluteOffset){
                offsetList.add(i - absoluteOffset.get(0));
            }
            LOG.info("计算得到所有座位的相对偏移值：{}",offsetList);
        }else {
            LOG.info("本次购票没有选座");
        };


        // 选座

            // 一个车箱一个车箱的获取座位数据

            // 挑选符合条件的座位，如果这个车箱不满足，则进入下个车箱（多个选座应该在同一个车厢）

        // 选中座位后事务处理：

            // 座位表修改售卖情况sell；
            // 余票详情表修改余票；
            // 为会员增加购票记录
            // 更新确认订单为成功
    }
}
