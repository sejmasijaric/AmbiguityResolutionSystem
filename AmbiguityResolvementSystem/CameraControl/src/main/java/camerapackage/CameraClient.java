package camerapackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class CameraClient {

    private static final String BASE_URL = "http://localhost:8000";

    public String startCamera() throws IOException, URISyntaxException {
        String request = "/start-camera";
        return sendGetRequest(request);
    }
    public String stopCamera() throws IOException , URISyntaxException{
        String request = "/stop-camera";
        return sendGetRequest(request);
    }
    public String captureFrame() throws IOException, URISyntaxException {
        String request = "/capture-frame";
        return sendGetRequest(request);
    }

    // Helper method to send the frames
    // Reference: https://docs.oracle.com/javase/tutorial/networking/urls/connecting.html#:~:text=When%20you%20do%20this%20you,openConnection()%3B%20myURLConnection.
    // Since URL constructors are deprecated: https://stackoverflow.com/questions/75966165/how-to-replace-the-deprecated-url-constructors-in-java-20
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
            return "Success:" + responseString;
        } else {
            return "Error (" + responseCode + "): " + responseString.toString();
        }
    }

    public static void main(String[] args) {

        CameraClient client = new CameraClient();

        try {
            // Start camera
            System.out.println(client.startCamera());

            // Capture a frame
            System.out.println(client.captureFrame());

            // Stop camera
            System.out.println(client.stopCamera());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}