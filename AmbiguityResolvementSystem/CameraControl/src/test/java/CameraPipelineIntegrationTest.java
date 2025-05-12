package camerapackage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CameraPipelineIntegrationTest {

    private CameraServiceClient client = new CameraServiceClientImpl();

    @BeforeEach
    public void setUp() {
        CameraConfigLoader config = new CameraConfigLoader() {
            @Override
            public String get(String key) {
                switch (key) {
                    case "cameraControl.baseUrl":
                        return "http://localhost:8000";
                    case "cameraControl.startCameraEndpoint":
                        return "/start-camera";
                    case "cameraControl.stopCameraEndpoint":
                        return "/stop-camera";
                    case "cameraControl.captureFrameEndpoint":
                        return "/capture-frame";
                    case "cameraControl.numberOfFrames":
                        return "5";
                    case "cameraControl.waitingTime":
                        return "500"; // 100 ms delay
                    default:
                        return "";
                }
            }
        };
        client = new CameraServiceClientImpl(config);
    }

    @Test
    public void testFullCameraPipeline() throws Exception {
        List<String> frames = client.getFrames();
        System.out.println(frames);

        assertEquals(5, frames.size(), "Should capture 5 frames");

        System.out.println("Current working directory: " + new File(".").getAbsolutePath());
        for (String path : frames) {
            File file = new File(path);
            System.out.println("Captured file: " + file.getAbsolutePath());
            assertTrue(file.getAbsoluteFile().exists(), "Captured file should exist: " + path);
            assertTrue(file.length() > 0, "Captured file should not be empty: " + path);
        }
    }
}
