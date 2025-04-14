package orchestrator.service;

import org.deckfour.xes.model.XEvent;
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
        if (receivedEvents )
    }
}

