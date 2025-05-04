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
public class OrchestratorController {

    private final OrchestratorService orchestrator;

    @Autowired
    public OrchestratorController(OrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }
    // Post mapping for when the orchestrator is triggered --> meaning a new event has happened
    @PostMapping("/unambiguous-event")
    public ResponseEntity<String> receiveNewEvent(@RequestBody String jsonEvent) {
        try {
            orchestrator.publishUnambiguousEvent(jsonEvent);
            return ResponseEntity.ok("Event received and processed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event: " + e.getMessage());
        }
    }

    @PostMapping("/ambiguous-event")
    public ResponseEntity<String> receiveAmbiguousEvent(@RequestBody String jsonEvent) {
        try {
            orchestrator.resolveAmbiguityAndPublishEvent(jsonEvent);
            return ResponseEntity.ok("Ambiguous event received and processed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing ambiguous event: " + e.getMessage());
        }
    }
}
