package mlpackage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;


public class MLClient {
    private static final String BASE_URL = "http://localhost:8001";
    private static final double CONFIDENCE_THRESHOLD = 0.95;
    private boolean resolvedAmbiguity;

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

        // Parse the JSON response
        // Reference: https://www.baeldung.com/jackson-object-mapper-tutorial
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseString.toString());
        JsonNode resultNode = jsonNode.path("result");

        String topClass = resultNode.path("top_class").asText();
        double confidence = resultNode.path("confidence").asDouble();

        resolvedAmbiguity = confidence >= CONFIDENCE_THRESHOLD;

        return "Top class: " + topClass + ", Confidence: " + confidence + ", Resolved Ambiguity: " + resolvedAmbiguity;
    }
    public boolean isResolvedAmbiguity() {
        return resolvedAmbiguity;
    }

    public static void main(String[] args) throws URISyntaxException, IOException {
        MLClient client = new MLClient();
        System.out.println(client.analyzeFrames());;
    }

}