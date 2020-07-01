package com.youxin.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/test")
public class TestController {


    @Autowired(required=false)
    private TestFeign testFeign;

    /**
     *
     *
     * @param
     * @return
     */
    @GetMapping("/vv")
    public String add() {
        return testFeign.getAll();
    }


}
