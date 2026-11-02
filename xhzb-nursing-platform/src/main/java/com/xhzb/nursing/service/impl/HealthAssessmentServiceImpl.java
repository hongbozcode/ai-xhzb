package com.xhzb.nursing.service.impl;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.common.utils.PDFUtil;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.SystemConstant;
import com.xhzb.nursing.domain.HealthAssessmentDataCollection;
import com.xhzb.nursing.domain.HealthAssessmentReport;
import com.xhzb.nursing.domain.dto.ElderAssessmentDto;
import com.xhzb.nursing.domain.vo.ElderLevelResultVO;
import com.xhzb.nursing.domain.vo.HealthReportResultVO;
import com.xhzb.nursing.service.IHealthAssessmentReportService;
import com.xhzb.oss.client.OSSAliyunFileStorageService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.HealthAssessmentMapper;
import com.xhzb.nursing.domain.HealthAssessment;
import com.xhzb.nursing.service.IHealthAssessmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 健康评估记录Service业务层处理
 * 
 * @author hongbozcode
 * @date 2026-11-01
 */
@Service
public class HealthAssessmentServiceImpl extends ServiceImpl<HealthAssessmentMapper, HealthAssessment> implements IHealthAssessmentService
{
    @Autowired
    private HealthAssessmentMapper healthAssessmentMapper;
    @Autowired
    private HealthAssessmentDataCollectionServiceImpl healthAssessmentDataCollectionService;
    @Autowired
    private ChatClient chatClientHealth;
    @Autowired
    private IHealthAssessmentReportService iHealthAssessmentReportService;
    @Autowired
    private OSSAliyunFileStorageService ossAliyunFileStorageService;

    /**
     * 查询健康评估采集数据详情
     * 
     * @param id 健康评估采集数据主键
     * @return 健康评估采集数据
     */
    @Override
    public HealthAssessmentDataCollection selectHealthAssessmentDataCollectionById(Long id)
    {
        return healthAssessmentDataCollectionService.getById(id);
    }

    /**
     * 查询健康评估记录列表
     * 
     * @param healthAssessment 健康评估记录
     * @return 健康评估记录
     */
    @Override
    public List<HealthAssessment> selectHealthAssessmentList(HealthAssessment healthAssessment)
    {
        return healthAssessmentMapper.selectHealthAssessmentList(healthAssessment);
    }

