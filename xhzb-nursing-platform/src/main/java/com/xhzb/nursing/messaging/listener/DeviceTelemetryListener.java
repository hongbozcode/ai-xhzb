package com.xhzb.nursing.messaging.listener;

import com.xhzb.nursing.config.MqConfig;
import com.xhzb.nursing.domain.vo.IotDaDeviceReportVo;
import com.xhzb.nursing.service.IDeviceDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceTelemetryListener {

    private final IDeviceDataService deviceDataService;

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

    @RabbitListener(autoStartup = "${xhzb.iot.demo-consumers:true}", bindings = @QueueBinding(
            value = @Queue(value = MqConfig.DEVICE_ALERT_QUEUE, durable = "true"),
            exchange = @Exchange(
                    value = MqConfig.IOT_EXCHANGE,
                    type = ExchangeTypes.TOPIC,
                    durable = "true"
            ),
            key = MqConfig.TELEMETRY_ROUTING_KEY
    ))
    public void handleDeviceAlert(IotDaDeviceReportVo report) {
        log.info("告警队列收到测试消息，resource={}, requestId={}", report.getResource(), report.getRequestId());
    }
}
