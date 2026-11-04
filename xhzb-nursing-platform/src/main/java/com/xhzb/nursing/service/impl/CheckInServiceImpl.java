package com.xhzb.nursing.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.nursing.domain.Bed;
import com.xhzb.nursing.domain.CheckInConfig;
import com.xhzb.nursing.domain.Contract;
import com.xhzb.nursing.domain.Elder;
import com.xhzb.nursing.domain.HealthAssessment;
import com.xhzb.nursing.domain.HealthAssessmentReport;
import com.xhzb.nursing.domain.NursingLevel;
import com.xhzb.nursing.domain.dto.CheckInApplyDto;
import com.xhzb.nursing.domain.dto.CheckInConfigDto;
import com.xhzb.nursing.domain.dto.CheckInElderDto;
import com.xhzb.nursing.domain.dto.ElderFamilyDto;
import com.xhzb.nursing.domain.vo.CheckInConfigVo;
import com.xhzb.nursing.domain.vo.CheckInDetailVo;
import com.xhzb.nursing.domain.vo.CheckInElderVo;
import com.xhzb.nursing.domain.vo.CheckInRoomVo;
import com.xhzb.nursing.mapper.AdmissionMapper;
import com.xhzb.nursing.mapper.BedMapper;
import com.xhzb.nursing.mapper.CheckInConfigMapper;
import com.xhzb.nursing.mapper.ContractMapper;
import com.xhzb.nursing.mapper.ElderMapper;
import com.xhzb.nursing.mapper.HealthAssessmentMapper;
import com.xhzb.nursing.mapper.HealthAssessmentReportMapper;
import com.xhzb.nursing.mapper.NursingLevelMapper;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.Duration;
import java.util.Objects;
import java.util.Locale;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.CheckInMapper;
import com.xhzb.nursing.domain.CheckIn;
import com.xhzb.nursing.service.ICheckInService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;

/**
 * 入住信息Service业务层处理
 * 
 * @author hongbozcode
 * @date 2026-11-03
 */
@Service
public class CheckInServiceImpl extends ServiceImpl<CheckInMapper, CheckIn> implements ICheckInService
{
    @Autowired
    private CheckInMapper checkInMapper;

    /**
     * 查询入住信息
     * 
     * @param id 入住信息主键
     * @return 入住信息
     */
    @Override
    public CheckIn selectCheckInById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询入住信息列表
     * 
     * @param checkIn 入住信息
     * @return 入住信息
     */
    @Override
    public List<CheckIn> selectCheckInList(CheckIn checkIn)
    {
        return checkInMapper.selectCheckInList(checkIn);
    }

    /**
     * 新增入住信息
     * 
     * @param checkIn 入住信息
     * @return 结果
     */
    @Override
    public int insertCheckIn(CheckIn checkIn)
    {
        return save(checkIn)? 1 : 0;
    }

    /**
     * 修改入住信息
     * 
     * @param checkIn 入住信息
     * @return 结果
     */
    @Override
    public int updateCheckIn(CheckIn checkIn)
    {
        return updateById(checkIn)? 1 : 0;
    }

    /**
     * 批量删除入住信息
     * 
     * @param ids 需要删除的入住信息主键
     * @return 结果
     */
    @Override
    public int deleteCheckInByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除入住信息信息
     * 
     * @param id 入住信息主键
     * @return 结果
     */
    @Override
    public int deleteCheckInById(Long id)
    {
        return removeById(id)? 1 : 0;
    }

