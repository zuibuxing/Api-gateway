package com.youxin.gateway.task;

import com.youxin.gateway.enums.RouteStatusEnum;
import com.youxin.gateway.result.ResultCodeEnum;
import com.youxin.gateway.service.DynamicRouteServiceImpl;
import com.youxin.gateway.util.ResultUtil;
import com.youxin.gateway.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Component
public class PollRouteListTask implements ApplicationRunner {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${poll.server.flag}")
    private boolean pollFlag;

    @Autowired
    private DynamicRouteServiceImpl dynamicRouteService;


    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    //用于拉取最新的路由信息，一开始是空，全量拉取
    private String lastUpdateTime = "";


    //实现ApplicationRunner，在项目启动后再拉取路由，防止有bean未加载完成
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (pollFlag) {
            pollAllRouteList();
        }
    }


    /**
     * 轮询拉取路由列表,拉取状态为 0 和1 的所有路由
     */
    @Scheduled(cron = "${poll.server.corn}")
    public void pollRouteList() {
        if (pollFlag) {
            logger.info("- - - - - - - - - 定时拉取路由信息开始 - - - - - - - - - ");
            Result<List<Result>> result = (Result<List<Result>>) dynamicRouteService.refresh(lastUpdateTime, "");
            //操作成功更新时间戳
            if (fetchResultIsAvailabe(result)) {
                lastUpdateTime = dtf.format(LocalDateTime.now());
            } else {
                logger.error("定时拉取路由信息失败:异常{}", result.getMessage());
            }
            logger.info("- - - - - - - - - 定时拉取路由信息结束 - - - - - - - - - ");
        }

    }

    /**
     * 服务启动时拉取状态为1的路由信息
     */
    public void pollAllRouteList() {
        logger.info("- - - - - - - - - 初始化拉取全量路由信息开始 - - - - - - - - - ");
        Result<List<Result>> result = (Result<List<Result>>) dynamicRouteService.refresh(lastUpdateTime, RouteStatusEnum.START.getStatus());
        //操作成功更新时间戳
        if (fetchResultIsAvailabe(result)) {
            lastUpdateTime = dtf.format(LocalDateTime.now());
        } else {
            logger.error("初始化拉取全量路由信息失败:异常{}", result.getMessage());
        }
        logger.info("- - - - - - - - - 初始化拉取全量路由信息结束 - - - - - - - - - ");
    }

    /**
     * 把 Result里每一个 result的结果是否成功，list中有一个result成功，则当前整个操作成功
     */
    private boolean fetchResultIsAvailabe(Result<List<Result>> result) {
        List<Result> resultList = (List<Result>) result.getData();

        return ResultUtil.resultIsAvailabe(result) && (resultList.size() == 0 || resultList.stream().anyMatch(actionResult -> ResultCodeEnum.SUCCESS.getCode() == actionResult.getCode()));
    }

}
