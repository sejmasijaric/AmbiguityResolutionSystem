package camerapackage;

import java.util.List;

public interface CameraServiceClient {

    public List<String> captureFrames() throws Exception;
}
