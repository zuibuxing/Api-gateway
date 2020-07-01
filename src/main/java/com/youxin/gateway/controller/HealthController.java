package com.youxin.gateway.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @RequestMapping("ok")
    public String ok() {
        return "ok";
    }

    @RequestMapping("health")
    public String health() {
        return "health";
    }
}
