package com.antithesis.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers {@link Assert#alwaysSome} and {@link Assert#sometimesAll}: their
 * boolean condition semantics (OR / AND), the boolean guidance direction, and
 * the propositions carried in guidance_data.
 */
public class AssertBooleanGuidanceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private CaptureSupport capture;

    @BeforeEach
    void setUp() {
        capture = CaptureSupport.install();
    }

    private ObjectNode details() {
        return mapper.createObjectNode();
    }

    private Map<String, Boolean> map(final Boolean a, final Boolean b) {
        Map<String, Boolean> m = new LinkedHashMap<>();
        m.put("a", a);
        m.put("b", b);
        return m;
    }

    private JsonNode assertion(final String id) {
        return capture.assertionsFor(id).get(0);
    }

    private JsonNode guidance(final String id) {
        return capture.guidanceFor(id).get(0);
    }

    @Test
    void alwaysSomeIsTrueWhenAnyConditionTrue() {
        Assert.alwaysSome(map(false, true), "some-any-true", details());
        assertEquals(true, assertion("some-any-true").get("condition").asBoolean());
    }

    @Test
    void alwaysSomeIsFalseWhenAllConditionsFalse() {
        Assert.alwaysSome(map(false, false), "some-all-false", details());
        assertEquals(false, assertion("some-all-false").get("condition").asBoolean());
    }

    @Test
    void alwaysSomeGuidanceShape() {
        Assert.alwaysSome(map(true, false), "some-shape", details());
        JsonNode g = guidance("some-shape");
        assertEquals("boolean", g.get("guidance_type").asText(), "guidance_type");
        assertEquals(false, g.get("maximize").asBoolean(), "alwaysSome uses maximize=false");
        assertEquals(true, g.get("guidance_data").get("a").asBoolean(), "proposition a");
        assertEquals(false, g.get("guidance_data").get("b").asBoolean(), "proposition b");
    }

    @Test
    void sometimesAllIsTrueWhenNoConditionFalse() {
        Assert.sometimesAll(map(true, true), "all-none-false", details());
        assertEquals(true, assertion("all-none-false").get("condition").asBoolean());
    }

    @Test
    void sometimesAllIsFalseWhenAnyConditionFalse() {
        Assert.sometimesAll(map(true, false), "all-any-false", details());
        assertEquals(false, assertion("all-any-false").get("condition").asBoolean());
    }

    @Test
    void sometimesAllGuidanceShape() {
        Assert.sometimesAll(map(true, true), "all-shape", details());
        JsonNode g = guidance("all-shape");
        assertEquals("boolean", g.get("guidance_type").asText(), "guidance_type");
        assertEquals(true, g.get("maximize").asBoolean(), "sometimesAll uses maximize=true");
    }

    // --- empty-map corner cases (identity of OR is false, identity of AND is true) ---

    @Test
    void alwaysSomeWithEmptyMapIsFalse() {
        Assert.alwaysSome(new LinkedHashMap<>(), "some-empty", details());
        assertEquals(false, assertion("some-empty").get("condition").asBoolean(),
                "OR of no conditions is false");
    }

    @Test
    void sometimesAllWithEmptyMapIsTrue() {
        Assert.sometimesAll(new LinkedHashMap<>(), "all-empty", details());
        assertEquals(true, assertion("all-empty").get("condition").asBoolean(),
                "AND of no conditions is true");
    }
}
