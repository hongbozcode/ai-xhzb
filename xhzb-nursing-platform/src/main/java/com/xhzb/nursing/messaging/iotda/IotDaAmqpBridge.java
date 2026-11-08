package com.xhzb.nursing.messaging.iotda;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.xhzb.framework.config.properties.HuaWeiIotConfigProperties;
import com.xhzb.nursing.config.MqConfig;
import com.xhzb.nursing.domain.vo.IotDaDeviceReportVo;
import com.xhzb.nursing.messaging.publisher.DeviceEventPublisher;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.qpid.jms.JmsConnection;
import org.apache.qpid.jms.JmsConnectionExtensions;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.JmsConnectionListener;
import org.apache.qpid.jms.JmsQueue;
import org.apache.qpid.jms.message.JmsInboundMessageDispatch;
import org.apache.qpid.jms.message.JmsMessageSupport;
import org.apache.qpid.jms.transports.TransportOptions;
import org.apache.qpid.jms.transports.TransportSupport;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageFormatException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author itcast
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "huaweicloud", name = "bridge-enabled", havingValue = "true")
public class IotDaAmqpBridge implements ApplicationRunner {

    // Qpid JMS 扩展模式，acknowledge() 只确认当前消息。
    private static final int INDIVIDUAL_ACKNOWLEDGE = 101;
    private static final long RECEIVE_POLL_MILLIS = 10;
    private static final long PUBLISH_RETRY_SECONDS = 1;

    private final HuaWeiIotConfigProperties huaWeiIotConfigProperties;
    private final DeviceEventPublisher deviceEventPublisher;
    private final ObjectMapper objectMapper;
    private final AmqpAdmin amqpAdmin;
    private volatile Connection connection;
    private volatile boolean running;
    private Thread receiverThread;

    //控制台服务端订阅中消费组状态页客户端ID一栏将显示clientId参数。
    //建议使用机器UUID、MAC地址、IP等唯一标识等作为clientId。便于您区分识别不同的客户端。
    private static String clientId;

