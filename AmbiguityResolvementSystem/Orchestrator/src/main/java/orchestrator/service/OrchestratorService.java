package orchestrator.service;

import camerapackage.CameraServiceClient;
import mlpackage.MLServiceClient;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.impl.XEventImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import publisherpackage.EventStreamListener;
import ambiguitypackage.AmbiguityDetection;
import camerapackage.CameraServiceClientImpl;
import mlpackage.MLServiceClientImpl;
import publisherpackage.PublishingServiceClient;


import java.util.ArrayList;
import java.util.List;

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
    CameraServiceClient cameraClient;
    MLServiceClient mlClient;
    PublishingServiceClient publishingClient;
    private final List<XEvent> receivedEvents = new ArrayList<>();

    public void resolveAmbiguityAndPublishEvent(String json_events) {
        try {
            List<String> image_paths = cameraClient.captureFrames();
            String mlOutput = mlClient.analyzeFrames(image_paths);
            // check if the camera output is empty
            if (mlOutput != null) {
                System.out.println("ML output: " + mlOutput);
                publishingClient.publishResolvedAmbiguousEvent(mlOutput, json_events);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void publishUnambiguousEvent(String jsonEvent) {
        try {
            publishingClient.publishUnambiguousEvent(jsonEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

