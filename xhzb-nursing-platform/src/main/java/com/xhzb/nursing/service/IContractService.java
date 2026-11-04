package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.Contract;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 合同信息Service接口
 * 
 * @author hongbozcode
 * @date 2026-11-03
 */
public interface IContractService extends IService<Contract>
{
    /**
     * 查询合同信息
     * 
     * @param id 合同信息主键
     * @return 合同信息
     */
    public Contract selectContractById(Long id);

    /**
     * 查询合同信息列表
     * 
     * @param contract 合同信息
     * @return 合同信息集合
     */
    public List<Contract> selectContractList(Contract contract);

    /**
     * 新增合同信息
     * 
     * @param contract 合同信息
     * @return 结果
     */
    public int insertContract(Contract contract);

    /**
     * 修改合同信息
     * 
     * @param contract 合同信息
     * @return 结果
     */
    public int updateContract(Contract contract);

    /**
     * 批量删除合同信息
     * 
     * @param ids 需要删除的合同信息主键集合
     * @return 结果
     */
    public int deleteContractByIds(Long[] ids);

    /**
     * 删除合同信息信息
     * 
     * @param id 合同信息主键
     * @return 结果
     */
    public int deleteContractById(Long id);
}