    static {
        try {
            clientId = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        start();
    }

    public void start() throws Exception {
        if (huaWeiIotConfigProperties.getConnectionCount() != 1) {
            throw new IllegalStateException("IoTDA 桥接需要一个源消费者，请将 huaweicloud.connection-count 设置为 1");
        }
        if (huaWeiIotConfigProperties.getBridgeWorkers() < 1 || huaWeiIotConfigProperties.getBridgeMaxInFlight() < 1) {
            throw new IllegalStateException("IoTDA 桥接的发布线程数和在途消息上限必须大于 0");
        }
        // 首次连接 RabbitMQ 时声明监听器定义的队列与绑定，再启动 IoTDA 消费。
        if (amqpAdmin.getQueueInfo(MqConfig.DEVICE_DATA_QUEUE) == null
                || amqpAdmin.getQueueInfo(MqConfig.DEVICE_ALERT_QUEUE) == null) {
            throw new IllegalStateException("RabbitMQ 设备上报队列尚未就绪");
        }

        Subscription subscription = openSubscription();
        connection = subscription.connection();
        running = true;
        receiverThread = new Thread(() -> runReceiver(subscription), "iotda-receiver");
        receiverThread.start();
        log.info("IoTDA AMQP 桥接已启动，逐条确认，发布线程数={}，在途消息上限={}",
                huaWeiIotConfigProperties.getBridgeWorkers(), huaWeiIotConfigProperties.getBridgeMaxInFlight());
    }

    private Subscription openSubscription() throws Exception {
        Connection sourceConnection = getConnection();
        try {
            ((JmsConnection) sourceConnection).addConnectionListener(myJmsConnectionListener);
            Session session = sourceConnection.createSession(false, INDIVIDUAL_ACKNOWLEDGE);
            MessageConsumer consumer = newConsumer(session, sourceConnection, huaWeiIotConfigProperties.getQueueName());
            sourceConnection.start();
            return new Subscription(sourceConnection, consumer);
        } catch (Exception exception) {
            try {
                sourceConnection.close();
            } catch (JMSException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    @PreDestroy
    public void close() throws JMSException {
        running = false;
        if (receiverThread != null) {
            receiverThread.interrupt();
        }
        Connection activeConnection = connection;
        if (activeConnection != null) {
            activeConnection.close();
        }
    }

    /**
     * 创建amqp连接
     *
     * @return amqp连接
     */
    private Connection getConnection() throws Exception {
        String connectionUrl = generateConnectUrl();
        JmsConnectionFactory cf = new JmsConnectionFactory(connectionUrl);
        // 信任服务端
        TransportOptions to = new TransportOptions();
        to.setTrustAll(true);
        cf.setSslContext(TransportSupport.createJdkSslContext(to));
        String userName = "accessKey=" + huaWeiIotConfigProperties.getAccessKey();
        cf.setExtension(JmsConnectionExtensions.USERNAME_OVERRIDE.toString(), (connection, uri) -> {
            // IoTDA的userName组成格式如下：“accessKey=${accessKey}|timestamp=${timestamp}”
            String newUserName = userName;
            if (connection instanceof JmsConnection) {
                newUserName = ((JmsConnection) connection).getUsername();
            }
            return newUserName + "|timestamp=" + System.currentTimeMillis();
        });

        // 创建连接。
        return cf.createConnection(userName, huaWeiIotConfigProperties.getAccessCode());
    }

    /**
     * 生成amqp连接地址
     *
     * @return amqp连接地址
     */
    public String generateConnectUrl() {
        String uri = MessageFormat.format("{0}://{1}:{2}",
                (huaWeiIotConfigProperties.isUseSsl() ? "amqps" : "amqp"),
                huaWeiIotConfigProperties.getHost(),
                String.valueOf(huaWeiIotConfigProperties.getPort()));
        Map<String, String> uriOptions = new HashMap<>();
        uriOptions.put("amqp.vhost", huaWeiIotConfigProperties.getVhost());
        uriOptions.put("amqp.idleTimeout", String.valueOf(huaWeiIotConfigProperties.getIdleTimeout()));
        uriOptions.put("amqp.saslMechanisms", huaWeiIotConfigProperties.getSaslMechanisms());

        Map<String, String> jmsOptions = new HashMap<>();
        int prefetch = Math.min(huaWeiIotConfigProperties.getQueuePrefetch(), huaWeiIotConfigProperties.getBridgeMaxInFlight());
        jmsOptions.put("jms.prefetchPolicy.queuePrefetch", String.valueOf(prefetch));
        if (CharSequenceUtil.isNotBlank(clientId)) {
            jmsOptions.put("jms.clientID", clientId);
        } else {
            jmsOptions.put("jms.clientID", UUID.randomUUID().toString());
        }
        jmsOptions.put("failover.reconnectDelay", String.valueOf(huaWeiIotConfigProperties.getReconnectDelay()));
        jmsOptions.put("failover.maxReconnectDelay", String.valueOf(huaWeiIotConfigProperties.getMaxReconnectDelay()));
        if (huaWeiIotConfigProperties.getMaxReconnectAttempts() > 0) {
            jmsOptions.put("failover.maxReconnectAttempts", String.valueOf(huaWeiIotConfigProperties.getMaxReconnectAttempts()));
        }
        if (huaWeiIotConfigProperties.getExtendedOptions() != null) {
            for (Map.Entry<String, String> option : huaWeiIotConfigProperties.getExtendedOptions().entrySet()) {
                if (option.getKey().startsWith("amqp.") || option.getKey().startsWith("transport.")) {
                    uriOptions.put(option.getKey(), option.getValue());
                } else {
                    jmsOptions.put(option.getKey(), option.getValue());
                }
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(uriOptions.entrySet().stream()
                .map(option -> MessageFormat.format("{0}={1}", option.getKey(), option.getValue()))
                .collect(Collectors.joining("&", "failover:(" + uri + "?", ")")));
        stringBuilder.append(jmsOptions.entrySet().stream()
                .map(option -> MessageFormat.format("{0}={1}", option.getKey(), option.getValue()))
                .collect(Collectors.joining("&", "?", "")));
        return stringBuilder.toString();
    }

    /**
     * 创建消费者
     *
     * @param session    session
     * @param connection amqp连接
     * @param queueName  队列名称
     * @return 消费者
     */
    public MessageConsumer newConsumer(Session session, Connection connection, String queueName) throws Exception {
        if (connection == null || !(connection instanceof JmsConnection) || ((JmsConnection) connection).isClosed()) {
            throw new Exception("create consumer failed,the connection is disconnected.");
        }

        return session.createConsumer(new JmsQueue(queueName));
    }

    private void runReceiver(Subscription subscription) {
        while (running) {
            try {
                if (subscription == null) {
                    TimeUnit.MILLISECONDS.sleep(huaWeiIotConfigProperties.getReconnectDelay());
                    subscription = openSubscription();
                    connection = subscription.connection();
                }
                try (Connection sourceConnection = subscription.connection()) {
                    receiveAndPublish(subscription.consumer());
                } finally {
                    subscription = null;
                    connection = null;
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception exception) {
                if (running) {
                    log.warn("IoTDA 接收线程将重新订阅，未确认消息留给源队列重投", exception);
                }
            }
        }
    }

    private void receiveAndPublish(MessageConsumer consumer) throws JMSException, InterruptedException {
        int maxInFlight = huaWeiIotConfigProperties.getBridgeMaxInFlight();
        ScheduledExecutorService publishers = Executors.newScheduledThreadPool(
                huaWeiIotConfigProperties.getBridgeWorkers(), new CustomizableThreadFactory("iotda-publisher-"));
        BlockingQueue<PendingDelivery> published = new LinkedBlockingQueue<>();
        Map<String, Deque<PendingDelivery>> pendingByDevice = new HashMap<>();
        ObjectReader reportReader = objectMapper.readerFor(IotDaDeviceReportVo.class);
        int inFlight = 0;
        try {
            while (running) {
                boolean windowFull = inFlight >= maxInFlight;
                PendingDelivery completed = windowFull
                        ? published.poll(RECEIVE_POLL_MILLIS, TimeUnit.MILLISECONDS) : published.poll();
                if (completed != null) {
                    // 只有接收线程操作 JMS。确认当前消息后才提交同一设备的下一条。
                    completed.source().acknowledge();
                    Deque<PendingDelivery> deviceQueue = pendingByDevice.get(completed.deviceId());
                    deviceQueue.removeFirst();
                    inFlight--;
                    if (deviceQueue.isEmpty()) {
                        pendingByDevice.remove(completed.deviceId());
                    } else {
                        schedulePublish(publishers, published, deviceQueue.getFirst(), 0);
                    }
                    continue;
                }
                if (windowFull) {
                    continue;
                }

                Message source = consumer.receive(RECEIVE_POLL_MILLIS);
                if (source == null) {
                    continue;
                }
                IotDaDeviceReportVo report;
                try {
                    String content = source.getBody(String.class);
                    report = content == null ? null : reportReader.readValue(content);
                    if (report == null) {
                        throw new MessageFormatException("IoTDA 上报正文为空");
                    }
                } catch (JsonProcessingException | MessageFormatException exception) {
                    // 沿用源队列重投语义，释放格式错误的消息而非接受它。
                    log.warn("IoTDA 上报正文无法解析，释放消息等待重投，异常={}", exception.getClass().getSimpleName());
                    source.setIntProperty(JmsMessageSupport.JMS_AMQP_ACK_TYPE, JmsMessageSupport.RELEASED);
                    source.acknowledge();
                    TimeUnit.MILLISECONDS.sleep(100);
                    continue;
                }

                String deviceId = deviceIdOf(report);
                PendingDelivery pending = new PendingDelivery(source, report, deviceId);
                Deque<PendingDelivery> deviceQueue = pendingByDevice.computeIfAbsent(deviceId, key -> new ArrayDeque<>());
                deviceQueue.addLast(pending);
                inFlight++;
                if (deviceQueue.size() == 1) {
                    schedulePublish(publishers, published, pending, 0);
                }
            }
        } finally {
            publishers.shutdownNow();
            // 重订阅前等旧任务退出，避免它们和重投的同设备消息同时发布。
            while (running && !publishers.awaitTermination(1, TimeUnit.SECONDS)) {
                log.debug("等待上一轮 IoTDA 发布任务退出");
            }
        }
    }

    private String deviceIdOf(IotDaDeviceReportVo report) {
        IotDaDeviceReportVo.NotifyData notifyData = report.getNotifyData();
        if (notifyData != null && notifyData.getHeader() != null
                && StringUtils.hasText(notifyData.getHeader().getDeviceId())) {
            return notifyData.getHeader().getDeviceId();
        }
        return StringUtils.hasText(report.getDeviceId()) ? report.getDeviceId() : null;
    }

    private void schedulePublish(ScheduledExecutorService publishers, BlockingQueue<PendingDelivery> published,
                                 PendingDelivery pending, long delaySeconds) {
        publishers.schedule(() -> {
            try {
                deviceEventPublisher.publish(pending.report());
                published.add(pending);
            } catch (RuntimeException exception) {
                if (running && !publishers.isShutdown()) {
                    log.warn("IoTDA 上报发布失败，将重试设备队首消息，deviceId={}", pending.deviceId(), exception);
                    schedulePublish(publishers, published, pending, PUBLISH_RETRY_SECONDS);
                }
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private record Subscription(Connection connection, MessageConsumer consumer) {
    }

    private record PendingDelivery(Message source, IotDaDeviceReportVo report, String deviceId) {
    }

    private final JmsConnectionListener myJmsConnectionListener = new JmsConnectionListener() {
        /**
         * 连接成功建立。
         */
        @Override
        public void onConnectionEstablished(URI remoteURI) {
            log.info("onConnectionEstablished, remoteUri:{}", remoteURI);
        }

        /**
         * 尝试过最大重试次数之后，最终连接失败。
         */
        @Override
        public void onConnectionFailure(Throwable error) {
            log.error("onConnectionFailure, {}", error.getMessage());
        }

        /**
         * 连接中断。
         */
        @Override
        public void onConnectionInterrupted(URI remoteURI) {
            log.info("onConnectionInterrupted, remoteUri:{}", remoteURI);
        }

        /**
         * 连接中断后又自动重连上。
         */
        @Override
        public void onConnectionRestored(URI remoteURI) {
            log.info("onConnectionRestored, remoteUri:{}", remoteURI);
        }

        @Override
        public void onInboundMessage(JmsInboundMessageDispatch envelope) {
        }

        @Override
        public void onSessionClosed(Session session, Throwable cause) {
        }

        @Override
        public void onConsumerClosed(MessageConsumer consumer, Throwable cause) {
        }

        @Override
        public void onProducerClosed(MessageProducer producer, Throwable cause) {
        }
    };
}