    /**
     * 新增或修改健康评估记录
     * 
     * @param dto 健康评估记录收集信息
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long insertOrUpdateHealthAssessment(ElderAssessmentDto dto)
    {
        //封装健康评估记录对象
        HealthAssessment assessment = new HealthAssessment();
        assessment.setElderName(dto.getBasicInfo().getElderName());//老人名称
        assessment.setIdCard(dto.getBasicInfo().getIdCard());//身份证号

        //1. 健康评估记录ID无值处理逻辑
        if (dto.getId()==null) {
            //1.1 补充新增前对象的额外属性值
            assessment.setCheckInStatus(0);//未入住
            assessment.setEvaluationProgress(0);//评估中
            assessment.setCreateTime(DateUtils.getNowDate());//创建时间

            //1.2 新增健康评估记录
            this.save(assessment);
        } else {
            //2. 健康评估记录ID有值处理逻辑

            //2.1 根据ID查询健康评估记录
            HealthAssessment healthAssessment = this.getById(dto.getId());

            //2.2 判断健康评估记录是否存在
            if(healthAssessment==null) {
                throw new BaseException("健康评估记录不存在，无法更新");
            }

            //2.3 判断健康评估记录状态是否评估中
            if (healthAssessment.getEvaluationProgress()!=0) {
                throw new BaseException("健康评估记录评估已完成或已取消，无法更新");
            }

            //2.4 更新健康评估记录
            assessment.setId(dto.getId());
            this.updateById(assessment);
        }

        //3.  封装健康评估数据收集对象
        HealthAssessmentDataCollection collection = new HealthAssessmentDataCollection();
        collection.setId(assessment.getId()); //主键ID
        collection.setBasicInfo(JSONUtil.toJsonStr(dto.getBasicInfo())); //基本信息
        collection.setHealthAssessment(JSONUtil.toJsonStr(dto.getHealthAssessmentDto()));//健康评估信息
        collection.setDailyLivingActivities(JSONUtil.toJsonStr(dto.getDailyLivingActivities()));//日常活动
        collection.setMentalState(JSONUtil.toJsonStr(dto.getMentalState()));//精神状态
        collection.setPerceptionCommunication(JSONUtil.toJsonStr(dto.getPerceptionAndCommunication()));//感知觉及沟通
        collection.setSocialParticipation(JSONUtil.toJsonStr(dto.getSocialParticipation()));//社会参与
        if (dto.getId()==null) {
            collection.setCreateTime(DateUtils.getNowDate());//创建时间
        }

        //4. 新增或更新健康评估收集数据
        healthAssessmentDataCollectionService.saveOrUpdate(collection);

        //5. 返回健康评估记录主键ID
        return assessment.getId();

    }

    /**
     * AI评估
     *
     * @param dto
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public long assessmentData(ElderAssessmentDto dto) {
    //        1.更新保存评估数据
    //        接收前端提交的老人评估表单，把本次评估的基础表单数据保存或者更新到数据库。
        this.insertOrUpdateHealthAssessment(dto);
    //        2.提取各项能力数据
    //        从表单里面抽取出四大模块评估结果： 日常生活活动等级、精神状态等级、感知觉与沟通等级、社会参与等级。 把 “能力完好、轻度受损、中度受损、重度受损” 这类文字等级转换为对应数字编码。 同时提取配套风险信息：近 30 天跌倒、噎食、自杀尝试、走失、昏迷发生次数，是否患有痴呆、精神疾病，画钟测试结果，全部收集齐全，供 AI 分析使用。
        String dailyActivityLevel = this.getCurrentLevel(dto.getDailyLivingActivities().getAbilityRating());
        String mentalStatusLevel = this.getCurrentLevel(dto.getMentalState().getAbilityRating());
        String perceptionCommunicationLevel = this.getCurrentLevel(dto.getPerceptionAndCommunication().getAbilityRating());
        String socialParticipationLevel = this.getCurrentLevel(dto.getSocialParticipation().getAbilityRating());
        // 跌倒次数
        Integer fall = dto.getHealthAssessmentDto().getRecent30Days().getFall();
        // 噎食次数
        Integer choking = dto.getHealthAssessmentDto().getRecent30Days().getChoking();
        // 自杀次数
        Integer suicideAttempt = dto.getHealthAssessmentDto().getRecent30Days().getSuicideAttempt();
        // 走失次数
        Integer lost = dto.getHealthAssessmentDto().getRecent30Days().getLost();
        // 昏迷次数
        Integer coma = dto.getHealthAssessmentDto().getRecent30Days().getComa();
        // 痴呆疾病  0-表示无 1-表示有
        Integer dementia  = dto.getHealthAssessmentDto().getDiseaseDiagnosis().getDementia().equals("无") ? 0 : 1;
        // 精神疾病 0-表示无 1-表示有
        Integer mentalillness  = dto.getHealthAssessmentDto().getDiseaseDiagnosis().getMentalIllness().equals("无") ? 0 : 1;
        // 是否确诊为认知障碍
        Integer known = dto.getMentalState().getClockDrawingTest().getResult()==2 ? 1 : 0;

    //        3.第一阶段 AI分析老人多维度能力评估
    //        1）使用收集好的老人各项评估信息，拼接组装 AI 提示词 prompt，带上评估规则。
        String userPrompt1 = String.format(SystemConstant.USER_PROMPT1,
                dailyActivityLevel,mentalStatusLevel,perceptionCommunicationLevel,socialParticipationLevel,
                fall,choking,suicideAttempt,lost,
                coma,dementia,mentalillness,known
        );
        //        2）通过 ChatClient 调用大模型做能力分析。
        //        3）处理大模型返回内容，剔除多余文字，只解析出 JSON 格式结果，拿到老人初始能力等级、最终能力等级、等级变化原因。

        ElderLevelResultVO elderLevelResultVO = this.chatBigModel(userPrompt1,ElderLevelResultVO.class);

    //        4）新建一条健康评估报告记录，把评估基础信息、第一阶段 AI 的分析结果先存入数据库。
        HealthAssessmentReport report = new HealthAssessmentReport();
        report.setAssessmentTime(LocalDateTime.now());//评估时间
        report.setHealthAssessmentId(dto.getId().toString());//健康评估记录主键ID(逻辑外键)
        report.setAssessorName(SecurityUtils.getUsername());//评估员名称(当前登录用户)
        report.setDailyActivityLevel(dailyActivityLevel);
        report.setMentalStatusLevel(mentalStatusLevel);
        report.setPerceptionCommunicationLevel(perceptionCommunicationLevel);
        report.setSocialParticipationLevel(socialParticipationLevel);
        report.setInitialAbilityLevel(elderLevelResultVO.getPreLevel());
        report.setFinalAbilityLevel(elderLevelResultVO.getFinalLevel());
        report.setLevelChangeReason(elderLevelResultVO.getReason());
        iHealthAssessmentReportService.save(report);
    //        4.第二阶段 AI 分析：体检报告分析
    //        1）从阿里云 OSS 文件存储下载老人上传的体检 PDF 报告文件。
        InputStream inputStream = ossAliyunFileStorageService.download(dto.getHealthAssessmentDto().getRecent30Days().getMedicalReport());
        //        2）读取解析 PDF，提取出体检报告全部文本内容。
        String pdfContent = PDFUtil.pdfToString(inputStream);
        //        3）将体检文本内容组装成第二套 AI 提示词 prompt。
        String  userPrompt2 = String.format(SystemConstant.USER_PROMPT2,pdfContent);
    //        4）调用大模型对体检报告做专业分析。
    //        5）解析 AI 返回结果，拿到健康评分、风险等级、报告总结、身体异常项、各身体系统评分。
        HealthReportResultVO healthReportResultVO = this.chatBigModel(userPrompt2, HealthReportResultVO.class);
        //        5.保存评估结果到评估结果表

    //        把两阶段 AI 产出的全部分析结果回填到评估报告记录中。
    //        业务判断规则：
    //        1）是否建议入住判断规则：健康评分≥60 分，标记为建议入住；低于 60 不建议入住。
    //        2）依据健康分数计算建议护理等级：90 分以上三级护理，80 以上二级，70 以上一级，60 以上特级，低于 60 不建议入住。
    //        3）报告入住状态统一标记为未入住。
    //        4）报告表所有字段根据实际情况，该有值的尽量都赋值
        Double healthScore = healthReportResultVO.getHealthScore();
        report.setHealthScore(healthScore.toString());
        report.setRiskLevel(healthReportResultVO.getRiskLevel());
        report.setReportSummary(healthReportResultVO.getSummarize());
        report.setAbnormalAnalysis(JSONUtil.toJsonStr(healthReportResultVO.getAbnormalData()));
        report.setSystemScore((JSONUtil.toJsonStr(healthReportResultVO.getSystemScore())));
        Integer coreSuggestion = healthScore >= 60 ? 1 :0;//判断是否建议入住
        report.setCoreSuggestion(coreSuggestion);
        report.setCheckInStatus(0);//未入住
        String careLevel = this.getCareLevel(healthScore);//建议护理等级
        report.setRecommendedCareLevel(careLevel);
        iHealthAssessmentReportService.updateById(report);

    //        6.更新评估主记录的状态与入住建议，修改评估主表数据，写入是否建议入住的标记，把评估进度设置为【评估已完成】。

        HealthAssessment healthAssessment = new HealthAssessment();
        healthAssessment.setId(dto.getId());
        healthAssessment.setCoreSuggestion(coreSuggestion);//是否建议入住
        healthAssessment.setEvaluationProgress(1);//评估进度：0-评估中，1-已完成，2-已取消
        this.updateById(healthAssessment);
        //        7.返回评估 ID 整个业务流程结束，返回本次评估记录的唯一ID。
        return dto.getId();
    }

    /**
     *  根据等级中文名称获取等级数值
     * @param ability
     * @return
     */
    private String getCurrentLevel(String ability){
        String level="";
        switch (ability){
            case "能力完好":
                level="0";
                break;
            case "轻度受损":
                level="1";
                break;
            case "中度受损":
                level="2";
                break;
            case "重度受损":
                level="3";
                break;

        }
        return level;

    }

