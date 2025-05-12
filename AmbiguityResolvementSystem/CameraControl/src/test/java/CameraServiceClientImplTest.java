package camerapackage;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CameraServiceClientImplTest {

    private CameraConfigLoader mockConfig;
    private CameraServiceClientImpl client;

    @BeforeEach
    public void setUp() {
        mockConfig = mock(CameraConfigLoader.class);
        client = Mockito.spy(new CameraServiceClientImpl(mockConfig));

        when(mockConfig.get("cameraControl.baseUrl")).thenReturn("http://localhost:8000");
        when(mockConfig.get("cameraControl.startCameraEndpoint")).thenReturn("/start-camera");
        when(mockConfig.get("cameraControl.stopCameraEndpoint")).thenReturn("/stop-camera");
        when(mockConfig.get("cameraControl.captureFrameEndpoint")).thenReturn("/capture-frame");
        when(mockConfig.get("cameraControl.numberOfFrames")).thenReturn("3");
        when(mockConfig.get("cameraControl.waitingTime")).thenReturn("10");
    }

    @Test
    public void testGetFrames_SuccessfulCapture() throws Exception {
        // Mock sendGetRequest to simulate successful capture
        String mockResponse = "{\"status\": \"successful\", \"filepath\": \"frame1.jpg\"}";
        doReturn(mockResponse).when(client).sendGetRequest("/capture-frame");

        List<String> frames = client.getFrames();

        assertEquals(3, frames.size());
        assertTrue(frames.stream().allMatch(p -> p.equals("frame1.jpg")));
    }

    @Test
    public void testGetFrames_CaptureFailsOnce() throws Exception {
        String captureFrameEndpoint = mockConfig.get("cameraControl.captureFrameEndpoint");

        // Start and Stop camera should be stubbed too
        doReturn("{\"status\": \"camera started\"}").when(client).sendGetRequest("/start-camera");
        doReturn("{\"status\": \"Camera stopped!\"}").when(client).sendGetRequest("/stop-camera");

        doReturn("{\"status\": \"successful\", \"filepath\": \"frame2.jpg\"}")
                .doReturn("{\"status\": \"successful\", \"filepath\": \"frame3.jpg\"}")
                .doReturn("{\"status\": \"successful\", \"filepath\": \"frame4.jpg\"}")
                .when(client).sendGetRequest(captureFrameEndpoint);

        List<String> frames = client.getFrames();

        assertEquals(3, frames.size());
        assertTrue(frames.contains("frame2.jpg"));
        assertTrue(frames.contains("frame3.jpg"));
    }

    @Test
    public void testCaptureFrame_HandlesUnsuccessfulStatus() throws Exception {
        doReturn("{\"status\": \"fail\"}").when(client).sendGetRequest("/capture-frame");

        String result = client.captureFrame();

        assertNull(result);
    }

    @Test
    public void testCaptureFrame_SuccessfulJsonParsing() throws Exception {
        doReturn("{\"status\": \"successful\", \"filepath\": \"frame5.jpg\"}").when(client).sendGetRequest("/capture-frame");

        String result = client.captureFrame();

        assertEquals("frame5.jpg", result);
    }

    @Test
    public void testCaptureFrame_ThrowsParseException() {
        assertThrows(ParseException.class, () -> client.parseJsonResponse("INVALID_JSON"));
    }

    @Test
    public void testSendGetRequest_InvalidURL_ThrowsIOException() {
        when(mockConfig.get("cameraControl.baseUrl")).thenReturn("http://localhost:8000");
        assertThrows(IOException.class, () -> client.sendGetRequest("/capture")); // invalid endpoint
    }
}
