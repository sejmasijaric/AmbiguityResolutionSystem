package publisherpackage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

@Service
public class PublishingServiceClientImpl implements PublishingServiceClient {
    private final String broker = "tcp://broker.emqx.io:1883";

    @Override
    public void publishResolvedAmbiguousEvent(String mlOutput, String ambiguousJsonEvents) throws MqttException {
        try {
            // read the ml output --> check if ambiguity got resolved
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootMLOutput = objectMapper.readTree(mlOutput);
            JsonNode results = rootMLOutput.path("results");
            String topClass = results.path("top_class").asText();
            boolean resolvedAmb = results.path("resolved_ambiguity").asBoolean();

            // get the list of ambiguous events
            JsonNode eventsRoot = objectMapper.readTree(ambiguousJsonEvents);
            ArrayNode ambiguousEvents = (ArrayNode) eventsRoot.get("events");

            if (resolvedAmb && !ambiguousEvents.isEmpty()) {
                ObjectNode resolvedEvent = objectMapper.createObjectNode();
                resolvedEvent.put("concept:name", topClass);
                // get the data that's same for all the ambiguous events
                JsonNode firstEvent = ambiguousEvents.get(0);
                for(int i = 1 ; i < firstEvent.size(); i++) {
                    String key = firstEvent.get(i).asText();
                    String value = firstEvent.get(key).asText();
                    resolvedEvent.put(key, value);
                }
                // Resource: https://docs.oracle.com/cd/B14099_12/web.1012/b12024/javax/xml/parsers/DocumentBuilderFactory.html#:~:text=Class%20DocumentBuilderFactory&text=Defines%20a%20factory%20API%20that,object%20trees%20from%20XML%20documents.&text=Allows%20the%20user%20to%20retrieve%20specific%20attributes%20on%20the%20underlying%20implementation.
                // converting json to xml
                // create a factory for XML document builders
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                // Use the factory to create a document builder
                DocumentBuilder builder = factory.newDocumentBuilder();
                // create a new, empty XML document in memory
                Document doc = builder.newDocument();
                String resolvedEventString = jsonToXesMapper(resolvedEvent, doc);
                publishMessage( "clean-events", resolvedEventString);
            } else {
                System.out.println("Ambiguity not resolved. Manual intervention needed on following events:");
                System.out.println("Confidence for classes: " + results.path("class_confidences").toString());
                for (JsonNode event : ambiguousEvents) {
                    System.out.println(event.toString());
                }
                System.out.println("Events published to topic: ambiguous-events\nPlease subscribe to the topic to see the events.");
                System.out.println("Frame paths relevant for ambiguity resolution are: " + results.path("frames_paths").toString());
                publishMessage("ambiguous-events", ambiguousJsonEvents);
            }
        } catch (Exception e) {
            System.err.println("Error processing ML output: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private MqttClient connectToBroker() throws MqttException {
        System.out.println("Connecting to broker: " + broker);
        MqttClient client = new MqttClient(broker, MqttClient.generateClientId());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        try {
            client.connect(options);
        } catch (MqttException e) {
            System.err.println("MQTT connection failed: " + e.getReasonCode());
            e.printStackTrace();
            throw e;
        }        System.out.println("Connected to broker: " + broker);
        return client;
    }

    private String jsonToXesMapper(JsonNode jsonEvent, Document doc) {
        Element eventElement = doc.createElement("event");

        Iterator<Map.Entry<String, JsonNode>> fields = jsonEvent.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            if (entry.getValue().isTextual()) {
                Element stringElement = doc.createElement("string");
                stringElement.setAttribute("key", key);
                stringElement.setAttribute("value", entry.getValue().asText());
                eventElement.appendChild(stringElement);
            } else {
                Element dateElement = doc.createElement("date");
                dateElement.setAttribute("key", key);
                dateElement.setAttribute("value", entry.getValue().asText());
                eventElement.appendChild(dateElement);
            }
        }
        // prepare the event format here
        return eventElement.toString();
    }

    @Override
    public void publishUnambiguousEvent(String unambEvent) throws MqttException {
        System.out.println("Publishing unambiguous event...");
        String topic = "clean-events";
        // create a factory for XML document builders
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Use the factory to create a document builder
        DocumentBuilder builder = factory.newDocumentBuilder();
        // create a new, empty XML document in memory
        Document doc = builder.newDocument();
        String json_event = jsonToXesMapper(unambEvent, doc)
        publishMessage(topic, json_event);
        System.out.println("Unambiguous event published to topic: " + topic);
    }

    public void publishMessage (String topic, String message) throws MqttException {
        MqttClient client = connectToBroker();
        // parse correctly to prepare the message in the initial format
        try {
            if (client.isConnected()) {
                System.out.println("Client is connected to the broker.");
                MqttMessage mqttMessage = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
                mqttMessage.setQos(1);
                client.publish(topic, mqttMessage);
                System.out.println("Message published: " + message);
            } else {
                System.out.println("Client is not connected to the broker.");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        } finally {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                    System.out.println("Client disconnected from broker.");
                }
            } catch (MqttException e) {
                System.err.println("Error while disconnecting:");
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        PublishingServiceClientImpl publishing = new PublishingServiceClientImpl();
        try {
            String message = "<event>\n" +
            "<string key=\"concept:name\" value=\"Next event\"/>\n" +
            "<date key=\"time:timestamp\" value=\"2024-09-11T15:56:16.000+00:00\"/>\n" +
            "<string key=\"perform:donor\" value=\"D001\"/>\n" +
            "<string key=\"location:station\" value=\"Left station\"/>\n" +
            "</event>";
            //message = publishing.prepareMessage();
            String jsonMessage = "{"
                    + "\"concept:name\": \"EJUB\", "
                    + "\"time:timestamp\": \"2024-09-11T15:56:16.000+00:00\", "
                    + "\"perform:donor\": \"D001\", "
                    + "\"location:station\": \"Left station\""
                    + "}";
            publishing.publishMessage(message, "test/topic");
            //publishing.publishMessage(message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

}