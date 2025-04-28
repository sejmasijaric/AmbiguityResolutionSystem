package mlpackage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;


@Service
public class MLServiceClientImpl implements MLServiceClient {
    private static final String BASE_URL = "http://localhost:8001";
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
        MLServiceClientImpl client = new MLServiceClientImpl();
        client.startMlServer();
        System.out.println("ML server started.");
        System.out.println(client.analyzeFrames());
        System.out.println("ML analysis completed.");
        client.stopMlServer();
        System.out.println("ML server stopped.");
    }

}