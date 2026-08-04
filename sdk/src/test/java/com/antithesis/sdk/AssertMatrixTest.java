package com.antithesis.sdk;

import com.antithesis.sdk.internal.CaptureSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the exact JSON that each core {@link Assert} method emits: the
 * assert_type (lower-cased), display_type, condition, hit, must_hit, and the
 * passthrough of message/id/details.  These pin down the semantic matrix that
 * the pre-existing smoke tests never checked.
 */
public class AssertMatrixTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private CaptureSupport capture;

    @BeforeEach
    void setUp() {
        capture = CaptureSupport.install();
    }

    @AfterEach
    void cleanUp() {
        CaptureSupport.uninstall();
    }

    private ObjectNode details() {
        return mapper.createObjectNode().put("k", "v");
    }

    private JsonNode singleAssertion(final String id) {
        List<JsonNode> found = capture.assertionsFor(id);
        assertEquals(1, found.size(), "expected exactly one emitted assertion for id=" + id);
        return found.get(0);
    }

    private void assertCommon(final JsonNode a, final String assertType, final String displayType,
                              final boolean condition, final boolean hit, final boolean mustHit,
                              final String message) {
        assertEquals(assertType, a.get("assert_type").asText(), "assert_type");
        assertEquals(displayType, a.get("display_type").asText(), "display_type");
        assertEquals(condition, a.get("condition").asBoolean(), "condition");
        assertEquals(hit, a.get("hit").asBoolean(), "hit");
        assertEquals(mustHit, a.get("must_hit").asBoolean(), "must_hit");
        assertEquals(message, a.get("id").asText(), "id");
        assertEquals(message, a.get("message").asText(), "message");
        assertTrue(a.has("location"), "location present");
        assertTrue(a.has("details"), "details present");
        assertEquals("v", a.get("details").get("k").asText(), "details passthrough");
    }

    @Test
    void alwaysEmitsExpectedShape() {
        Assert.always(true, "matrix-always", details());
        assertCommon(singleAssertion("matrix-always"),
                "always", "Always", true, true, true, "matrix-always");
    }

    @Test
    void alwaysWithFalseConditionStillCarriesCondition() {
        Assert.always(false, "matrix-always-false", details());
        assertCommon(singleAssertion("matrix-always-false"),
                "always", "Always", false, true, true, "matrix-always-false");
    }

    @Test
    void alwaysOrUnreachableEmitsExpectedShape() {
        Assert.alwaysOrUnreachable(true, "matrix-aou", details());
        assertCommon(singleAssertion("matrix-aou"),
                "always", "AlwaysOrUnreachable", true, true, false, "matrix-aou");
    }

    @Test
    void sometimesEmitsExpectedShape() {
        Assert.sometimes(true, "matrix-sometimes", details());
        assertCommon(singleAssertion("matrix-sometimes"),
                "sometimes", "Sometimes", true, true, true, "matrix-sometimes");
    }

    @Test
    void reachableEmitsExpectedShape() {
        Assert.reachable("matrix-reachable", details());
        assertCommon(singleAssertion("matrix-reachable"),
                "reachability", "Reachable", true, true, true, "matrix-reachable");
    }

    @Test
    void unreachableEmitsExpectedShape() {
        Assert.unreachable("matrix-unreachable", details());
        assertCommon(singleAssertion("matrix-unreachable"),
                "reachability", "Unreachable", false, true, false, "matrix-unreachable");
    }
}
