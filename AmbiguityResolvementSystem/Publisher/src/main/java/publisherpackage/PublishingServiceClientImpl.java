package publisherpackage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

@Service
public class PublishingServiceClientImpl implements PublishingServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(PublishingServiceClientImpl.class);

    // load configuration
    ConfigLoader config = new ConfigLoader();
    private final String topicUnambiguousEvents = config.get("publisher.topicUnambiguousEvent");
    private final String topicAmbiguousEvents = config.get("publisher.topicAmbiguousEvent");

    private MqttClient connectToBroker() throws MqttException {
        String broker = "tcp://broker.emqx.io:1883";
        logger.info("Connecting to broker: " + broker);
        MqttClient client = new MqttClient(broker, MqttClient.generateClientId());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        try {
            client.connect(options);
        } catch (MqttException e) {
            logger.error("MQTT connection failed: " + e.getReasonCode());
        }
        return client;
    }

    /**
     * Checks if the ambiguity got resolved by the ML model
     * If yes: Constructs the xml event and publishes it to the MQTT broker
     * If no: Publishes the ambiguous events to the MQTT broker on a different topic
     *
     * @param mlOutput ML output from the ML model
     * @param ambiguousJsonEvents JSON string containing ambiguous events
     */
    @Override
    public void publishResolvedAmbiguousEvent(String mlOutput, String ambiguousJsonEvents) throws MqttException {

        try {
            // read the ml output --> get necessary data
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootMLOutput = objectMapper.readTree(mlOutput);
            String topClass = rootMLOutput.path("top_class").asText();
            boolean resolvedAmb = rootMLOutput.path("resolved_ambiguity").asBoolean();
            String classConfidences = rootMLOutput.path("class_confidences").toString();
            String framePaths = rootMLOutput.path("frames_paths").toString();

            // get the list of ambiguous events
            JsonNode eventsRoot = objectMapper.readTree(ambiguousJsonEvents);
            ArrayNode ambiguousEvents = (ArrayNode) eventsRoot.get("events");

            // check if ambiguity got resolved by ml model
            if (resolvedAmb) {
                logger.info("Ambiguity resolved. Top class: " + topClass);
                // create new JSON object for the resolved event
                ObjectNode resolvedEvent = objectMapper.createObjectNode();
                resolvedEvent.put("concept:name", topClass);

                // get the data that is same for all the ambiguous events
                JsonNode firstEvent = ambiguousEvents.get(0);
                Iterator<Map.Entry<String, JsonNode>> fields = firstEvent.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String key = entry.getKey();
                    if (key.equals("concept:name")) {
                        continue; // skip this key
                    }
                    String value = entry.getValue().asText();
                    resolvedEvent.put(key, value);
                }
                // convert the new JSON event to XES format
                JsonToXesMapper jsonToXesMapper = new JsonToXesMapper();
                String resolvedEventString = jsonToXesMapper.convertJsonToXes(resolvedEvent.toString());
                logger.info("Publishing event with resolved ambiguity: " + resolvedEventString);
                publishEvent(topicUnambiguousEvents, resolvedEventString);
            } else {
                // handle case when ambiguity is not resolved (the classification confidence is below the threshold)
                logger.error("Confidence for classes: " + classConfidences);
                logger.error("Ambiguity not resolved. Manual intervention needed on following events:");
                for (JsonNode event : ambiguousEvents) {
                    logger.info("Event: " + event.toString());
                }
                logger.info("Events published to topic: ambiguous-events. Please subscribe to the topic to see the events.");
                logger.info("Frame paths relevant for manual ambiguity resolution are: " + framePaths);
                publishEvent(topicAmbiguousEvents, ambiguousJsonEvents);
            }
        } catch (Exception e) {
            logger.error("Error processing ML output: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Constructs the xml event from the JSON string and publishes it to the MQTT broker
     *
     * @param unambEvent the unambiguous event in JSON format
     */
    @Override
    public void publishUnambiguousEvent(String unambEvent) throws MqttException {

        try {
            // get the unambiguous event
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(unambEvent);
            JsonNode eventsNode = root.get("events");
            String eventsAsString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventsNode);

            // convert the JSON event to XES format
            JsonToXesMapper jsonToXesMapper = new JsonToXesMapper();
            String xesEvent = jsonToXesMapper.convertJsonToXes(eventsAsString);
            logger.info("Publishing unambiguous event: " + xesEvent);
            publishEvent(topicUnambiguousEvents, xesEvent);
        } catch (JsonProcessingException err) {
            logger.error("Error processing JSON event: " + err.getMessage());
            err.printStackTrace();
        } catch (ParserConfigurationException e) {
            logger.error("Error creating XML document: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            logger.error("Error transforming XML document: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Connect to broker and publish event to the MQTT broker
     *
     * @param topic the topic to publish to
     * @param message the message to publish
     */
    public void publishEvent (String topic, String message) throws MqttException {
        // connect to the broker
        MqttClient client = connectToBroker();
        try {
            // check connection to broker and publish message
            if (client.isConnected()) {
                logger.info("Client is connected to the broker.");
                MqttMessage mqttMessage = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
                mqttMessage.setQos(1);
                client.publish(topic, mqttMessage);
                logger.info("Event published successfully to topic: " + topic);
            } else {
                logger.error("Client is not connected to the broker.");
            }
        } catch (MqttException e) {
            logger.error("Error while publishing message: " + e.getMessage());
            e.printStackTrace();
        // disconnect from broker after sending event
        } finally {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                    logger.info("Client disconnected from broker.");
                }
            } catch (MqttException e) {
                logger.error("Error while disconnecting:" + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}