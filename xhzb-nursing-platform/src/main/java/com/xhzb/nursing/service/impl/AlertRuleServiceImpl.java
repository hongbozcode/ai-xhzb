package com.xhzb.nursing.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.common.constant.CacheConstants;
import com.xhzb.nursing.domain.AlertRule;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.mapper.AlertRuleMapper;
import com.xhzb.nursing.messaging.publisher.DeviceEventPublisher;
import com.xhzb.nursing.service.IAlertDataService;
import com.xhzb.nursing.service.IAlertRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 报警规则Service业务层处理
 * 
 * @author hongbozcode
 * @date 2026-11-09
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleMapper, AlertRule> implements IAlertRuleService
{
    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String ALL_DEVICES = "-1";
    private static final String ALERT_STATE_PREFIX = "iot:alert:state:";
    private static final Duration COUNT_STATE_TTL = Duration.ofMinutes(3);

    private final AlertRuleMapper alertRuleMapper;
    private final StringRedisTemplate redis;
    private final IAlertDataService alertDataService;
    private final DeviceEventPublisher deviceEventPublisher;

    /** 扫描设备最新上报。由现有调度任务保证同一时间只有一轮执行。 */
    @Override
    public void alertFilter()
    {
        log.info("开始扫描设备报警规则");
        List<AlertRule> enabledRules = lambdaQuery().eq(AlertRule::getStatus, 1).list();
        if (enabledRules.isEmpty())
        {
            log.info("没有启用的报警规则，结束本轮扫描");
            return;
        }
        List<String> cachedReports = redis.<String, String>opsForHash().values(CacheConstants.IOT_DATA_LIST);
        if (cachedReports == null || cachedReports.isEmpty())
        {
            log.error("Redis 中没有设备上报缓存，结束本轮扫描");
            return;
        }
        List<DeviceData> reports = readReports(cachedReports);
        if (reports.isEmpty())
        {
            log.error("缓存中没有可读取的上报属性，结束本轮扫描");
            return;
        }

        Map<RuleScope, List<AlertRule>> rulesByScope = enabledRules.stream().collect(Collectors.groupingBy(
                rule -> new RuleScope(rule.getProductKey(), rule.getFunctionId(), rule.getIotId())));
        LocalDateTime scanTime = LocalDateTime.now(REPORT_ZONE);
        long scanMinute = toEpochMinute(scanTime);
        for (DeviceData report : reports)
        {
            if (report == null || !StringUtils.hasText(report.getIotId())
                    || !StringUtils.hasText(report.getProductKey()) || !StringUtils.hasText(report.getFunctionId()))
            {
                log.error("设备上报缺少设备、产品或属性标识，跳过该属性");
                continue;
            }
            LocalDateTime reportedAt = report.getAlarmTime();
            if (reportedAt == null || reportedAt.isAfter(scanTime) || reportedAt.isBefore(scanTime.minusMinutes(1)))
            {
                log.warn("设备上报时间无效或超过 1 分钟，跳过该属性，iotId={}, functionId={}, reportedAt={}",
                        report.getIotId(), report.getFunctionId(), reportedAt);
                continue;
            }

            RuleScope commonScope = new RuleScope(report.getProductKey(), report.getFunctionId(), ALL_DEVICES);
            RuleScope deviceScope = new RuleScope(report.getProductKey(), report.getFunctionId(), report.getIotId());
            Set<AlertRule> matchingRules = new LinkedHashSet<>(rulesByScope.getOrDefault(commonScope, List.of()));
            matchingRules.addAll(rulesByScope.getOrDefault(deviceScope, List.of()));
            if (matchingRules.isEmpty())
            {
                log.info("未匹配到报警规则，跳过该属性，iotId={}, functionId={}",
                        report.getIotId(), report.getFunctionId());
                continue;
            }
            for (AlertRule rule : matchingRules)
            {
                try
                {
                    evaluateRule(rule, report, reportedAt, scanMinute);
                }
                catch (RuntimeException exception)
                {
                    log.error("报警规则处理失败，ruleId={}, iotId={}, functionId={}",
                            rule.getId(), report.getIotId(), report.getFunctionId(), exception);
                }
            }
        }
        log.info("设备报警规则扫描结束，启用规则数={}，上报属性数={}", enabledRules.size(), reports.size());
    }

    /** 每个设备的缓存值是一次上报拆分得到的属性数组。 */
    private List<DeviceData> readReports(List<String> cachedReports)
    {
        List<DeviceData> reports = new ArrayList<>();
        for (String cachedReport : cachedReports)
        {
            try
            {
                List<DeviceData> batch = JSON.parseArray(cachedReport, DeviceData.class);
                if (batch != null)
                {
                    reports.addAll(batch);
                }
            }
            catch (RuntimeException exception)
            {
                log.error("设备上报缓存无法解析，跳过该条缓存，异常类型={}", exception.getClass().getSimpleName());
            }
        }
        return reports;
    }

    /** 先更新连续异常计数，沉默期只限制报警触发。 */
    private void evaluateRule(AlertRule rule, DeviceData report, LocalDateTime reportedAt, long scanMinute)
    {
        Long ruleId = rule.getId();
        String iotId = report.getIotId();
        if (!isEffective(rule.getAlertEffectivePeriod(), reportedAt.toLocalTime().withNano(0)))
        {
            log.info("上报不在报警生效时段，跳过规则，ruleId={}, iotId={}", ruleId, iotId);
            return;
        }
        boolean abnormal = exceedsThreshold(rule, report.getDataValue());
        String key = ALERT_STATE_PREFIX + ruleId + ":" + iotId + ":" + report.getFunctionId();
        AlertState state = readState(key);
        long reportMinute = toEpochMinute(reportedAt);
        if (state != null && reportMinute < state.lastMinute())
        {
            log.error("上报早于上次计数分钟，跳过规则，ruleId={}, iotId={}, reportMinute={}", ruleId, iotId, reportMinute);
            return;
        }
        if (!abnormal)
        {
            if (state != null && state.count() > 0)
            {
                // 正常值清除异常次数，保留已计过的分钟，防止本分钟再次计数。
                storeState(key, new AlertState(state.lastMinute(), 0, state.silentUntilMinute()), scanMinute);
            }
            log.info("上报值未达到报警阈值，连续计数已重置，ruleId={}, iotId={}", ruleId, iotId);
            return;
        }
        if (state != null && state.lastMinute() == reportMinute)
        {
            log.error("该上报分钟已经计数，跳过规则，ruleId={}, iotId={}, reportMinute={}", ruleId, iotId, reportMinute);
            return;
        }

        Integer duration = rule.getDuration();
        int silentMinutes = rule.getAlertSilentPeriod() == null ? 0 : rule.getAlertSilentPeriod();
        if (duration == null || duration < 1 || silentMinutes < 0)
        {
            log.error("报警持续周期或沉默周期配置无效，跳过规则，ruleId={}, duration={}, silentMinutes={}",
                    ruleId, duration, silentMinutes);
            return;
        }
        long count = state != null && state.lastMinute() == reportMinute - 1 ? state.count() + 1 : 1;
        if (state != null && scanMinute < state.silentUntilMinute())
        {
            storeState(key, new AlertState(reportMinute, count, state.silentUntilMinute()), scanMinute);
            log.info("报警处于沉默期，已更新连续异常次数，ruleId={}, iotId={}, count={}", ruleId, iotId, count);
            return;
        }
        if (count < duration)
        {
            storeState(key, new AlertState(reportMinute, count, 0), scanMinute);
            log.info("连续异常次数尚未达到持续周期，ruleId={}, iotId={}, count={}, duration={}",
                    ruleId, iotId, count, duration);
            return;
        }

        // 批量落库返回时事务已提交，保存失败时仍可用原计数重试。
        List<Long> alertIds = alertDataService.saveTriggeredAlerts(rule, report);
        if (alertIds.isEmpty())
        {
            log.error("本次报警未生成接收人记录，跳过沉默期和发布，ruleId={}, iotId={}", ruleId, iotId);
            return;
        }
        log.info("已批量保存设备报警，ruleId={}, iotId={}, 记录数={}", ruleId, iotId, alertIds.size());
        AlertState nextState = new AlertState(reportMinute, count, scanMinute + silentMinutes);
        finishAlert(key, nextState, scanMinute, alertIds);
    }

    /** 提交后保留连续计数，更新沉默截止分钟并发布已落库的记录 ID。 */
    private void finishAlert(String key, AlertState state, long scanMinute, List<Long> alertIds)
    {
        try
        {
            storeState(key, state, scanMinute);
        }
        catch (RuntimeException exception)
        {
            log.error("报警已提交，更新 Redis 沉默状态失败，alertIds={}", alertIds, exception);
        }
        try
        {
            deviceEventPublisher.publishAlertCreated(alertIds);
        }
        catch (RuntimeException exception)
        {
            log.error("报警已提交，发布报警事件失败，alertIds={}", alertIds, exception);
        }
    }

    /** 兼容已缓存的毫秒截止时间，后续写回时使用分钟。 */
    private AlertState readState(String key)
    {
        JSONObject cached = JSON.parseObject(redis.opsForValue().get(key));
        if (cached == null)
        {
            return null;
        }
        Long silentUntilMinute = cached.getLong("silentUntilMinute");
        long deadline = silentUntilMinute == null
                ? TimeUnit.MILLISECONDS.toMinutes(cached.getLongValue("silentUntil")) : silentUntilMinute;
        return new AlertState(cached.getLongValue("lastMinute"), cached.getLongValue("count"), deadline);
    }

    /** 计数和沉默状态一起写回，TTL 用于回收闲置状态。 */
    private void storeState(String key, AlertState state, long scanMinute)
    {
        long remainingSilence = Math.max(0, state.silentUntilMinute() - scanMinute);
        redis.opsForValue().set(key, JSON.toJSONString(state), COUNT_STATE_TTL.plusMinutes(remainingSilence));
    }

    /** 将上报和扫描时间换算成可直接比较的分钟。 */
    private long toEpochMinute(LocalDateTime time)
    {
        return Math.floorDiv(time.atZone(REPORT_ZONE).toEpochSecond(), 60);
    }

    /** 生效时段包含起止时间，也支持跨午夜。 */
    private boolean isEffective(String period, LocalTime reportedTime)
    {
        if (period == null || !period.contains("~"))
        {
            throw new IllegalArgumentException("报警生效时段缺少起止时间");
        }
        String[] bounds = period.split("~", 2);
        LocalTime start = LocalTime.parse(bounds[0].trim());
        LocalTime end = LocalTime.parse(bounds[1].trim());
        if (end.isBefore(start))
        {
            return !reportedTime.isBefore(start) || !reportedTime.isAfter(end);
        }
        return !reportedTime.isBefore(start) && !reportedTime.isAfter(end);
    }

    /** 按规则运算符进行数值比较。 */
    private boolean exceedsThreshold(AlertRule rule, String value)
    {
        if (!StringUtils.hasText(value) || !StringUtils.hasText(rule.getOperator()) || rule.getValue() == null)
        {
            throw new IllegalArgumentException("缺少数值、运算符或报警阈值");
        }
        int comparison = new BigDecimal(value.trim()).compareTo(BigDecimal.valueOf(rule.getValue()));
        return switch (rule.getOperator().trim())
        {
            case ">" -> comparison > 0;
            case ">=", "≥" -> comparison >= 0;
            case "<" -> comparison < 0;
            case "<=", "≤" -> comparison <= 0;
            case "=", "==" -> comparison == 0;
            case "!=", "<>", "≠" -> comparison != 0;
            default -> throw new IllegalArgumentException("不支持的报警规则运算符");
        };
    }

    private record RuleScope(String productKey, String functionId, String iotId) {}

    /** 同一规则和设备属性最近一次计数的分钟、连续次数和沉默截止分钟。 */
    private record AlertState(long lastMinute, long count, long silentUntilMinute) {}

    /**
     * 查询报警规则
     * 
     * @param id 报警规则主键
     * @return 报警规则
     */
    @Override
    public AlertRule selectAlertRuleById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询报警规则列表
     * 
     * @param alertRule 报警规则
     * @return 报警规则
     */
    @Override
    public List<AlertRule> selectAlertRuleList(AlertRule alertRule)
    {
        return alertRuleMapper.selectAlertRuleList(alertRule);
    }

    /**
     * 新增报警规则
     * 
     * @param alertRule 报警规则
     * @return 结果
     */
    @Override
    public int insertAlertRule(AlertRule alertRule)
    {
        return save(alertRule)? 1 : 0;
    }

    /**
     * 修改报警规则
     * 
     * @param alertRule 报警规则
     * @return 结果
     */
    @Override
    public int updateAlertRule(AlertRule alertRule)
    {
        return updateById(alertRule)? 1 : 0;
    }

    /**
     * 批量删除报警规则
     * 
     * @param ids 需要删除的报警规则主键
     * @return 结果
     */
    @Override
    public int deleteAlertRuleByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除报警规则信息
     * 
     * @param id 报警规则主键
     * @return 结果
     */
    @Override
    public int deleteAlertRuleById(Long id)
    {
        return removeById(id)? 1 : 0;
    }
}
