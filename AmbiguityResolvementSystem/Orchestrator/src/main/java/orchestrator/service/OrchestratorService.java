package orchestrator.service;

import java.io.FileWriter;
import java.io.IOException;
import static java.lang.System.currentTimeMillis;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import camerapackage.CameraServiceClient;
import mlpackage.MLServiceClient;
import publisherpackage.PublishingServiceClient;

/*
This service does the following:
* 1. Wait for new event to be sent via HTTP POST request (EventController.java and EventStreamPublisher.java)
* 2. Add the new event to the list of events
* 2.1. Maintain the list --> max x events in the list
* 2.2. If the list is full, remove the oldest event (use a queue)
* 3. Check if the most recent events in the list are ambiguous (or all events, depending on what x is equal to)
* 3.1. If ambiguous, trigger ambiguity resolution --> camera, ml model and publisher
* 3.2. If non-ambiguous, trigger publisher
* */


@Service
public class OrchestratorService {

    @Autowired
    public OrchestratorService(CameraServiceClient cameraClient, MLServiceClient mlClient, PublishingServiceClient publishingClient) {
        this.cameraClient = cameraClient;
        this.mlClient = mlClient;
        this.publishingClient = publishingClient;
    }

    private static final Logger logger = LoggerFactory.getLogger(OrchestratorService.class);
    CameraServiceClient cameraClient;
    MLServiceClient mlClient;
    PublishingServiceClient publishingClient;

    private String extractEventId(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);
            if (node.has("event_id")) {
                return node.get("event_id").asText();
            } else if (node.has("events")) {
                JsonNode eventsNode = node.get("events");
                if (eventsNode.isArray() && eventsNode.size() > 0 && eventsNode.get(0).has("event_id")) {
                    return eventsNode.get(0).get("event_id").asText();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to extract event_id: " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * This method is called when ambiguity is detected by the AmbiguityDetection module via HTTP POST request
     * It triggers the ambiguity resolution process
     *
     * @param json_events JSON string containing the ambiguous events
     */
    public void resolveAmbiguityAndPublishEvent(String json_events) {
        String eventId = extractEventId(json_events);
        long start = currentTimeMillis();
        try {
            List<String> image_paths = cameraClient.getFrames();
            long latency = currentTimeMillis() - start;
            logPerformance(eventId, "CameraClientService", latency);
            start = currentTimeMillis();
            String mlOutput = mlClient.analyzeFrames(image_paths);
            latency = currentTimeMillis() - start;
            logPerformance(eventId, "MLServiceClient", latency);

            // check if the ml output is empty
            if (mlOutput != null) {
                start = currentTimeMillis();
                publishingClient.publishResolvedAmbiguousEvent(mlOutput, json_events);
                latency = currentTimeMillis() - start;
                logPerformance(eventId, "PublishingResolvedAmbiguity", latency);
            }
        } catch (Exception e) {
            logger.error("Error while resolving ambiguity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method is called when ambiguity isn't detected by the AmbiguityDetection module
     * It publishes the unambiguous event to the MQTT broker
     *
     * @param jsonEvent JSON string containing the unambiguous event
     */
    public void publishUnambiguousEvent(String jsonEvent) {
        String eventId = extractEventId(jsonEvent);
        try {
            long start = currentTimeMillis();
            publishingClient.publishUnambiguousEvent(jsonEvent);
            long latency = currentTimeMillis() - start;
            logPerformance(eventId, "PublishingUnambiguousEvent", latency);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void logPerformance(String eventId, String component, long latency) {
        try (FileWriter fw = new FileWriter("latency.csv", true)) {
            String timestamp = java.time.LocalDateTime.now().toString();
            fw.write(timestamp + "," + eventId + "," + component + "," + latency + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