    /**
     * 从 LLM 响应中提取首个 JSON 对象。
     * <p>
     * 处理三种常见情况：纯 JSON、带 markdown ```json 围栏、JSON 前后夹杂说明文字。
     * </p>
     */
    private String extractJson(String content) {
        // 优先匹配 ```json ... ``` 代码块
        Pattern fence = Pattern.compile("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```", Pattern.CASE_INSENSITIVE);
        Matcher fm = fence.matcher(content);
        if (fm.find()) {
            return fm.group(1);
        }
        // 否则取首个 {...} 顶层对象
        Pattern obj = Pattern.compile("\\{[\\s\\S]*\\}");
        Matcher om = obj.matcher(content);
        if (om.find()) {
            return om.group();
        }
        return content;
    }
    /**
     * 与大模型交互对话
     * @param prompt
     * @param type
     * @return
     * @param <T>
     */

    private <T> T chatBigModel(String prompt,Class<T> type){
        String content = chatClientHealth.prompt()
                .user(prompt)
                .call()
                .content();
        String json = extractJson(content);
        T t = JSONUtil.toBean(json, type);
        return t;
    }

    /**
     * 根据健康评分获取建议护理等级
     * @param healthScore
     * @return
     */
    private String getCareLevel(Double healthScore) {
        String level = "不建议入住";
        if (healthScore>=90) {
            level = "三级护理";
        } else if (healthScore>=80) {
            level = "二级护理";
        } else if (healthScore>=70){
            level = "一级护理";
        } else if (healthScore>=60) {
            level = "特级护理";
        }
        return level;
    }


    /**
     * 批量删除健康评估记录
     * 
     * @param ids 需要删除的健康评估记录主键
     * @return 结果
     */
    @Override
    public int deleteHealthAssessmentByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除健康评估记录信息
     * 
     * @param id 健康评估记录主键
     * @return 结果
     */
    @Override
    public int deleteHealthAssessmentById(Long id)
    {
        return removeById(id)? 1 : 0;
    }
}
