package com.xhzb.nursing.messaging.listener;

import com.xhzb.common.exception.ServiceException;
import com.xhzb.nursing.config.MqConfig;
import com.xhzb.nursing.domain.AlertData;
import com.xhzb.nursing.domain.dto.AlertCreatedEvent;
import com.xhzb.nursing.domain.vo.IotDaDeviceReportVo;
import com.xhzb.nursing.service.AlertSmsService;
import com.xhzb.nursing.service.IAlertDataService;
import com.xhzb.nursing.service.IDeviceDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceTelemetryListener {

    private final IDeviceDataService deviceDataService;
    private final IAlertDataService alertDataService;
    private final AlertSmsService alertSmsService;

    /** 消费 IoTDA 设备上报并保存设备数据。 */
    @RabbitListener(autoStartup = "${xhzb.iot.demo-consumers:true}", bindings = @QueueBinding(
            value = @Queue(value = MqConfig.DEVICE_DATA_QUEUE, durable = "true"),
            exchange = @Exchange(
                    value = MqConfig.IOT_EXCHANGE,
                    type = ExchangeTypes.TOPIC,
                    durable = "true"
            ),
            key = MqConfig.TELEMETRY_ROUTING_KEY
    ))
    public void handleDeviceData(IotDaDeviceReportVo report) {
        deviceDataService.saveDeviceReport(report);
    }

    /** 消费已提交的报警事件并发送短信通知。 */
    @RabbitListener(autoStartup = "${xhzb.iot.demo-consumers:true}", bindings = @QueueBinding(
            value = @Queue(value = MqConfig.DEVICE_ALERT_QUEUE, durable = "true"),
            exchange = @Exchange(
                    value = MqConfig.IOT_EXCHANGE,
                    type = ExchangeTypes.TOPIC,
                    durable = "true"
            ),
            key = MqConfig.ALERT_CREATED_ROUTING_KEY
    ))
    public void handleDeviceAlert(AlertCreatedEvent event, Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        if (!MqConfig.ALERT_CREATED_ROUTING_KEY.equals(routingKey)) {
            log.info("忽略告警队列升级前积压的上报副本，routingKey={}", routingKey);
            return;
        }
        List<Long> alertIds = event.getAlertDataIds();
        if (alertIds == null || alertIds.isEmpty() || alertIds.stream().anyMatch(Objects::isNull)) {
            log.error("报警事件未携带有效记录 ID");
            throw new AmqpRejectAndDontRequeueException("报警事件未携带有效记录 ID");
        }
        List<AlertData> alerts = alertDataService.listByIds(alertIds);
        if (alerts.isEmpty()) {
            log.error("报警通知对应的记录已不存在，alertIds={}", alertIds);
            return;
        }
        try {
            int submittedCount = alertSmsService.sendAlerts(alerts);
            log.info("已处理报警通知事件，报警记录数={}，提交 SMS8 的短信数={}", alerts.size(), submittedCount);
        } catch (ServiceException exception) {
            // 请求超时时 SMS8 可能已接收短信，避免 RabbitMQ 重投后重复发送。
            log.error("报警短信提交失败，alertIds={}, sms8Code={}, reason={}",
                    alertIds, exception.getCode(), exception.getMessage());
            throw new AmqpRejectAndDontRequeueException("报警短信提交失败", exception);
        }
    }
}
