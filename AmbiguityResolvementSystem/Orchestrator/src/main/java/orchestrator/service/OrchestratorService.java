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
    AmbiguityDetection ambiguityDetection = new AmbiguityDetection();
    CameraServiceClient cameraClient;
    MLServiceClient mlClient;
    PublishingServiceClient publishingClient;
    private final List<XEvent> receivedEvents = new ArrayList<>();

    public void processEvent(String xesEvent) {

        // TODO: remove additional responsibilities -> orch does only data flow orchestration
        //XEvent xevent = streamListener.parseXesEvent(xesEvent);

        try {
            if (ambiguityDetection.isAmbiguous(receivedEvents)) {
                System.out.println("Ambiguous events detected, triggering ambiguity resolution...");
                String mlOutput = cameraClient.captureFrame();


                // Check if the ML model resolved the ambiguity
                if (mlOutput != null) {
                    System.out.println("ML output: " + mlOutput);
                    // TODO: add here the new xevent format for example with the resolved ambiguity
                    publishingClient.publishMessage(mlOutput);
                } else {
                    // add an additional attr with label confidences -> if conf is less that threshold someone should take a manual look at it
                    System.out.println("Ambiguity not resolved. Manual intervention needed on event: {}");
                }

                // Split output to get the top class --> top class will be splitOutput[1]
                String[] splitOutput = mlOutput.split(",")[0].split(":");

                // TODO: what to do with the multiple events that are ambiguous --> only one event can be published
                // TODO: create a new event with the top class and publish it
                // delete the ambiguous events from the list and leave only the new event with the resolved ambiguity
                XEvent eventToPublish = new XEventImpl();


            } else {
                System.out.println("No ambiguous event detected, triggering publisher...");
                publishingClient.publishMessage(xesEvent);
                // TODO: shouldi re-trigger the published and republish non-ambiguous events or just print the message
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

