package publisherpackage;

import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XEvent;
import org.eclipse.paho.client.mqttv3.*;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import ambiguitypackage.AmbiguityDetection;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/*
* This module needs to:
* 1. Connect to the broker
* 2. Listen to stream of events
* 3. Convert from format to format --> as required
* 4. Send the event to orchestrator
* 5. Publish event via MQTT when needed
* */

// Using Eclipse Paho MQTT and OpenXES library
// Class for listening to MQTT events and processing them

// Reference: https://www.emqx.com/en/blog/how-to-use-mqtt-in-java
// TODO: describe the reference you used (1-to-1 or adjusted..)
@Component
public class EventStreamListener {
    private static final String BASE_URL = "http://localhost:8080";
    private final String broker = "tcp://localhost";
    private String topic = "topic/test";


    public void connectToBroker(String clientId) {
        try {
            MqttClient client = new MqttClient(broker, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            client.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public void sendEventToOrchestrator(String newStringEvent) throws Exception {
        String request = "/orchestrate/new-event";
        URI url = new URI(BASE_URL + request);
        HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/xml");
        connection.setDoOutput(true); // there will be data in the body of the request

        // Write the event to the request body
        try (OutputStream outputStream = connection.getOutputStream()) {
            byte[] input = newStringEvent.getBytes(StandardCharsets.UTF_8);
            outputStream.write(input, 0, input.length);
        }
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("Event sent to orchestrator successfully.");
        } else {
            System.out.println("Failed to send event to orchestrator. Response code: " + responseCode);
        }

        connection.disconnect();

    }

    public static void main(String[] args) {
        EventStreamListener listener = new EventStreamListener();
        listener.startListener();
    }
    @PostConstruct
    public void startListener() {
        Thread listenerThread = new Thread(() -> {
            try {
                consumeEvents();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        listenerThread.setDaemon(false);
        listenerThread.start();
        System.out.println("Listening to events...");
    }
    public void consumeEvents() {
        try {
            MqttClient client = new MqttClient(broker, "subscriber", new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            client.connect(options);

            if (client.isConnected()) {
                System.out.println("Connected to MQTT broker!");
                client.setCallback(new MqttCallback() {
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        String payload = new String(message.getPayload());
                        System.out.println("Message received: " + payload);
                        System.out.println("This here is printeeedèèèè");

                        // since every message indicates a new event, we need to process it and check for ambiguity!
                        XEvent event = parseXesEvent(payload);
                        // TODO: the whole http part and sending this message to orchestrator via http
                        String stringEvent = prepareMessage(event);
                        sendEventToOrchestrator(stringEvent);
                    }
                    public void connectionLost(Throwable cause) {
                        System.out.println("Connection lost! " + cause.getMessage());
                    }

                    public void deliveryComplete(IMqttDeliveryToken token) {
                        System.out.println("deliveryComplete: " + token.isComplete());
                    }
                });

                client.subscribe(topic, 1);
                synchronized (this) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        System.out.println("Listener thread interrupted: " + e.getMessage());
                    }
                }
            }

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public void publishEvent (XEvent event) throws MqttException {
        MqttClient client = new MqttClient(broker, "publisher", new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        client.connect(options);
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
        // hard coded - make more generic
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

    public XEvent parseXesEvent(String xml) {
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


}