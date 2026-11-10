package com.antithesis.sdk;

import com.antithesis.sdk.internal.CaptureSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class EmissionGateTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private CaptureSupport capture;

    @BeforeEach void install() { capture = CaptureSupport.install(); }
    @AfterEach void uninstall() { CaptureSupport.uninstall(); }

    @Test void numericConversionsRemainEagerWhenBothRecordsAreSuppressed() {
        List<String> conversions = new ArrayList<>();
        Number left = new CountingNumber("left", 2, conversions);
        Number right = new CountingNumber("right", 1, conversions);
        for (int i = 0; i < 10; i++) {
            Assert.alwaysGreaterThan(left, right, "eager", null);
        }
        assertEquals(20, conversions.size());
        for (int i = 0; i < conversions.size(); i += 2) {
            assertEquals("left", conversions.get(i));
            assertEquals("right", conversions.get(i + 1));
        }
        assertEquals(1, capture.assertionsFor("eager").size());
        assertEquals(1, capture.guidanceFor("eager").size());
    }

    @Test void assertionAndGuidanceGatesAreIndependent() {
        Assert.rawGuidance(Assert.GuidanceType.Numeric,
                MAPPER.createObjectNode().put("left", 100).put("right", 0), true,
                "class", "function", "file", 1, 1, "independent", "independent", true);
        Assert.sometimesGreaterThan(2, 1, "independent", null);
        assertEquals(1, capture.assertionsFor("independent").size());
        assertEquals(1, capture.guidanceFor("independent").size());
        JsonNode details = capture.assertionsFor("independent").get(0).get("details");
        assertEquals(2, details.get("left").asInt());
        assertEquals(1, details.get("right").asInt());
        Assert.sometimesGreaterThan(200, 1, "independent", null);
        assertEquals(1, capture.assertionsFor("independent").size());
        assertEquals(2, capture.guidanceFor("independent").size());
        JsonNode data = capture.guidanceFor("independent").get(1).get("guidance_data");
        assertEquals(200, data.get("left").asInt());
        assertEquals(1, data.get("right").asInt());
    }

    @Test void rawEmissionUsesCurrentLocationWhileOrdinaryHitsUseRegistration() {
        raw("registered", false, false);
        raw("emitted", true, true);
        raw("suppressed", true, true);
        Assert.always(false, "location", null);
        List<JsonNode> records = capture.assertionsFor("location");
        assertEquals(3, records.size());
        assertEquals("registered", records.get(0).get("location").get("file").asText());
        assertEquals("emitted", records.get(1).get("location").get("file").asText());
        assertEquals("registered", records.get(2).get("location").get("file").asText());
    }

    @Test void concurrentIdenticalHitsEmitOnlyOncePerOutcomeAndWatermark() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> tasks = new ArrayList<>();
        try {
            for (int i = 0; i < 8; i++) {
                tasks.add(executor.submit(() -> {
                    start.await();
                    for (int j = 0; j < 100; j++) {
                        Assert.alwaysGreaterThan(2, 1, "concurrent", null);
                        Assert.alwaysGreaterThan(0, 1, "concurrent", null);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> task : tasks) task.get();
        } finally {
            executor.shutdownNow();
        }
        assertEquals(2, capture.assertionsFor("concurrent").size());
        // A losing thread may reach the lower watermark before the first
        // thread reaches guidance, so the higher watermark may never emit.
        List<JsonNode> guidance = capture.guidanceFor("concurrent");
        assertTrue(guidance.size() >= 1 && guidance.size() <= 2);
        assertEquals(1, guidance.stream().filter(n -> n.get("guidance_data").get("left").asInt() == 0).count());
    }

    private void raw(String file, boolean condition, boolean hit) {
        Assert.rawAssert(Assert.AssertType.Always, "Always", "class", "function", file,
                1, 1, "location", condition, "location", null, hit, true);
    }

    private static final class CountingNumber extends Number {
        private final String name;
        private final double value;
        private final List<String> conversions;

        CountingNumber(String name, double value, List<String> conversions) {
            this.name = name;
            this.value = value;
            this.conversions = conversions;
        }
        @Override public double doubleValue() { conversions.add(name); return value; }
        @Override public int intValue() { throw new AssertionError(); }
        @Override public long longValue() { throw new AssertionError(); }
        @Override public float floatValue() { throw new AssertionError(); }
    }
}
