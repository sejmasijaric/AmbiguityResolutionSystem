
import mlpackage.MLServiceClient;
import mlpackage.MLServiceClientImpl;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MLPipelineIntegrationTest {

    private final MLServiceClient mlServiceClient = new MLServiceClientImpl();

    /**
     * This test method verifies the functionality of the ML pipeline by sending a list of image paths to the ML service
     * The test requires the ml server to be running and accessible
     */
    @Test
    public void testMachineLearningPipeline_withRealFrames_returnsValidResult() throws Exception {
        String currentWorkingDirectory = System.getProperty("user.dir");

// Construct the full path dynamically
        List<String> framePaths = List.of(
                "ml_api/test-set/desinfection_left_cluttered3_frame_0000.jpg",
                "ml_api/test-set/desinfection_left_cluttered3_frame_0009.jpg",
                "ml_api/test-set/desinfection_left_cluttered3_frame_0021.jpg",
                "ml_api/test-set/desinfection_left_cluttered3_frame_0027.jpg",
                "ml_api/test-set/desinfection_left_cluttered3_frame_0029.jpg",
                "ml_api/test-set/desinfection_left_cluttered6_frame_0025.jpg"
        );

        // Resolve the absolute paths
        List<String> absoluteFramePaths = framePaths.stream()
                .map(frame -> Paths.get(currentWorkingDirectory, frame).toString())
                .toList();

        String result = mlServiceClient.analyzeFrames(absoluteFramePaths);

        assertNotNull(result);
        assertTrue(result.contains("resolved_ambiguity"), "Response should contain 'resolved_ambiguity'");
        assertTrue(result.contains("confidence"), "Response should contain 'confidence'");
        assertTrue(result.contains("all_class_probabilities"), "Response should contain 'all_class_probabilities'");
    }
}