    @Autowired
    private AdmissionMapper admissionMapper;
    @Autowired
    private HealthAssessmentMapper assessmentMapper;
    @Autowired
    private HealthAssessmentReportMapper reportMapper;
    @Autowired
    private BedMapper bedMapper;
    @Autowired
    private ElderMapper elderMapper;
    @Autowired
    private ContractMapper contractMapper;
    @Autowired
    private CheckInConfigMapper configMapper;
    @Autowired
    private NursingLevelMapper levelMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 完成入住办理；任何一步失败均回滚数据库修改。 */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void apply(CheckInApplyDto dto) {
        HealthAssessment initial = assessmentMapper.selectById(dto.getHealthAssessmentId());
        if (initial == null) { throw new ServiceException("未完成评估不能入住"); }
        // 同一老人可能有多次评估，锁定该身份证全部评估，避免不同评估同时申请入住。
        HealthAssessment assessment = admissionMapper.lockAssessments(initial.getIdCard()).stream()
                .filter(item -> item.getId().equals(dto.getHealthAssessmentId())).findFirst().orElse(null);
        if (assessment == null || !Integer.valueOf(1).equals(assessment.getEvaluationProgress())) {
            throw new ServiceException("未完成评估不能入住");
        }
        if (!admissionMapper.lockActiveCheckIns(assessment.getIdCard()).isEmpty()) {
            throw new ServiceException("老人已入住");
        }
        CheckInElderDto elderDto = dto.getCheckInElderDto();
        if (!assessment.getIdCard().equalsIgnoreCase(elderDto.getIdCardNo())) {
            throw new ServiceException("老人身份证与评估信息不一致");
        }
        CheckInConfigDto configDto = dto.getCheckInConfigDto();
        validateDates(configDto);
        String familyJson = JSON.toJSONString(dto.getElderFamilyDtoList());
        if (familyJson.codePointCount(0, familyJson.length()) > 255) {
            throw new ServiceException("家属信息超过255字符，无法保存到入住备注");
        }
        List<HealthAssessmentReport> reports = reportMapper.selectList(Wrappers.<HealthAssessmentReport>lambdaQuery()
                .eq(HealthAssessmentReport::getHealthAssessmentId, assessment.getId()));
        if (reports.isEmpty()) { throw new ServiceException("评估结果不存在"); }
        NursingLevel level = levelMapper.selectById(configDto.getNursingLevelId());
        if (level == null || !Integer.valueOf(1).equals(level.getStatus())) {
            throw new ServiceException("护理等级不存在或未启用");
        }
        Bed bed = bedMapper.selectById(configDto.getBedId());
        if (bed == null) { throw new ServiceException("床位不存在"); }
        CheckInRoomVo room = admissionMapper.selectRoom(bed.getRoomId());
        if (room == null || !Objects.equals(bed.getRoomId(), configDto.getRoomId())
                || !Objects.equals(room.getFloorId(), configDto.getFloorId().toString())) {
            throw new ServiceException("床位、房间与楼层不匹配");
        }
        if (bed.getBedNumber() == null || bed.getBedNumber().length() > 20) {
            throw new ServiceException("床位编号为空或超过老人表允许长度");
        }
        // 条件更新是最终占床判据，两个事务抢同一床时只有一个成功。
        int occupied = bedMapper.update(new Bed(), Wrappers.<Bed>lambdaUpdate()
                .eq(Bed::getId, bed.getId()).eq(Bed::getBedStatus, 0).set(Bed::getBedStatus, 1));
        if (occupied != 1) { throw new ServiceException("有其他老人已入住"); }

        List<Elder> existing = elderMapper.selectList(Wrappers.<Elder>lambdaQuery()
                .eq(Elder::getIdCardNo, assessment.getIdCard()));
        if (existing.size() > 1) { throw new ServiceException("老人身份证存在重复档案，请先核对"); }
        Elder elder = existing.isEmpty() ? new Elder() : existing.get(0);
        BeanUtils.copyProperties(elderDto, elder);
        elder.setIdCardNo(assessment.getIdCard());
        elder.setStatus(1);
        elder.setBedId(bed.getId());
        elder.setBedNumber(bed.getBedNumber());
        if (elder.getId() == null) {
            requireWrite(elderMapper.insert(elder));
        } else {
            requireWrite(elderMapper.updateById(elder));
        }
        // contract.elder_id 为 int，显式检查，禁止 bigint 老人ID溢出后错误关联。
        if (elder.getId() > Integer.MAX_VALUE) { throw new ServiceException("老人ID超出合同表字段范围"); }
        Contract contract = new Contract();
        BeanUtils.copyProperties(dto.getCheckInContractDto(), contract);
        contract.setElderId(elder.getId().intValue());
        contract.setElderName(elder.getName());
        contract.setContractNumber(nextContractNumber());
        contract.setStartDate(configDto.getStartDate());
        contract.setEndDate(configDto.getEndDate());
        contract.setStatus(contract.getSignDate().isAfter(LocalDateTime.now()) ? 0 : 1);
        contract.setSortOrder(0);
        requireWrite(contractMapper.insert(contract));

        CheckIn checkIn = new CheckIn();
        checkIn.setElderId(elder.getId());
        checkIn.setElderName(elder.getName());
        checkIn.setIdCardNo(elder.getIdCardNo());
        checkIn.setStartDate(configDto.getStartDate());
        checkIn.setEndDate(configDto.getEndDate());
        checkIn.setNursingLevelName(level.getName());
        checkIn.setBedNumber(bed.getBedNumber());
        checkIn.setStatus(0);
        checkIn.setSortOrder(0);
        checkIn.setRemark(familyJson);
        requireWrite(checkInMapper.insert(checkIn));
        CheckInConfig config = new CheckInConfig();
        BeanUtils.copyProperties(configDto, config);
        config.setCheckInId(checkIn.getId());
        config.setNursingLevelName(level.getName());
        config.setSortOrder(0);
        // 表中没有合同外键，利用现有备注记录本次合同ID，避免再次入住时串合同。
        JSONObject link = new JSONObject();
        link.put("contractId", contract.getId());
        config.setRemark(link.toJSONString());
        requireWrite(configMapper.insert(config));
        requireWrite(assessmentMapper.update(new HealthAssessment(), Wrappers.<HealthAssessment>lambdaUpdate()
                .eq(HealthAssessment::getId, assessment.getId())
                .set(HealthAssessment::getElderId, elder.getId()).set(HealthAssessment::getCheckInStatus, 1)));
        int updatedReports = reportMapper.update(new HealthAssessmentReport(), Wrappers.<HealthAssessmentReport>lambdaUpdate()
                .eq(HealthAssessmentReport::getHealthAssessmentId, assessment.getId())
                .set(HealthAssessmentReport::getCheckInStatus, 1));
        if (updatedReports < 1) { throw new ServiceException("评估结果更新失败"); }
    }

