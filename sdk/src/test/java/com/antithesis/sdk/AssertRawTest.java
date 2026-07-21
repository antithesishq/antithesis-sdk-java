package com.antithesis.sdk;

import com.antithesis.sdk.Assert.AssertType;
import com.antithesis.sdk.Assert.GuidanceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that the low-level {@link Assert#rawAssert} and
 * {@link Assert#rawGuidance} entry points pass the caller-supplied location
 * information through to the emitted JSON verbatim.
 */
public class AssertRawTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private CaptureSupport capture;

    @BeforeEach
    void setUp() {
        capture = CaptureSupport.install();
    }

    private ObjectNode details() {
        return mapper.createObjectNode();
    }

    @Test
    void rawAssertPassesLocationThrough() {
        Assert.rawAssert(AssertType.Sometimes, "Sometimes",
                "com.example.MyClass", "myFunction", "MyClass.java", 42, 7,
                "raw-assert-id", true, "raw assert message", details(),
                /* hit */ true, /* mustHit */ true);

        JsonNode a = capture.assertionsFor("raw-assert-id").get(0);
        assertEquals("sometimes", a.get("assert_type").asText());
        assertEquals("Sometimes", a.get("display_type").asText());
        assertEquals("raw assert message", a.get("message").asText());
        JsonNode loc = a.get("location");
        assertEquals("com.example.MyClass", loc.get("class").asText());
        assertEquals("myFunction", loc.get("function").asText());
        assertEquals("MyClass.java", loc.get("file").asText());
        assertEquals(42, loc.get("begin_line").asInt());
        assertEquals(7, loc.get("begin_column").asInt());
    }

    @Test
    void rawGuidancePassesLocationAndDataThrough() {
        ObjectNode data = mapper.createObjectNode().put("left", 1).put("right", 2);
        Assert.rawGuidance(GuidanceType.Numeric, data, true,
                "com.example.MyClass", "myFunction", "MyClass.java", 10, 3,
                "raw-guidance-id", "raw guidance message", /* hit */ false);

        JsonNode g = capture.guidanceFor("raw-guidance-id").get(0);
        assertEquals("numeric", g.get("guidance_type").asText());
        assertEquals(true, g.get("maximize").asBoolean());
        assertEquals("raw guidance message", g.get("message").asText());
        JsonNode loc = g.get("location");
        assertEquals("com.example.MyClass", loc.get("class").asText());
        assertEquals(10, loc.get("begin_line").asInt());
        assertEquals(3, loc.get("begin_column").asInt());
    }
}
