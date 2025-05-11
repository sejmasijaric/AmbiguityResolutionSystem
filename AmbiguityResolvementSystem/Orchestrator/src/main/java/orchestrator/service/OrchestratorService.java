package orchestrator.service;

import camerapackage.CameraServiceClient;
import mlpackage.MLServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import publisherpackage.PublishingServiceClient;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * This method is called when ambiguity is detected by the AmbiguityDetection module via HTTP POST request
     * It triggers the ambiguity resolution process
     *
     * @param json_events JSON string containing the ambiguous events
     */
    public void resolveAmbiguityAndPublishEvent(String json_events) {
        try {
            List<String> image_paths = cameraClient.getFrames();
            String mlOutput = mlClient.analyzeFrames(image_paths);
            // check if the camera output is empty
            if (mlOutput != null) {
                publishingClient.publishResolvedAmbiguousEvent(mlOutput, json_events);
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
        try {
            publishingClient.publishUnambiguousEvent(jsonEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

