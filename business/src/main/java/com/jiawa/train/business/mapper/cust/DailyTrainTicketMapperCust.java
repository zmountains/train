package com.jiawa.train.business.mapper.cust;

import java.util.Date;
public interface DailyTrainTicketMapperCust {
    int updateCountBySell(Date date, String trainCode, String seatTypeCode, Integer minStartIndex, Integer maxStartIndex, Integer minEndIndex, Integer maxEndIndex);
}