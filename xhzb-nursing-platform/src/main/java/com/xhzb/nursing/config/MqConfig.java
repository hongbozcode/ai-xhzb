package com.xhzb.nursing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class MqConfig {

    public static final String IOT_EXCHANGE = "xhzb.iot.events";
    public static final String DEVICE_DATA_QUEUE = "xhzb.iot.device-data";
    public static final String DEVICE_ALERT_QUEUE = "xhzb.iot.device-alert";
    public static final String TELEMETRY_ROUTING_KEY = "device.telemetry.reported";
    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        jackson2JsonMessageConverter.setCreateMessageIds(true);
        return jackson2JsonMessageConverter;
    }

    @Bean
    public RabbitTemplateCustomizer rabbitReturnsLogger() {
        return template -> template.setReturnsCallback(returned ->
                log.error("RabbitMQ 消息无法路由，exchange={}, routingKey={}, code={}, reason={}",
                        returned.getExchange(),
                        returned.getRoutingKey(),
                        returned.getReplyCode(),
                        returned.getReplyText()));
    }
}
