package com.jiawa.train.member.service;

import com.jiawa.train.common.exception.BussinessException;
import com.jiawa.train.common.exception.BussinessExceptionEnum;
import com.jiawa.train.common.util.SnowUtil;
import com.jiawa.train.member.domain.Member;
import com.jiawa.train.member.domain.MemberExample;
import com.jiawa.train.member.mapper.MemberMapper;
import com.jiawa.train.member.req.MemberRegisterReq;
import com.jiawa.train.member.req.MemberSendCodeReq;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    private static final Logger LOG = LoggerFactory.getLogger(MemberService.class);
    @Resource
    private MemberMapper memberMapper;
    public int count(){
         return (int) memberMapper.countByExample(null);
    }

    public long register(MemberRegisterReq req){
        String mobile = req.getMobile();
        MemberExample memberExample = new MemberExample();
        memberExample.createCriteria().andMobileEqualTo(mobile);
        List<Member> list = memberMapper.selectByExample(memberExample);
        if(!list.isEmpty()){
            throw new BussinessException(BussinessExceptionEnum.MEMBER_MOBILE_EXIST);
        }

        Member member = new Member();
        member.setMobile(mobile);
        member.setId(SnowUtil.getSnowflakeNextId());
        memberMapper.insert(member);
        return member.getId();
    }

    public void sendCode(MemberSendCodeReq req){
        String mobile = req.getMobile();
        MemberExample memberExample = new MemberExample();
        memberExample.createCriteria().andMobileEqualTo(mobile);
        List<Member> list = memberMapper.selectByExample(memberExample);

        //如果手机号存在，则插入一条记录
        if(list.isEmpty()){
            LOG.info("手机号不存在，插入一条记录");
            Member member = new Member();
            member.setMobile(mobile);
            member.setId(SnowUtil.getSnowflakeNextId());
            memberMapper.insert(member);
        }else{
            LOG.info("手机号存在，不插入记录");
        }

        //生成验证码
        //String code = RandomUtil.randomString(4);
        String code = "8888";
        LOG.info("生成短信验证码：{}",code);

        // 保存短信记录表：手机号，短信验证码，有效期，是否已使用，业务类型，发送时间，使用时间
        LOG.info("保存短信记录表");

        // 对接短信通道，发送短信
        LOG.info("对接短信通道");
    }
}
