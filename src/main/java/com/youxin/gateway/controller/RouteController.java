package com.youxin.gateway.controller;

import com.netflix.loadbalancer.Server;
import com.youxin.gateway.model.GatewayRouteDefinition;
import com.youxin.gateway.service.DynamicRouteServiceImpl;
import com.youxin.gateway.result.Result;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/route")
public class RouteController {

    private static final Logger logger = LoggerFactory.getLogger(RouteController.class);

    @Autowired
    private DynamicRouteServiceImpl dynamicRouteService;


    /**
     * 增加路由
     *
     * @param gwdefinition
     * @return
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody GatewayRouteDefinition gwdefinition) {
        return this.dynamicRouteService.add(gwdefinition);
    }


    /**
     * 批量增加路由
     *
     * @param gwdefinitionList
     * @return
     */
    @PostMapping("/addBatch")
    public Result<?> addBatch(@RequestBody List<GatewayRouteDefinition> gwdefinitionList) {
        return this.dynamicRouteService.addBatch(gwdefinitionList);
    }


    /**
     * 删除
     *
     * @param id
     * @return
     */
    @GetMapping("/delete/{id}")
    public Result<String> delete(@PathVariable String id) {
        return this.dynamicRouteService.delete(id);
    }


    /**
     * 更新路由
     *
     * @param gwdefinition
     * @return
     */
    @PostMapping("/update")
    public Result<String> update(@RequestBody GatewayRouteDefinition gwdefinition) {
        return this.dynamicRouteService.update(gwdefinition);
    }


    @PostMapping("/refresh")
    public Result<?> refresh(@RequestParam(required = false) String lastUpdateTime) {
        //重置路由是
        return this.dynamicRouteService.refresh(lastUpdateTime,"");
    }

    @GetMapping("/refreshCache")
    public void refreshCache() {
        //重置路由是
        this.dynamicRouteService.refreshCache();
    }


}
