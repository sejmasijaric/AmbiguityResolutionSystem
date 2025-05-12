import mlpackage.ConfigLoader;
import mlpackage.MLServiceClientImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MLServiceClientImplTest {

    @Mock
    private ConfigLoader configLoader;

    @InjectMocks
    private MLServiceClientImpl mlServiceClient;

    @Test
    void analyzeFrames_ReturnsValidOutput_WhenConfidenceAboveThreshold() throws Exception {
        List<String> framePaths = List.of("/path/to/frame1.jpg", "/path/to/frame2.jpg");
        String baseUrl = "http://ml-service.com";
        String requestEndpoint = "/analyze";
        String confidenceThreshold = "0.8";
        String mockResponse = "{ \"result\": { \"confidence\": 0.85 } }";

        when(configLoader.get("ml.baseUrl")).thenReturn(baseUrl);
        when(configLoader.get("ml.requestEndpoint")).thenReturn(requestEndpoint);
        when(configLoader.get("ml.confidenceThreshold")).thenReturn(confidenceThreshold);

        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(mockResponse.getBytes()));

        try (MockedStatic<URI> mockedUri = mockStatic(URI.class);
             MockedStatic<URL> mockedUrl = mockStatic(URL.class)) {
            URL mockUrl = mock(URL.class);
            when(mockUrl.openConnection()).thenReturn(mockConnection);
            mockedUrl.when(() -> new URI(baseUrl + requestEndpoint).toURL()).thenReturn(mockUrl);

            String result = mlServiceClient.analyzeFrames(framePaths);

            assertNotNull(result);
            assertTrue(result.contains("\"resolved_ambiguity\":true"));
        }
    }

    @Test
    void analyzeFrames_ThrowsIOException_WhenResponseCodeNotOk() throws Exception {
        List<String> framePaths = List.of("/path/to/frame1.jpg", "/path/to/frame2.jpg");
        String baseUrl = "http://ml-service.com";
        String requestEndpoint = "/analyze";

        when(configLoader.get("ml.baseUrl")).thenReturn(baseUrl);
        when(configLoader.get("ml.requestEndpoint")).thenReturn(requestEndpoint);

        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

        try (MockedStatic<URI> mockedUri = mockStatic(URI.class);
             MockedStatic<URL> mockedUrl = mockStatic(URL.class)) {
            URL mockUrl = mock(URL.class);
            when(mockUrl.openConnection()).thenReturn(mockConnection);
            mockedUrl.when(() -> new URI(baseUrl + requestEndpoint).toURL()).thenReturn(mockUrl);

            assertThrows(IOException.class, () -> mlServiceClient.analyzeFrames(framePaths));
        }
    }

    @Test
    void analyzeFrames_ReturnsValidOutput_WhenConfidenceBelowThreshold() throws Exception {
        List<String> framePaths = List.of("/path/to/frame1.jpg", "/path/to/frame2.jpg");
        String baseUrl = "http://ml-service.com";
        String requestEndpoint = "/analyze";
        String confidenceThreshold = "0.8";
        String mockResponse = "{ \"result\": { \"confidence\": 0.5 } }";

        when(configLoader.get("ml.baseUrl")).thenReturn(baseUrl);
        when(configLoader.get("ml.requestEndpoint")).thenReturn(requestEndpoint);
        when(configLoader.get("ml.confidenceThreshold")).thenReturn(confidenceThreshold);

        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(mockResponse.getBytes()));

        try (MockedStatic<URI> mockedUri = mockStatic(URI.class);
             MockedStatic<URL> mockedUrl = mockStatic(URL.class)) {
            URL mockUrl = mock(URL.class);
            when(mockUrl.openConnection()).thenReturn(mockConnection);
            mockedUrl.when(() -> new URI(baseUrl + requestEndpoint).toURL()).thenReturn(mockUrl);

            String result = mlServiceClient.analyzeFrames(framePaths);

            assertNotNull(result);
            assertTrue(result.contains("\"resolved_ambiguity\":false"));
        }
    }

    @Test
    void analyzeFrames_ThrowsIOException_WhenConnectionFails() throws Exception {
        List<String> framePaths = List.of("/path/to/frame1.jpg", "/path/to/frame2.jpg");
        String baseUrl = "http://ml-service.com";
        String requestEndpoint = "/analyze";

        when(configLoader.get("ml.baseUrl")).thenReturn(baseUrl);
        when(configLoader.get("ml.requestEndpoint")).thenReturn(requestEndpoint);

        try (MockedStatic<URI> mockedUri = mockStatic(URI.class);
             MockedStatic<URL> mockedUrl = mockStatic(URL.class)) {
            mockedUrl.when(() -> new URI(baseUrl + requestEndpoint).toURL()).thenThrow(new IOException());

            assertThrows(IOException.class, () -> mlServiceClient.analyzeFrames(framePaths));
        }
    }
}