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

package com.checo.fake12306.biz.ticketservice.job;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import com.checo.fake12306.biz.ticketservice.common.constant.Fake12306Constant;
import com.checo.fake12306.biz.ticketservice.dao.entity.TrainDO;
import com.checo.fake12306.biz.ticketservice.dao.entity.TrainStationDO;
import com.checo.fake12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import com.checo.fake12306.biz.ticketservice.job.base.AbstractTrainStationJobHandlerTemplate;
import com.checo.fake12306.framework.starter.cache.DistributedCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.checo.fake12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_STOPOVER_DETAIL;

/**
 * 列车路线信息定时任务
 * 已通过运行时实时获取解决该定时任务
 */
@Deprecated
@RestController
@RequiredArgsConstructor
public class TrainStationJobHandler extends AbstractTrainStationJobHandlerTemplate {

    private final TrainStationMapper trainStationMapper;
    private final DistributedCache distributedCache;

    @XxlJob(value = "trainStationJobHandler")
    @GetMapping("/api/ticket-service/train-station/job/cache-init/execute")
    @Override
    public void execute() {
        super.execute();
    }

    @Override
    protected void actualExecute(List<TrainDO> trainDOPageRecords) {
        for (TrainDO each : trainDOPageRecords) {
            LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                    .eq(TrainStationDO::getTrainId, each.getId());
            List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);
            distributedCache.put(
                    TRAIN_STATION_STOPOVER_DETAIL + each.getId(),
                    JSON.toJSONString(trainStationDOList),
                    Fake12306Constant.ADVANCE_TICKET_DAY,
                    TimeUnit.DAYS
            );
        }
    }
}
