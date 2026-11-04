package com.xhzb.nursing.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.nursing.domain.Contract;

/**
 * 合同信息Mapper接口
 * 
 * @author hongbozcode
 * @date 2026-11-03
 */
@Mapper
public interface ContractMapper extends BaseMapper<Contract>
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
     * 删除合同信息
     * 
     * @param id 合同信息主键
     * @return 结果
     */
    public int deleteContractById(Long id);

    /**
     * 批量删除合同信息
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteContractByIds(Long[] ids);
}
