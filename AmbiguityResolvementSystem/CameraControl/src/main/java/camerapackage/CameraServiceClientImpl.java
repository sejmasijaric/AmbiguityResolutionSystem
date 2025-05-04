package camerapackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import netscape.javascript.JSObject;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

@Service
public class CameraServiceClientImpl implements CameraServiceClient {

   // Properties properties = loadProperties();
    private final String BASE_URL = "http://localhost:8003";

/*    private Properties loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("../../resources/application.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find application.properties");
                return null;
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return properties;
    }
*/

    @Override
    public List<String> captureFrames() throws Exception {
        startCamera();
        List<String> filepaths = new ArrayList<>();
        String filepath;
        for (int i = 0; i < 5; i++) {
            filepath = takePhoto();
            if (filepath.equals("Could not capture frame")) {
                System.out.println("Failed to capture frame.");
                continue;
            }
            filepaths.add(filepath);
            try {
                Thread.sleep(500); // Wait for 500 milliseconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // good practice: re-interrupt the thread
                e.printStackTrace();
            }
        }
        stopCamera();
        return filepaths;
    }

    private void startCamera() throws IOException, URISyntaxException {
        System.out.println("Starting camera...");
        sendGetRequest("/start-camera");
        System.out.println("Camera started!");
    }
    private void stopCamera() throws IOException , URISyntaxException{
        System.out.println("Stopping camera...");
        sendGetRequest("/stop-camera");
        System.out.println("Camera stopped!");
    }
    private String takePhoto() throws IOException, URISyntaxException, ParseException {
        String response = sendGetRequest("/capture-frame");
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(response);
        String status = jsonObject.get("status").toString();
        String filepath;
        if (status.equals("frame captured")) {
            System.out.println("Frame captured successfully!");
            filepath = jsonObject.get("filepath").toString();
        } else {
            System.out.println("Failed to capture frame.");
            return "Could not capture frame";
        }
        return filepath;
    }

    // Helper method to send the frames
    // Reference: https://docs.oracle.com/javase/tutorial/networking/urls/connecting.html#:~:text=When%20you%20do%20this%20you,openConnection()%3B%20myURLConnection.
    // Since URL constructors are deprecated: https://stackoverflow.com/questions/75966165/how-to-replace-the-deprecated-url-constructors-in-java-20
    // reference used to establish connection
    private String sendGetRequest(String request) throws IOException, URISyntaxException {
        URI url = new URI(BASE_URL + request);
        HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
        connection.setRequestMethod("GET");

        // Check the response code
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to connect to the server. Response code: " + responseCode);
        }

        // Read the response
        BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder responseString = new StringBuilder();
        while ((line = response.readLine()) != null) {
            responseString.append(line);
        }
        response.close();

        if (responseCode == 200) {
            return responseString.toString();
        } else {
            return responseString.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        CameraServiceClientImpl cameraClient = new CameraServiceClientImpl();
        List<String> paths = cameraClient.captureFrames();
        for (String path : paths) {
            System.out.println("Captured frame: " + path);
        }
    }
}