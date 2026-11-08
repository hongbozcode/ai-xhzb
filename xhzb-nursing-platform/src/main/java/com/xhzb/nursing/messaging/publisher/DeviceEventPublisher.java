package com.xhzb.nursing.messaging.publisher;

import com.xhzb.nursing.config.MqConfig;
import com.xhzb.nursing.domain.vo.IotDaDeviceReportVo;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class DeviceEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /** 发布 IoTDA 上报对象，等待 RabbitMQ 确认。 */
    public void publish(IotDaDeviceReportVo report) {
        CorrelationData correlation = new CorrelationData();
        rabbitTemplate.convertAndSend(
                MqConfig.IOT_EXCHANGE,
                MqConfig.TELEMETRY_ROUTING_KEY,
                report,
                correlation
        );

        CorrelationData.Confirm confirm;
        try {
            confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AmqpException("等待 RabbitMQ 发布确认时被中断", exception);
        } catch (TimeoutException exception) {
            throw new AmqpException("等待 RabbitMQ 发布确认超时", exception);
        } catch (ExecutionException exception) {
            throw new AmqpException("获取 RabbitMQ 发布确认失败", exception);
        }

        ReturnedMessage returned = correlation.getReturned();
        if (returned != null) {
            throw new AmqpException("设备消息被 RabbitMQ 退回：" + returned.getReplyText());
        }
        if (!confirm.isAck()) {
            throw new AmqpException("RabbitMQ 拒绝发布设备消息：" + confirm.getReason());
        }
    }
}
