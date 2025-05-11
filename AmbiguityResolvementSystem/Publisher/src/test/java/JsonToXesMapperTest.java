import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import publisherpackage.JsonToXesMapper;

import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

public class JsonToXesMapperTest {

    private JsonToXesMapper mapper;

    @BeforeEach
    public void setUp() {
        mapper = new JsonToXesMapper();
    }

    @Test
    public void testMapValidJsonToXes() throws Exception {
        String json = "{"
                + "\"concept:name\": \"EJUB\", "
                + "\"time:timestamp\": \"2024-09-11T15:56:16.000+00:00\", "
                + "\"perform:donor\": \"D001\", "
                + "\"location:station\": \"Left station\""
                + "}";

        String xes = mapper.convertJsonToXes(json);

        assertTrue(xes.contains("<event>"));
        assertTrue(xes.contains("<string key=\"concept:name\" value=\"EJUB\"/>"));
        assertTrue(xes.contains("<date key=\"time:timestamp\" value=\"2024-09-11T15:56:16.000+00:00\"/>"));
        assertTrue(xes.contains("<string key=\"perform:donor\" value=\"D001\"/>"));
        assertTrue(xes.contains("<string key=\"location:station\" value=\"Left station\"/>"));
    }

    @Test
    public void testEmptyJson() throws Exception {
        String json = "{}";
        String xes = mapper.convertJsonToXes(json);
        assertFalse(xes.contains("<event>"));
        // Should not contain any attributes
        assertEquals("<event/>", xes.trim());
    }

    @Test
    public void testJsonWithOnlyTimestamp() throws Exception {
        String json = "{ \"time:timestamp\": \"2024-05-11T12:34:56Z\" }";
        String xes = mapper.convertJsonToXes(json);

        assertTrue(xes.contains("<date key=\"time:created\" value=\"2024-05-11T12:34:56Z\"/>"));
    }

    @Test
    public void testJsonThrowsException() {
        String malformedJson = "{ \"concept:name\": \"Test\" "; // missing closing }

        assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class, () -> {
            mapper.convertJsonToXes(malformedJson);
        });
    }
    @Test
    public void testXmlFormatCompliance() throws Exception {
        String json = "{ \"concept:name\": \"Test\", \"time:timestamp\": \"2024-01-01T00:00:00Z\" }";
        String xes = mapper.convertJsonToXes(json);

        // Rough check for XML format: starts with <event>, ends with </event> or self-closing
        assertTrue(Pattern.compile("^<event.*>.*</event>$|^<event/>$", Pattern.DOTALL).matcher(xes.trim()).find());
    }
}
