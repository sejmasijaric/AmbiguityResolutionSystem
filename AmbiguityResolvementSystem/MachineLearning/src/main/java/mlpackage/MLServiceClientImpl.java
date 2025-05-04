package mlpackage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;


@Service
public class MLServiceClientImpl implements MLServiceClient {
    private static final String BASE_URL = "http://localhost:8081";
    private static final double CONFIDENCE_THRESHOLD = 0.95;
    private boolean resolvedAmbiguity;
    private Process pythonServerProcess;

    @PostConstruct
    public void startMlServer () throws IOException {
        // Start the Python server
        ProcessBuilder processBuilder = new ProcessBuilder(
                "/Users/sejmasijaric/Documents/Bachelor Thesis/venv/bin/python3", "-m", "uvicorn", "ml_api:service", "--host", "0.0.0.0", "--port", "8001"
        );
        processBuilder.redirectErrorStream(true);
        pythonServerProcess = processBuilder.start();

        try{
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void stopMlServer () {
        if (pythonServerProcess != null && pythonServerProcess.isAlive()) {
            pythonServerProcess.destroy();
        }
    }

    @Override
    public String analyzeFrames (List<String> frame_paths) throws URISyntaxException, IOException {
        String request = "/analyze-frames";
        URI url = new URI(BASE_URL + request);
        HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true); // enables writing to request body

        // convert input list of frame paths to JSON and write to request body
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> payload = new HashMap<>();
        payload.put("frame_paths", frame_paths);
        String jsonInputString = objectMapper.writeValueAsString(payload);
        connection.getOutputStream().write(jsonInputString.getBytes());
        // get response code
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to connect to the server. Response code: " + responseCode);
        }

        // read response
        BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder responseString = new StringBuilder();
        String line;
        while ((line = response.readLine()) != null) {
            responseString.append(line);
        }
        response.close();

        // Parse the JSON response
        // Reference: https://www.baeldung.com/jackson-object-mapper-tutorial
        JsonNode fullJson = objectMapper.readTree(responseString.toString());
        JsonNode resultNode = fullJson.path("result");
        String result = resultNode.path("result").asText();
        double confidence = resultNode.path("confidence").asDouble();
        // Check if the confidence is above the threshold
        resolvedAmbiguity = confidence >= CONFIDENCE_THRESHOLD;

        ObjectNode resultObject = (ObjectNode) resultNode;
        resultObject.put("resolved_ambiguity", resolvedAmbiguity);


        return objectMapper.writeValueAsString(resultObject);
    }

    public static void main(String[] args) throws URISyntaxException, IOException {
        MLServiceClientImpl client = new MLServiceClientImpl();
        client.startMlServer();
        System.out.println("ML server started.");
        List<String> testFramePaths = Arrays.asList(
                "/Users/sejmasijaric/Documents/Bachelor Thesis/AmbiguityResolvementSystem/MachineLearning/ml_api/test-set/desinfection_left_cluttered3_frame_0027.jpg",
                "/Users/sejmasijaric/Documents/Bachelor Thesis/AmbiguityResolvementSystem/MachineLearning/ml_api/test-set/desinfection_left_cluttered8_frame_0012.jpg",
                "/Users/sejmasijaric/Documents/Bachelor Thesis/AmbiguityResolvementSystem/MachineLearning/ml_api/test-set/desinfection_left_cluttered6_frame_0025.jpg"
        );
        System.out.println(client.analyzeFrames(testFramePaths));
        System.out.println("ML analysis completed.");
        client.stopMlServer();
        System.out.println("ML server stopped.");
    }

}