package publisherpackage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

@Service
public class PublishingServiceClientImpl implements PublishingServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(PublishingServiceClientImpl.class);

    private final MqttService mqttService;
    private final JsonToXesMapper jsonToXesMapper;
    private final ObjectMapper objectMapper;

    private final String topicUnambiguousEvents;
    private final String topicAmbiguousEvents;

    public PublishingServiceClientImpl(MqttService mqttService,
                                       JsonToXesMapper jsonToXesMapper,
                                       ObjectMapper objectMapper,
                                       ConfigLoader config) {
        this.mqttService = mqttService;
        this.jsonToXesMapper = jsonToXesMapper;
        this.objectMapper = objectMapper;
        this.topicUnambiguousEvents = config.get("publisher.topicUnambiguousEvent");
        this.topicAmbiguousEvents = config.get("publisher.topicAmbiguousEvent");

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
    public void publishResolvedAmbiguousEvent(String mlOutput, String ambiguousJsonEvents) {

        try {
            AmbiguityResolutionResult result = parseMlOutput(mlOutput);
            // get the list of ambiguous events
            JsonNode eventsRoot = objectMapper.readTree(ambiguousJsonEvents);
            ArrayNode ambiguousEvents = (ArrayNode) eventsRoot.get("events");

            // check if ambiguity got resolved by ml model
            if (result.isResolved()) {
                handleResolvedAmbiguity(result, ambiguousEvents);
            } else {
                handleUnresolvedAmbiguity(result, ambiguousEvents);
            }
        } catch (Exception e) {
            logger.error("Error processing ML output: " + e.getMessage());
            throw new RuntimeException("Failed to process ML output", e);
        }
    }
    private void handleResolvedAmbiguity(AmbiguityResolutionResult result, ArrayNode ambiguousEvents) throws JsonProcessingException, ParserConfigurationException, TransformerException, MqttException {
        logger.info("Ambiguity resolved. Top class: " + result.getTopClass());
        ObjectNode resolvedEvent = createResolvedEvent(result.getTopClass(), ambiguousEvents);
        String xesEvent = jsonToXesMapper.convertJsonToXes(resolvedEvent.toString());
        mqttService.publish(topicUnambiguousEvents, xesEvent);
    }
    private void handleUnresolvedAmbiguity(AmbiguityResolutionResult result, ArrayNode ambiguousEvents) throws MqttException {
        logger.error("Confidence for classes: " + result.getClassConfidences());
        logger.error("Ambiguity not resolved. Manual intervention needed for events: ");
        ambiguousEvents.forEach(event -> logger.error(event.toString()));
        logger.info("Publishing to ambiguous events topic: " + topicAmbiguousEvents);
        mqttService.publish(topicAmbiguousEvents, ambiguousEvents.toString());
    }
    private ObjectNode createResolvedEvent(String topClass, ArrayNode ambiguousEvents) {
        ObjectNode resolvedEvent = objectMapper.createObjectNode();
        resolvedEvent.put("concept:name", topClass);

        JsonNode firstEvent = ambiguousEvents.get(0);
        firstEvent.fields().forEachRemaining(entry -> {
            if (!entry.getKey().equals("concept:name")) {
                resolvedEvent.set(entry.getKey(), entry.getValue());
            }
        });
        return resolvedEvent;
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
            JsonNode root = objectMapper.readTree(unambEvent);
            JsonNode eventsNode = root.get("events");
            String eventsAsString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventsNode);

            // convert the JSON event to XES format
            JsonToXesMapper jsonToXesMapper = new JsonToXesMapper();
            String xesEvent = jsonToXesMapper.convertJsonToXes(eventsAsString);
            logger.info("Publishing unambiguous event: " + xesEvent);
            mqttService.publish(topicUnambiguousEvents, xesEvent);
        } catch (Exception e) {
            logger.error("Error publishing unambiguous event: " + e.getMessage());
            throw new RuntimeException("Failed to publish unambiguous event", e);
        }
    }

    private AmbiguityResolutionResult parseMlOutput(String mlOutput) throws JsonProcessingException {
        JsonNode rootMlOutput = objectMapper.readTree(mlOutput);
        System.out.println("ML output: " + rootMlOutput);
        return new AmbiguityResolutionResult(
                rootMlOutput.get("top_class").asText(),
                rootMlOutput.get("all_class_probabilities").toString(),
                rootMlOutput.get("frame_paths").toString(),
                rootMlOutput.get("resolved_ambiguity").asBoolean()
        );
    }

    public record AmbiguityResolutionResult(
            @Getter
            String topClass,
            @Getter
            String classConfidences,
            @Getter
            String framePaths,
            boolean resolvedAmb
    ) {
        public boolean isResolved() {
            return resolvedAmb;
        }
    }
}