    /** 检查入住及计费日期区间。 */
    private void validateDates(CheckInConfigDto config) {
        if (config.getStartDate().isAfter(config.getEndDate())
                || config.getFeeStartDate().isAfter(config.getFeeEndDate())
                || config.getFeeStartDate().isBefore(config.getStartDate())
                || config.getFeeEndDate().isAfter(config.getEndDate())) {
            throw new ServiceException("入住或计费日期区间不正确");
        }
    }

    /** Redis按秒递增，跨实例生成HT+时间+四位序号；失败回滚允许序号有空隙。 */
    private String nextContractNumber() {
        String second = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String key = "nursing:contract:sequence:" + second;
        while (true) {
            Long sequence = stringRedisTemplate.opsForValue().increment(key);
            stringRedisTemplate.expire(key, Duration.ofDays(2));
            if (sequence == null || sequence > 9999) { throw new ServiceException("合同编号生成繁忙，请稍后重试"); }
            String number = "HT" + second + String.format(Locale.ROOT, "%04d", sequence);
            // Redis恢复后也跳过数据库已经使用的编号。
            if (contractMapper.selectCount(Wrappers.<Contract>lambdaQuery()
                    .eq(Contract::getContractNumber, number)) == 0) { return number; }
        }
    }

    /** 所有关键写入都必须成功，否则触发事务回滚。 */
    private void requireWrite(int affected) {
        if (affected != 1) { throw new ServiceException("入住办理失败，请重试"); }
    }

    /** 从入住、老人、配置和合同中组装详情。 */
    @Override
    @Transactional(readOnly = true)
    public CheckInDetailVo detail(Long id) {
        CheckIn checkIn = checkInMapper.selectById(id);
        if (checkIn == null) { throw new ServiceException("入住记录不存在"); }
        Elder elder = elderMapper.selectById(checkIn.getElderId());
        if (elder == null) { throw new ServiceException("老人信息不存在"); }
        CheckInElderVo elderVo = new CheckInElderVo();
        BeanUtils.copyProperties(elder, elderVo);
        if (elder.getBirthday() != null && !elder.getBirthday().isBlank()) {
            try { elderVo.setAge(Period.between(LocalDate.parse(elder.getBirthday()), LocalDate.now()).getYears()); }
            catch (DateTimeParseException ex) { throw new ServiceException("老人出生日期格式不正确"); }
        }
        CheckInConfig config = configMapper.selectOne(Wrappers.<CheckInConfig>lambdaQuery()
                .eq(CheckInConfig::getCheckInId, id));
        if (config == null) { throw new ServiceException("入住配置不存在"); }
        CheckInConfigVo configVo = new CheckInConfigVo();
        BeanUtils.copyProperties(config, configVo);
        configVo.setRemark(null);
        configVo.setStartDate(checkIn.getStartDate());
        configVo.setEndDate(checkIn.getEndDate());
        configVo.setBedNumber(checkIn.getBedNumber());
        Contract contract = findContract(config, checkIn);
        CheckInDetailVo detail = new CheckInDetailVo();
        detail.setCheckInElderVo(elderVo);
        detail.setCheckInConfigVo(configVo);
        detail.setContract(contract);
        detail.setElderFamilyVoList(checkIn.getRemark() == null || checkIn.getRemark().isBlank()
                ? java.util.Collections.emptyList() : JSON.parseArray(checkIn.getRemark(), ElderFamilyDto.class));
        return detail;
    }

    /** 优先读取本次合同关联；旧记录仅在老人及起止日期唯一匹配时返回。 */
    private Contract findContract(CheckInConfig config, CheckIn checkIn) {
        if (config.getRemark() != null && JSON.isValidObject(config.getRemark())) {
            Long contractId = JSON.parseObject(config.getRemark()).getLong("contractId");
            if (contractId != null) {
                Contract contract = contractMapper.selectById(contractId);
                if (contract == null || !Objects.equals(contract.getElderId().longValue(), checkIn.getElderId())) {
                    throw new ServiceException("入住合同关联异常");
                }
                return contract;
            }
        }
        List<Contract> contracts = contractMapper.selectList(Wrappers.<Contract>lambdaQuery()
                .eq(Contract::getElderId, checkIn.getElderId())
                .eq(Contract::getStartDate, checkIn.getStartDate())
                .eq(Contract::getEndDate, checkIn.getEndDate()));
        if (contracts.size() != 1) { throw new ServiceException("入住合同缺失或存在多个匹配记录"); }
        return contracts.get(0);
    }
}
