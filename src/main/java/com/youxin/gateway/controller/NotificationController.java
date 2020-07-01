package com.youxin.gateway.controller;

import com.youxin.gateway.service.NotificationServiceImpl;
import com.youxin.gateway.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用于接收注册中心服务更新的通知,目前使用的是定时轮询和通知并存
 */
@RestController
@RequestMapping("/notify")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationServiceImpl notificationService;

    /**
     * 更新服务列表 updateServiceList
     *
     * @param
     * @return
     */
    @PostMapping("/service/update")
    public Result<String> updateServiceList(String serviceName) {
            return notificationService.publishServiceUpdateEvent(serviceName);
    }


}
