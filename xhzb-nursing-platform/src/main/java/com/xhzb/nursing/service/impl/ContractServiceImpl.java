package com.xhzb.nursing.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.xhzb.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.ContractMapper;
import com.xhzb.nursing.domain.Contract;
import com.xhzb.nursing.service.IContractService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * 合同信息Service业务层处理
 * 
 * @author hongbozcode
 * @date 2026-11-03
 */
@Service
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements IContractService
{
    @Autowired
    private ContractMapper contractMapper;

    /**
     * 查询合同信息
     * 
     * @param id 合同信息主键
     * @return 合同信息
     */
    @Override
    public Contract selectContractById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询合同信息列表
     * 
     * @param contract 合同信息
     * @return 合同信息
     */
    @Override
    public List<Contract> selectContractList(Contract contract)
    {
        return contractMapper.selectContractList(contract);
    }

    /**
     * 新增合同信息
     * 
     * @param contract 合同信息
     * @return 结果
     */
    @Override
    public int insertContract(Contract contract)
    {
        return save(contract)? 1 : 0;
    }

    /**
     * 修改合同信息
     * 
     * @param contract 合同信息
     * @return 结果
     */
    @Override
    public int updateContract(Contract contract)
    {
        return updateById(contract)? 1 : 0;
    }

    /**
     * 批量删除合同信息
     * 
     * @param ids 需要删除的合同信息主键
     * @return 结果
     */
    @Override
    public int deleteContractByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除合同信息信息
     * 
     * @param id 合同信息主键
     * @return 结果
     */
    @Override
    public int deleteContractById(Long id)
    {
        return removeById(id)? 1 : 0;
    }

    /**
     * 更新合同的状态
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateContractStatus() {
        //第一种需要处理的合同：当前状态未生效 且 系统时间>开始时间 且 系统时间<结束时间,要将合同状态更新为已生效
        //select id from contract where status=0 and now() >start_date  and now() < end_state
        List<Contract> contractList = this.lambdaQuery()
                .select(Contract::getId)
                .eq(Contract::getStatus,0)
                .le(Contract::getStartDate, LocalDateTime.now())
                .ge(Contract::getStartDate, LocalDateTime.now())
                .list();
        if(CollUtil.isNotEmpty(contractList)){
            List<Long> idList = contractList.stream().map(Contract::getId).toList();
            idList.forEach(x ->{
                this.lambdaUpdate().eq(Contract::getId,x).set(Contract::getStatus,1).update();
            });
        }

        // 第二种需要处理的合同：当前 状态已生效 且 系统时间>结束时间，要将合同状态更新为已过期
        //select id from contract where status=1 and now()>end_date
        List<Contract> contractList2 = this.lambdaQuery()
                .select(Contract::getId)
                .eq(Contract::getStatus, 1)
                .lt(Contract::getEndDate, LocalDateTime.now())
                .list();
        if(CollUtil.isNotEmpty(contractList2)) {
            List<Long> idList2 = contractList2.stream().map(Contract::getId).toList();
            idList2.forEach(x->{
                //update contract set status=2 where id=?
                this.lambdaUpdate().eq(Contract::getId,x).set(Contract::getStatus,2).update();
            });
        }

    }
}
