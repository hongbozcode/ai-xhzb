package com.xhzb.nursing.service.impl;

import java.util.List;
import com.xhzb.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.ContractMapper;
import com.xhzb.nursing.domain.Contract;
import com.xhzb.nursing.service.IContractService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
}
