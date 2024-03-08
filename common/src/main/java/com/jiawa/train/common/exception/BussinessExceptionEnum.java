package com.jiawa.train.common.exception;

public enum BussinessExceptionEnum {
    MEMBER_MOBILE_EXIST("手机号已注册");
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
