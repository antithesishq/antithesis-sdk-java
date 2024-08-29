package com.antithesis.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

public class AssertionTest {
    @Test
    void testAlwaysAssertion() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode details = mapper.createObjectNode();

        details.put("color", "always red");
        details.put("extent", 15);

        Assert.always(true, "Always message", details);
    }

    @Test
    void testSometimesAssertion() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode details = mapper.createObjectNode();

        details.put("color", "sometimes red");
        details.put("extent", 17);

        Assert.sometimes(true, "Sometimes message", details);
    }

    @Test
    void testReachableAssertion() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode details = mapper.createObjectNode();

        details.put("color", "reachable red");
        details.put("extent", 19);

        Assert.reachable("Reachable message", details);
    }
}
