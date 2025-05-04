package publisherpackage;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public interface PublishingServiceClient {


    void publishResolvedAmbiguousEvent(String mlOutput, String originalJsonEvents) throws MqttException;
    void publishUnambiguousEvent(String message) throws MqttException;

}
