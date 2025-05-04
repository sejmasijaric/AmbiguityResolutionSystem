package mlpackage;

import java.util.List;

public interface MLServiceClient {
    public String analyzeFrames(List<String> frame_paths) throws Exception;
}
