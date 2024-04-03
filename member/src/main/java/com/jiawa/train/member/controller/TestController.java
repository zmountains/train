package com.jiawa.train.member.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
//    @GetMapping("/hello")
//    public String hello(){ return "hello world!1123232";}

    @Value("${test.nacos}")
    private String testNacos;

    @GetMapping("/hello")
    public String hello(){
        return String.format("Hello %s!",testNacos);
    }
}
