package com.antithesis.sdk.lifecycle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

public class LifecycleTest {
    @Test
    void setupCompleteWithoutDetails() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode details = mapper.createObjectNode();

        Lifecycle.setupComplete(details);
    }

    @Test
    void setupCompleteWithDetails() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode details = mapper.createObjectNode();

        details.put("name", "Meow Cat");
        details.put("age", 11);
        details.putArray("phones").add("+1 2126581356").add("+1 2126581384");

        Lifecycle.setupComplete(details);
    }

    @Test
    void sendEventWithoutDetails() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode details = mapper.createObjectNode();

        Lifecycle.sendEvent("my event", details);
    }

    @Test
    void sendEventWithDetails() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode details = mapper.createObjectNode();

        details.put("name", "Tweety Bird");
        details.put("age", 4);
        details.putArray("phones").add("+1 9734970340");

        Lifecycle.sendEvent("my event 2", details);
    }

    @Test
    void sendEventUnnamedWithoutDetails() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode details = mapper.createObjectNode();

        Lifecycle.sendEvent("", details);
    }

    @Test
    void sendEventUnnamedWithDetails() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode details = mapper.createObjectNode();

        details.put("color", "red");

        Lifecycle.sendEvent("    ", details);
    }
}
