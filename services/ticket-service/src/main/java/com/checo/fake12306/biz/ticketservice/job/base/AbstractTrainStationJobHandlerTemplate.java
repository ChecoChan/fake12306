/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.checo.fake12306.biz.ticketservice.job.base;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import com.checo.fake12306.biz.ticketservice.dao.entity.TrainDO;
import com.checo.fake12306.biz.ticketservice.dao.mapper.TrainMapper;
import com.checo.fake12306.framework.starter.bases.ApplicationContextHolder;
import com.checo.fake12306.framework.starter.common.toolkit.EnvironmentUtil;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

/**
 * 抽象列车&车票相关定时任务
 */
public abstract class AbstractTrainStationJobHandlerTemplate extends IJobHandler {

    /**
     * 模板方法模式具体实现子类执行定时任务
     *
     * @param trainDOPageRecords 列车信息分页记录
     */
    protected abstract void actualExecute(List<TrainDO> trainDOPageRecords);

    @Override
    public void execute() {
        var currentPage = 1L;
        var size = 1000L;
        var requestParam = getJobRequestParam();
        var dateTime = StrUtil.isNotBlank(requestParam) ? DateUtil.parse(requestParam, "yyyy-MM-dd") : DateUtil.tomorrow();
        var trainMapper = ApplicationContextHolder.getBean(TrainMapper.class);
        for (; ; currentPage++) {
            var queryWrapper = Wrappers.lambdaQuery(TrainDO.class)
                    .between(TrainDO::getDepartureTime, DateUtil.beginOfDay(dateTime), DateUtil.endOfDay(dateTime));
            var trainDOPage = trainMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
            if (trainDOPage == null || CollUtil.isEmpty(trainDOPage.getRecords())) {
                break;
            }
            var trainDOPageRecords = trainDOPage.getRecords();
            actualExecute(trainDOPageRecords);
        }
    }

    private String getJobRequestParam() {
        return EnvironmentUtil.isDevEnvironment()
                ? Optional.ofNullable(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())).map(ServletRequestAttributes::getRequest).map(each -> each.getHeader("requestParam")).orElse(null)
                : XxlJobHelper.getJobParam();
    }
}
