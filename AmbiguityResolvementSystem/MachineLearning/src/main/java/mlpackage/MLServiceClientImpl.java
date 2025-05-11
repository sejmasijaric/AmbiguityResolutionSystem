package mlpackage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MLServiceClientImpl implements MLServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(MLServiceClientImpl.class);
    // load mlConfiguration
    ConfigLoader mlConfig = new ConfigLoader();

    /**
     * This method sends a POST request with frame paths to the ML service to analyze the frames
     * It returns the result of the analysis and whether the ambiguity was resolved (whether the classification confidence is above the threshold)
     *
     * @param frame_paths List of frame paths captured by the camera to be analyzed
     * @return JSON string containing the result of the analysis, the frame paths and whether the ambiguity was resolved
     */
    @Override
    public String analyzeFrames (List<String> frame_paths) throws URISyntaxException, IOException {
        logger.info("Sending request to ML service to analyze frames...");
        String BASE_URL = mlConfig.get("ml.baseUrl");
        String request = mlConfig.get("ml.requestEndpoint");
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
            logger.error("Failed to connect to the server. Response code: " + responseCode);
            throw new IOException("Failed to connect to the server. Response code: " + responseCode);
        }
        logger.info("ML model successfully processed frames!");
        // read response
        BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder responseString = new StringBuilder();
        String line;
        while ((line = response.readLine()) != null) {
            responseString.append(line);
        }
        response.close();

        // parse the JSON response
        // Reference: https://www.baeldung.com/jackson-object-mapper-tutorial
        // used to convert JSON to Java objects
        JsonNode fullJson = objectMapper.readTree(responseString.toString());
        JsonNode resultNode = fullJson.path("result");
        double confidence = resultNode.path("confidence").asDouble();
        double CONFIDENCE_THRESHOLD = Double.parseDouble(mlConfig.get("ml.confidenceThreshold"));
        // Check if the confidence is above the threshold
        boolean resolvedAmbiguity = confidence >= CONFIDENCE_THRESHOLD;
        logger.info("ML model confidence: " + confidence + ". Resolved ambiguity: " + resolvedAmbiguity);
        // Construct final JSON output including boolean indicating whether the ambiguity was resolved
        ObjectNode resultObject = (ObjectNode) resultNode;
        resultObject.put("resolved_ambiguity", resolvedAmbiguity);
        String mlOutput = objectMapper.writeValueAsString(resultObject);
        logger.info("ML output: " + mlOutput);
        return mlOutput;
    }
}