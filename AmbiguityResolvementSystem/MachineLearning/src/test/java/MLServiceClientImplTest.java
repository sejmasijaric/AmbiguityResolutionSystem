package mlpackage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MLServiceClientImplTest {

    private MLServiceClientImpl client;

    @BeforeEach
    void setUp() {
        ConfigLoader configLoader = new ConfigLoader() {
            @Override
            public String get(String key) {
                return switch (key) {
                    case "ml.confidenceThreshold" -> "0.8";
                    default -> "http://dummy-url.com/";
                };
            }
        };
        client = new MLServiceClientImpl(configLoader, new ObjectMapper());
    }

    @Test
    void testBuildRequestPayload() throws IOException {
        List<String> framePaths = List.of("frame1.jpg", "frame2.jpg");
        String json = client.buildRequestPayload(framePaths);
        assertTrue(json.contains("frame1.jpg"));
        assertTrue(json.contains("frame2.jpg"));
    }

    @Test
    void testParseResponseAndCheckConfidence_aboveThreshold() throws IOException {
        String mockResponse = """
        {
          "result": {
            "top_class": "injection",
            "confidence": 0.85
          }
        }
        """;

        ObjectNode result = client.parseResponseAndCheckConfidence(mockResponse);
        assertEquals("injection", result.get("top_class").asText());
        assertTrue(result.get("resolved_ambiguity").asBoolean());
    }

    @Test
    void testParseResponseAndCheckConfidence_belowThreshold() throws IOException {
        String mockResponse = """
        {
          "result": {
            "gesture": "wrapping",
            "confidence": 0.65
          }
        }
        """;

        ObjectNode result = client.parseResponseAndCheckConfidence(mockResponse);
        assertEquals("wrapping", result.get("gesture").asText());
        assertFalse(result.get("resolved_ambiguity").asBoolean());
    }
}
