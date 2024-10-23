package com.antithesis.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

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

    @Test
    void testRichAssertion() {
        ObjectMapper mapper = new ObjectMapper();
        for (int i = 0; i < 10; i++) {
            int v = (i % 2 == 0 ? -i : i) * 5;
            ObjectNode details = mapper.createObjectNode().put("field", 30);
            Assert.alwaysGreaterThan(10, v, "Rich assertion greater than", details);
            Assert.alwaysLessThan(10, v, "Rich assertion less than", details);
        }
        Map<String, Boolean> conditions = new HashMap<>();
        conditions.put("a", true);
        conditions.put("b", true);
        conditions.put("c", false);
        Assert.sometimesAll(conditions, "Rich assertion sometimes all", mapper.createObjectNode());
    }
}
