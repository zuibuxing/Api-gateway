package com.youxin.gateway.controller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name="test3")
public interface TestFeign {

    @RequestMapping(value = "/test/v2",method = RequestMethod.GET)
     String getAll();

}
