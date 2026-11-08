package com.xhzb.nursing.domain.vo;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** IoTDA 设备属性和设备消息上报的通知正文。 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class IotDaDeviceReportVo {

    /** IoTDA 资源类型，如 device.property 或 device.message */
    private String resource;

    /** IoTDA 事件类型 */
    private String event;

    /** 平台事件时间，保留 IoTDA 的 UTC 格式 */
    private String eventTime;

    /** 毫秒精度的平台事件时间 */
    private String eventTimeMs;

    /** IoTDA 报文中的请求标识 */
    private String requestId;

    /** 转发规则将设备 ID 投影到顶层时使用。 */
    private String deviceId;

    /** 通知头及正文 */
    private NotifyData notifyData;

    /** 保留转发规则增加的顶层字段 */
    @JsonAnyGetter
    @JsonAnySetter
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final Map<String, Object> extraFields = new LinkedHashMap<>();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NotifyData {

        /** 设备和产品标识 */
        private Header header;

        /** 设备上报内容 */
        private Body body;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Header {

        /** IoTDA 设备 ID */
        private String deviceId;

        /** IoTDA 产品 ID */
        private String productId;

        /** 应用或资源空间 ID */
        private String appId;

        /** 关联的网关设备 ID */
        private String gatewayId;

        /** 设备节点标识 */
        private String nodeId;

        /** 设备标签，保留 tag_key 和 tag_value */
        private List<Map<String, String>> tags;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Body {

        /** 设备属性上报中的服务列表 */
        private List<Service> services;

        /** 设备消息上报正文里的 MQTT Topic */
        private String topic;

        /** 设备消息上报内容，可为对象、数组或文本 */
        private JsonNode content;

        /** 设备消息上报附带的 MQTT 属性 */
        private Map<String, Object> properties;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Service {

        /** 产品模型定义的服务 ID */
        private String serviceId;

        /** 该服务的设备采集时间，保留 IoTDA 的 UTC 格式 */
        private String eventTime;

        /** 产品模型定义的属性名和值 */
        private Map<String, Object> properties;
    }
}
