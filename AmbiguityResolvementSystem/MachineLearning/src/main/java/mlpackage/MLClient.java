package mlpackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

public class MLClient {

    public static final String BASE_URL = "http://localhost:8001";

    public String analyzeFrames () throws URISyntaxException, IOException {
        String request = "/analyze-frames";
        URI url = new URI(BASE_URL + request);
        HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
        connection.setRequestMethod("POST");

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

    public static void main(String[] args) throws URISyntaxException, IOException {
        MLClient client = new MLClient();
        System.out.println(client.analyzeFrames());;
    }

}