package camerapackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CameraServiceClientImpl implements CameraServiceClient {


    private static final Logger logger = LoggerFactory.getLogger(CameraServiceClientImpl.class);
    private final CameraConfigLoader cameraConfig;

    public CameraServiceClientImpl() {
        this.cameraConfig = new CameraConfigLoader();
    }
    public CameraServiceClientImpl(CameraConfigLoader config) {
        this.cameraConfig = config;
    }

    @Override
    public List<String> getFrames() throws Exception {
        startCamera();
        Thread.sleep(300);
        List<String> filepaths = new ArrayList<>();
        String filepath = captureFrame();

        // get configuration values
        int numberOfFrames = Integer.parseInt(cameraConfig.get("cameraControl.numberOfFrames"));
        long waitingTime = Long.parseLong(cameraConfig.get("cameraControl.waitingTime"));
        for (int i = 0; i < numberOfFrames; i++) {
            if (filepath == null) {
                continue;
            }
            filepaths.add(filepath);
            filepath = captureFrame();
            waitBeforeNextFrame(waitingTime);
        }
        stopCamera();
        return filepaths;
    }

    private void waitBeforeNextFrame (long waitingTime) {
        try {
            Thread.sleep(waitingTime); // Wait for 500 milliseconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // re-interrupt the thread
            logger.error("Thread interrupted while waiting for next frame: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startCamera() throws IOException, URISyntaxException {
        logger.info("Starting camera...");
        String requestEndpoint = cameraConfig.get("cameraControl.startCameraEndpoint");
        sendGetRequest(requestEndpoint);
        logger.info("Camera started!");
    }
    private void stopCamera() throws IOException , URISyntaxException{
        logger.info("Stopping camera...");
        String requestEndpoint = cameraConfig.get("cameraControl.stopCameraEndpoint");
        sendGetRequest(requestEndpoint);
        logger.info("Camera stopped!");
    }
    protected String captureFrame() throws IOException, URISyntaxException, ParseException {
        logger.info("Capturing frame...");
        // get frame filepath from response
        String requestEndpoint = cameraConfig.get("cameraControl.captureFrameEndpoint");
        String response = sendGetRequest(requestEndpoint);
        JSONObject jsonObject = parseJsonResponse(response);
        String status = jsonObject.get("status").toString();
        if (status.equals("successful")) {
            return jsonObject.get("filepath").toString();
        } else {
            logger.error("Failed to capture frame.");
            return null;
        }
    }
    protected JSONObject parseJsonResponse(String response) throws ParseException {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(response);
    }

    // Helper method to send the frames
    // Reference: https://docs.oracle.com/javase/tutorial/networking/urls/connecting.html#:~:text=When%20you%20do%20this%20you,openConnection()%3B%20myURLConnection.
    // Since URL constructors are deprecated: https://stackoverflow.com/questions/75966165/how-to-replace-the-deprecated-url-constructors-in-java-20
    // reference used to establish connection
    protected String sendGetRequest(String request) throws IOException, URISyntaxException {
        String baseUrl = cameraConfig.get("cameraControl.baseUrl");
        URI url = new URI(baseUrl + request);

        HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
        connection.setRequestMethod("GET");

        // check the response code
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to connect to the server. Response code: " + responseCode);
        }

        // read the response
        BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder responseString = new StringBuilder();
        while ((line = response.readLine()) != null) {
            responseString.append(line);
        }
        response.close();

        return responseString.toString();
    }

    public static void main(String[] args) throws Exception {
        CameraServiceClientImpl cameraClient = new CameraServiceClientImpl();
        cameraClient.getFrames();
    }
}