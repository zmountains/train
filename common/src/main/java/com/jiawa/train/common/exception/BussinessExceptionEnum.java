package com.jiawa.train.common.exception;

public enum BussinessExceptionEnum {
    MEMBER_MOBILE_EXIST("手机号已注册"),
    MEMBER_MOBILE_NOT_EXIST("请先获取短信验证码"),
    MEMBER_MOBILE_CODE_ERROR("短信验证码错误"),
    CONFIRM_ORDER_TICKET_COUNT_ERROR("余票不足"),
    CONFIRM_ORDER_EXCEPTION("服务器忙，请稍后重试"),
    CONFIRM_ORDER_LOCK_FAIL("当前抢票人数多，请稍后重试");

    private String desc;

    BussinessExceptionEnum(String desce) {
        this.desc = desce;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "BussinessExceptionEnum{" +
                "desc='" + desc + '\'' +
                '}';
    }
}
