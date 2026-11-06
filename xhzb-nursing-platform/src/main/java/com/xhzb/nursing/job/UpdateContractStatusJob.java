package com.xhzb.nursing.job;

import cn.hutool.core.collection.CollUtil;
import com.xhzb.nursing.domain.Contract;
import com.xhzb.nursing.service.IContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 更新合同状态的定时任务
 *
 * @Author mr.wu
 * @Date 2026-8-20 9:59
 */
@Component("contractJob")
public class UpdateContractStatusJob {

    @Autowired
    private IContractService contractService;

    /**
     * 定时任务
     */
    public void updateStatus(){
        contractService.updateContractStatus();
    }
}