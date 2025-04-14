package publisherpackage;

import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XEvent;
import org.eclipse.paho.client.mqttv3.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import ambiguitypackage.AmbiguityDetection;

/*
* This module needs to:
* 1. Connect to the broker
* 2. Listen to stream of events
* 3.
* */

// Using Eclipse Paho MQTT and OpenXES library
// Class for listening to MQTT events and processing them

// Reference: https://www.emqx.com/en/blog/how-to-use-mqtt-in-java
public class EventStreamListener {

    private List<XEvent> receivedEvents = new ArrayList<>();
    private AmbiguityDetection ambiguityDetection = new AmbiguityDetection();
    String broker = "tcp://broker.emqx.io:1883";
    String clientId = "demo_client";
    String topic = "topic/test";
    MqttClient client;

    public void connectToBroker() {
        try {
            this.client = new MqttClient(broker, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            client.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public void consumeEvents() {

        try {
            if (client.isConnected()) {
                System.out.println("Connected to MQTT broker!");
                client.setCallback(new MqttCallback() {
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        String payload = new String(message.getPayload());
                        System.out.println("Message received: " + payload);

                        // since every message indicates a new event, we need to process it and check for ambiguity!
                        XEvent event = parseXesEvent(payload);
                        if (event != null) {
                            processEvent(event);
                        }
                    }
                    public void connectionLost(Throwable cause) {
                        System.out.println("connectionLost: " + cause.getMessage());
                    }

                    public void deliveryComplete(IMqttDeliveryToken token) {
                        System.out.println("deliveryComplete: " + token.isComplete());
                    }
                });

                client.subscribe(topic, 1);
            }

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public void publishEvents (XEvent event) {
        String message = prepareMessage(event);
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
                mqttMessage.setQos(1);
                client.publish(topic, mqttMessage);
            } else {
                System.out.println("Client is not connected to the broker.");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public String prepareMessage(XEvent event) {
        String conceptName = event.getAttributes().get("concept:name").toString();
        String timestamp = event.getAttributes().get("time:timestamp").toString();
        String location = event.getAttributes().get("location:station").toString();
        String performer;
        String message = "";
        if (event.getAttributes().containsKey("perform:hcw")) {
            performer = event.getAttributes().get("perform:hcw").toString();
            message =
                    "<event>\n" +
                    "<string key=\"conceptName\" value=\""+conceptName+"\"/>\n" +
                    "<date key=\"time:timestamp\" value=\""+timestamp+"\"/>\n" +
                    "<string key=\"perform:hcw\" value=\""+performer+"\"/>\n" +
                    "<string key=\"location:station\" value=\""+location+"\"/>\n";
        } else { // Assuming the events must have either "perform:hcw" or "perform:donor"
            performer = event.getAttributes().get("perform:donor").toString();
            message =
                    "<event>\n" +
                    "<string key=\"conceptName\" value=\""+conceptName+"\"/>\n" +
                    "<date key=\"time:timestamp\" value=\""+timestamp+"\"/>\n" +
                    "<string key=\"perform:donor\" value=\""+performer+"\"/>\n" +
                    "<string key=\"location:station\" value=\""+location+"\"/>\n";
        }
        if (event.getAttributes().containsKey("target:donor")) {
            String target = event.getAttributes().get("target:donor").toString();
            message = message +
                    "<string key=\"target:donor\" value=\""+target+"\"/>\n";
        }
        message = message + "</event>";
        return message;
    }
    private XEvent parseXesEvent(String xml) {
        try {
            String wrappedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<log>\n<trace>\n" + xml + "\n</trace>\n</log>";
            ByteArrayInputStream input = new ByteArrayInputStream(wrappedXml.getBytes(StandardCharsets.UTF_8));
            XesXmlParser parser = new XesXmlParser();
            List<XEvent> parsedEvents = parser.parse(input).get(0).get(0);
            return  parsedEvents.get(0);
        } catch (Exception e) {
            System.err.println("Error parsing XES event: " + e.getMessage());
            return null;
        }
    }
    private void processEvent(XEvent event) {
        if (receivedEvents.size() >= 6) {
            // Remove the oldest event if the list exceeds 100 events
            receivedEvents.remove(0);
        }
        receivedEvents.add(event);
        // TODO: Separate concerns -> checking for ambiguity should be done in orchestrator!!!
        if (ambiguityDetection.isAmbiguous(receivedEvents)) {
            System.out.println("Ambiguous events detected, triggering orchestrator...");
            ambiguityDetection.triggerOrchestrator();
        } else {
            System.out.println("No ambiguity detected, triggering publisher...");
            ambiguityDetection.triggerPublisher();
        }
    }
}