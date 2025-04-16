package orchestrator.service;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.impl.XEventImpl;
import org.springframework.stereotype.Service;
import publisherpackage.EventStreamListener;
import ambiguitypackage.AmbiguityDetection;
import camerapackage.CameraClient;
import mlpackage.MLClient;


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

    EventStreamListener streamListener = new EventStreamListener();
    AmbiguityDetection ambiguityDetection = new AmbiguityDetection();
    CameraClient cameraClient = new CameraClient();
    MLClient mlClient = new MLClient();
    private final List<XEvent> receivedEvents = new ArrayList<>();

    public void processEvent(String xesEvent) {
        XEvent xevent = streamListener.parseXesEvent(xesEvent);
        if (receivedEvents.size() >= 6){
            receivedEvents.remove(0); // remove the oldest event
        }
        receivedEvents.add(xevent); // add the new event
        try {
            if (ambiguityDetection.isAmbiguous(receivedEvents)) {
                System.out.println("Ambiguous events detected, triggering ambiguity resolution...");
                cameraClient.startCamera();
                cameraClient.captureFrame();
                String mlOutput;
                while (true) {
                    for (int i = 0; i <= 4; i++) {
                        cameraClient.captureFrame();
                        cameraClient.wait(500);
                    }
                    mlOutput = mlClient.analyzeFrames();
                    // Check if the ML model resolved the ambiguity
                    if (mlOutput != null && mlClient.isResolvedAmbiguity()) {
                        System.out.println("ML output: " + mlOutput);
                        break;
                    } else {
                        System.out.println("Ambiguity not resolved, retrying...");
                    }
                }
                // Split output to get the top class --> top class will be splitOutput[1]
                String[] splitOutput = mlOutput.split(",")[0].split(":");

                // TODO: what to do with the multiple events that are ambiguous --> only one event can be published
                // TODO: create a new event with the top class and publish it
                // delete the ambiguous events from the list and leave only the new event with the resolved ambiguity
                XEvent eventToPublish = new XEventImpl();


            } else {
                System.out.println("No ambiguous event detected, triggering publisher...");
                // TODO: shouldi re-trigger the published and republish non-ambiguous events or just print the message
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

