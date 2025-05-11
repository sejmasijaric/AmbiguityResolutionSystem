package publisherpackage;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Service
public class MqttServiceImpl implements MqttService {
    private static final Logger logger = LoggerFactory.getLogger(MqttServiceImpl.class);
    private MqttClient client;
    private final String brokerUrl;

    public MqttServiceImpl(@Value("${mqtt.broker.url}") String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    @PreDestroy
    public void cleanup() throws MqttException {
        disconnect();
    }

    @Override
    public void publish(String topic, String message) throws MqttException {
        connect();
        MqttMessage mqttMessage = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(1);
        client.publish(topic, mqttMessage);
        logger.info("Published message to topic: " + topic);
    }

    @Override
    public void connect() throws MqttException {
        logger.info("Connecting to MQTT broker... ");
        if (client==null || !client.isConnected()) {
            this.client = new MqttClient(brokerUrl, MqttClient.generateClientId());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            client.connect();
            logger.info("Connected to MQTT broker at " + brokerUrl);
        } else {
            logger.warn("Already connected to MQTT broker.");
        }
    }

    @Override
    public void disconnect() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            logger.info("Disconnected from MQTT broker.");
        } else {
            logger.warn("Not connected to MQTT broker.");
        }
    }

    @Override
    public boolean isConnected() {
        return client != null && client.isConnected();
    }
}
