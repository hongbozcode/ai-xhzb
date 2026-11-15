package com.xhzb.nursing.service;

import cn.hutool.core.util.StrUtil;
import com.xhzb.common.core.domain.entity.SysUser;
import com.xhzb.nursing.client.MessagesClient;
import com.xhzb.nursing.domain.AlertData;
import com.xhzb.nursing.domain.Bed;
import com.xhzb.nursing.domain.Elder;
import com.xhzb.nursing.domain.Floor;
import com.xhzb.nursing.domain.Room;
import com.xhzb.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** 根据已落库的告警收件人和设备位置生成短信。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertSmsService {
    private static final int ELDER_ALERT = 0;
    private static final int DEVICE_ALERT = 1;
    private static final int WEARABLE_DEVICE = 0;
    private static final int FLOOR_LOCATION = 0;
    private static final int ROOM_LOCATION = 1;
    private static final int BED_LOCATION = 2;
    private static final Pattern CHINESE_MOBILE = Pattern.compile("1[3-9][0-9]{9}");
    private static final Pattern INTERNATIONAL_NUMBER = Pattern.compile("\\+[1-9][0-9]{1,14}");
    private static final String ELDER_TEMPLATE =
            "【星海智伴】{name}，请注意：{location}老人在使用“{device}”时出现了异常数据，请及时查看并处理。";
    private static final String DEVICE_TEMPLATE =
            "【星海智伴】{name}，请注意：{location}设备“{device}”出现了异常，请及时查看并处理。";

    private final ISysUserService userService;
    private final IElderService elderService;
    private final IBedService bedService;
    private final IRoomService roomService;
    private final IFloorService floorService;
    private final MessagesClient messagesClient;

    /**
     * 同一次告警按可用手机号去重，批量提交短信。
     *
     * @param alerts 同一次告警为各接收人保存的记录
     * @return 已提交给 SMS8 的短信条数
     */
    public int sendAlerts(List<AlertData> alerts) {
        if (!messagesClient.isEnabled() || alerts.isEmpty()) {
            return 0;
        }

        AlertData alert = alerts.get(0);
        boolean elderAlert = Objects.equals(alert.getType(), ELDER_ALERT);
        if (!elderAlert && !Objects.equals(alert.getType(), DEVICE_ALERT)) {
            log.warn("报警类型无效，跳过短信，alertId={}, type={}", alert.getId(), alert.getType());
            return 0;
        }
        String template = elderAlert ? ELDER_TEMPLATE : DEVICE_TEMPLATE;
        String location = resolveLocation(alert);
        String locationText = StrUtil.isBlank(location) ? "" : "“" + location + "”的";
        String device = elderAlert
                ? StrUtil.blankToDefault(alert.getProductName(), alert.getDeviceName())
                : StrUtil.blankToDefault(alert.getDeviceName(), alert.getProductName());
        String deviceText = StrUtil.blankToDefault(device, "物联网设备");

        Map<String, String> messages = new LinkedHashMap<>();
        List<Long> userIds = alerts.stream().map(AlertData::getUserId).filter(Objects::nonNull).distinct().toList();
        for (Long userId : userIds) {
            SysUser user = userService.selectUserById(userId);
            if (user == null || !"0".equals(user.getStatus()) || !"0".equals(user.getDelFlag())) {
                log.warn("报警短信接收账号不存在或不可用，alertId={}, userId={}", alert.getId(), userId);
                continue;
            }
            String number = normalizeNumber(user.getPhonenumber());
            if (number == null) {
                log.warn("报警短信接收账号缺少有效手机号，alertId={}, userId={}", alert.getId(), userId);
                continue;
            }
            String name = StrUtil.blankToDefault(user.getNickName(), "您好");
            messages.putIfAbsent(number, StrUtil.format(template,
                    Map.of("name", name, "location", locationText, "device", deviceText)));
        }
        messagesClient.sendMessage(messages);
        return messages.size();
    }

    /** 老人随身设备绑定老人 ID，固定设备绑定楼层、房间或床位 ID。 */
    private String resolveLocation(AlertData alert) {
        String binding = alert.getAccessLocation();
        if (StrUtil.isBlank(binding)) {
            return null;
        }
        Long locationId;
        try {
            locationId = Long.valueOf(binding.trim());
        } catch (NumberFormatException exception) {
            return null;
        }

        if (Objects.equals(alert.getLocationType(), WEARABLE_DEVICE)) {
            Elder elder = elderService.selectElderById(locationId);
            if (elder == null) {
                return null;
            }
            return withSuffix(elder.getBedNumber(), "床位");
        }
        Integer physicalType = alert.getPhysicalLocationType();
        if (Objects.equals(physicalType, BED_LOCATION)) {
            Bed bed = bedService.selectBedById(locationId);
            return bed == null ? null : withSuffix(bed.getBedNumber(), "床位");
        }
        if (Objects.equals(physicalType, ROOM_LOCATION)) {
            Room room = roomService.selectRoomById(locationId);
            return room == null ? null : withSuffix(room.getCode(), "房间");
        }
        if (Objects.equals(physicalType, FLOOR_LOCATION)) {
            Floor floor = floorService.selectFloorById(locationId);
            return floor == null ? null : floor.getName();
        }
        return null;
    }

    private String withSuffix(String value, String suffix) {
        return StrUtil.isBlank(value) || value.endsWith(suffix) ? value : value + suffix;
    }

    private String normalizeNumber(String phone) {
        String number = StrUtil.trimToEmpty(phone);
        if (CHINESE_MOBILE.matcher(number).matches()) {
            return "+86" + number;
        }
        return INTERNATIONAL_NUMBER.matcher(number).matches() ? number : null;
    }
}
