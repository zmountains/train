package com.jiawa.train.member.req;

import jakarta.validation.constraints.NotBlank;

public class MemberLoginReq {

    @NotBlank(message = "【手机号】不能为空")
    private String mobile;
    @NotBlank(message = "【验证码】不能为空")
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    @Override
    public String toString() {
        return "MemberLoginReq{" +
                "mobile='" + mobile + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}

