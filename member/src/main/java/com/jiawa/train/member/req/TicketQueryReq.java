package com.jiawa.train.member.req;

import com.jiawa.train.common.req.PageReq;

public class TicketQueryReq extends PageReq {

    private Long memberID;

    public Long getMemberID() {
        return memberID;
    }

    public void setMemberID(Long memberID) {
        this.memberID = memberID;
    }

    @Override
    public String toString() {
        return "TicketQueryReq{" +
                "memberID=" + memberID +
                "} " + super.toString();
    }
}
