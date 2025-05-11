import org.junit.jupiter.api.Test;
import publisherpackage.ConfigLoader;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigLoaderTest {

    @Test
    public void testConfigLoaderReadsProperties() {
        ConfigLoader loader = new ConfigLoader();
        assertEquals("clean-events", loader.get("publisher.topicUnambiguousEvent"));
        assertEquals("ambiguous-events", loader.get("publisher.topicAmbiguousEvent"));
    }

    @Test
    public void testConfigLoaderReturnsNullForMissingKey() {
        ConfigLoader loader = new ConfigLoader();
        assertNull(loader.get("publisher.topic"));
    }
}
