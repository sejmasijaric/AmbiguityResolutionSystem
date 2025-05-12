import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import publisherpackage.ConfigLoader;
import publisherpackage.JsonToXesMapper;
import publisherpackage.MqttService;
import publisherpackage.PublishingServiceClientImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublishingServiceClientImplTest {

    @Mock
    private MqttService mqttService;

    @Mock
    private JsonToXesMapper jsonToXesMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ConfigLoader configLoader;

    @InjectMocks
    private PublishingServiceClientImpl publishingService;

    @Test
    void publishUnambiguousEvent_Success() throws Exception {
        String topic = "clean-events-test";
        String inputJson =
                "{"
                        +   "\"events\": ["
                        +     "{"
                        +       "\"concept:name\": \"Apply tourniquet\","
                        +       "\"time:timestamp\": \"2024-09-11T15:56:16.000+00:00\","
                        +       "\"perform:donor\": \"D001\","
                        +       "\"location:station\": \"Left station\""
                        +     "}"
                        +   "]"
                        + "}";
        // parse with real ObjectMapper so mockRoot.get("events") is non-null
        ObjectMapper realMapper = new ObjectMapper();
        JsonNode mockRoot = realMapper.readTree(inputJson);
        JsonNode eventsNode = mockRoot.get("events");

        when(configLoader.get("publisher.topicUnambiguousEvent")).thenReturn(topic);
        when(objectMapper.readTree(inputJson)).thenReturn(mockRoot);
        when(jsonToXesMapper.convertJsonToXes(eventsNode.toString()))
                .thenReturn("<event>…</event>");

        PublishingServiceClientImpl publishingService = new PublishingServiceClientImpl(
                mqttService, jsonToXesMapper, objectMapper, configLoader);
        publishingService.publishUnambiguousEvent(inputJson);
        verify(mqttService).publish(eq(topic), contains("<event>"));
    }
    @Test
    void publishResolvedAmbiguousEvent_UnresolvedAmbiguity() throws Exception {
        String topicAmbiguous = "ambiguous-events-test";
        String topicUnambiguous = "clean-events-test";
        String ambiguousJsonEvents = "{ \"events\": [{ \"concept:name\": \"Event1\" }] }";
        String mlOutput = "{"
                + "\"top_class\": \"injection\","
                + "\"all_class_probabilities\": {\"injection\": 0.78, \"wrapping\": 0.12, \"disinfection\": 0.10},"
                + "\"frame_paths\": [\"/frames/frame1.jpg\", \"/frames/frame2.jpg\", \"/frames/frame3.jpg\"],"
                + "\"resolved_ambiguity\": false"
                + "}";

        JsonNode mockRootEvents = new ObjectMapper().readTree(ambiguousJsonEvents);
        JsonNode mockRootMlOutput = new ObjectMapper().readTree(mlOutput);

        // Stub all required configuration keys
        when(configLoader.get("publisher.topicAmbiguousEvent")).thenReturn(topicAmbiguous);
        when(configLoader.get("publisher.topicUnambiguousEvent")).thenReturn(topicUnambiguous);

        // Stub JSON inputs and ObjectNode creation
        when(objectMapper.readTree(ambiguousJsonEvents)).thenReturn(mockRootEvents);
        when(objectMapper.readTree(mlOutput)).thenReturn(mockRootMlOutput);

        PublishingServiceClientImpl publishingService = new PublishingServiceClientImpl(
                mqttService, jsonToXesMapper, objectMapper, configLoader);

        publishingService.publishResolvedAmbiguousEvent(mlOutput, ambiguousJsonEvents);

        verify(mqttService).publish(eq(topicAmbiguous), eq(mockRootEvents.get("events").toString()));
    }
    @Test
    void publishResolvedAmbiguousEvent_ResolvedAmbiguity() throws Exception {
        String topicUnambiguous = "clean-events"; // Define the expected topic
        String ambiguousJsonEvents = "{ \"events\": [{ \"concept:name\": \"Event1\" }] }";
        String mlOutput = "{ \"top_class\": \"ResolvedEvent\", \"all_class_probabilities\": \"{}\", \"frame_paths\": \"[]\", \"resolved_ambiguity\": true }";

        JsonNode mockRootEvents = new ObjectMapper().readTree(ambiguousJsonEvents);
        JsonNode mockRootMlOutput = new ObjectMapper().readTree(mlOutput);
        ObjectNode mockResolvedEvent = new ObjectMapper().createObjectNode(); // Create a mock ObjectNode

        // Stub ConfigLoader to return the correct topic
        when(configLoader.get("publisher.topicUnambiguousEvent")).thenReturn(topicUnambiguous);
        // Stub JSON inputs and ObjectNode creation
        when(objectMapper.readTree(ambiguousJsonEvents)).thenReturn(mockRootEvents);
        when(objectMapper.readTree(mlOutput)).thenReturn(mockRootMlOutput);
        when(objectMapper.createObjectNode()).thenReturn(mockResolvedEvent); // Mock ObjectNode creation
        when(jsonToXesMapper.convertJsonToXes(anyString())).thenReturn("<event>ResolvedEvent</event>");

        PublishingServiceClientImpl publishingService = new PublishingServiceClientImpl(
                mqttService, jsonToXesMapper, objectMapper, configLoader);
        publishingService.publishResolvedAmbiguousEvent(mlOutput, ambiguousJsonEvents);

        // Verify the correct topic and payload are used
        verify(mqttService).publish(eq(topicUnambiguous), contains("<event>ResolvedEvent</event>"));
    }
    @Test
    void constructor_MissingConfigKey_ThrowsException() {
        when(configLoader.get("publisher.topicUnambiguousEvent")).thenThrow(new IllegalArgumentException("Missing config value for key: publisher.topicUnambiguousEvent"));

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                new PublishingServiceClientImpl(mqttService, jsonToXesMapper, objectMapper, configLoader)
        );

        assertEquals("Missing config value for key: publisher.topicUnambiguousEvent", exception.getMessage());
    }
    @Test
    void publishUnambiguousEvent_InvalidJson_ThrowsException() throws Exception {
        String invalidJson = "{ invalid }";

        when(objectMapper.readTree(invalidJson)).thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "Invalid JSON"));

        Exception exception = assertThrows(RuntimeException.class, () ->
                publishingService.publishUnambiguousEvent(invalidJson)
        );

        assertTrue(exception.getMessage().contains("Failed to publish unambiguous event"));
    }
    @Test
    void publishUnambiguousEvent_MqttPublishFailure_ThrowsException() throws Exception {
        String inputJson = "{ \"events\": [{ \"concept:name\": \"Event1\" }] }";
        JsonNode mockRoot = new ObjectMapper().readTree(inputJson);

        when(objectMapper.readTree(inputJson)).thenReturn(mockRoot);
        when(jsonToXesMapper.convertJsonToXes(anyString())).thenReturn("<event>Event1</event>");
        doThrow(new MqttException(0)).when(mqttService).publish(anyString(), anyString());

        Exception exception = assertThrows(RuntimeException.class, () ->
                publishingService.publishUnambiguousEvent(inputJson)
        );

        assertTrue(exception.getMessage().contains("Failed to publish unambiguous event"));
    }
}