package orchestrator.controller;

import orchestrator.service.OrchestratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Reference: GitHub repo of my group project --> Software Engineering Course (BuyTicketController.java)
@RestController
@RequestMapping("/orchestrate")
public class EventController {

    private final OrchestratorService orchestrator;

    @Autowired
    public EventController (OrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }
    // Post mapping for when the orchestrator is triggered --> meaning a new event has happened
    @PostMapping("/new-event")
    public ResponseEntity<String> receiveNewEvent(@RequestBody String xesEvent) {
        try {
            orchestrator.processEvent(xesEvent);
            return ResponseEntity.ok("Event received and processed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event: " + e.getMessage());
        }
    }
}
