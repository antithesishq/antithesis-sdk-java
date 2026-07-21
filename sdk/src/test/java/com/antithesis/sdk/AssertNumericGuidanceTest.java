package com.antithesis.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers numeric guidance emitted by the {@code always/sometimes *Than*} helpers:
 * the guidance_data payload, the {@code maximize} direction, the "strictly better
 * example" gating, and the NaN carve-out.
 * <p>
 * The expected {@code maximize} values are taken from the reference Antithesis
 * SDK convention: for a given comparison operator the "sometimes" variant uses
 * the opposite direction from the "always" variant.
 */
public class AssertNumericGuidanceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private CaptureSupport capture;

    @BeforeEach
    void setUp() {
        capture = CaptureSupport.install();
    }

    private ObjectNode details() {
        return mapper.createObjectNode();
    }

    private JsonNode singleGuidance(final String id) {
        List<JsonNode> found = capture.guidanceFor(id);
        assertEquals(1, found.size(), "expected exactly one guidance for id=" + id);
        return found.get(0);
    }

    @Test
    void numericGuidanceCarriesLeftRightAndType() {
        Assert.alwaysGreaterThan(7.0, 4.0, "num-data", details());
        JsonNode g = singleGuidance("num-data");
        assertEquals("numeric", g.get("guidance_type").asText(), "guidance_type");
        assertTrue(g.get("hit").asBoolean(), "hit");
        assertEquals("num-data", g.get("id").asText(), "id");
        JsonNode data = g.get("guidance_data");
        assertEquals(7.0, data.get("left").asDouble(), 0.0, "left");
        assertEquals(4.0, data.get("right").asDouble(), 0.0, "right");
    }

    @Test
    void guidanceDataIsAlsoMergedIntoAssertionDetails() {
        Assert.alwaysGreaterThan(7.0, 4.0, "num-merge", details());
        JsonNode a = capture.assertionsFor("num-merge").get(0);
        assertEquals(7.0, a.get("details").get("left").asDouble(), 0.0, "left merged into details");
        assertEquals(4.0, a.get("details").get("right").asDouble(), 0.0, "right merged into details");
    }

    /**
     * Reference convention: "sometimes" inverts the maximize direction relative
     * to "always" for the same operator.
     */
    @ParameterizedTest(name = "{0} -> maximize={1}")
    @CsvSource({
            "alwaysGreaterThan,false",
            "alwaysGreaterThanOrEqualTo,false",
            "alwaysLessThan,true",
            "alwaysLessThanOrEqualTo,true",
            "sometimesGreaterThan,true",
            "sometimesGreaterThanOrEqualTo,true",
            "sometimesLessThan,false",
            "sometimesLessThanOrEqualTo,false",
    })
    void maximizeDirectionMatchesReference(final String method, final boolean expectedMaximize) {
        invoke(method, 5.0, 3.0, method);
        JsonNode g = singleGuidance(method);
        assertEquals(expectedMaximize, g.get("maximize").asBoolean(),
                method + " should emit guidance with maximize=" + expectedMaximize);
    }

    private void invoke(final String method, final double left, final double right, final String message) {
        ObjectNode d = details();
        switch (method) {
            case "alwaysGreaterThan":
                Assert.alwaysGreaterThan(left, right, message, d); break;
            case "alwaysGreaterThanOrEqualTo":
                Assert.alwaysGreaterThanOrEqualTo(left, right, message, d); break;
            case "alwaysLessThan":
                Assert.alwaysLessThan(left, right, message, d); break;
            case "alwaysLessThanOrEqualTo":
                Assert.alwaysLessThanOrEqualTo(left, right, message, d); break;
            case "sometimesGreaterThan":
                Assert.sometimesGreaterThan(left, right, message, d); break;
            case "sometimesGreaterThanOrEqualTo":
                Assert.sometimesGreaterThanOrEqualTo(left, right, message, d); break;
            case "sometimesLessThan":
                Assert.sometimesLessThan(left, right, message, d); break;
            case "sometimesLessThanOrEqualTo":
                Assert.sometimesLessThanOrEqualTo(left, right, message, d); break;
            default:
                throw new IllegalArgumentException("unknown method " + method);
        }
    }

    /**
     * With maximize=false (as alwaysGreaterThan uses), guidance should only be
     * re-emitted when a strictly smaller (left-right) is seen.
     */
    @Test
    void guidanceReEmittedOnlyOnStrictlyBetterExample() {
        String id = "num-strictly-better";
        Assert.alwaysGreaterThan(10.0, 0.0, id, details()); // diff 10  -> emit (1)
        Assert.alwaysGreaterThan(10.0, 5.0, id, details()); // diff 5   -> emit (2)
        Assert.alwaysGreaterThan(10.0, 5.0, id, details()); // diff 5   -> no
        Assert.alwaysGreaterThan(10.0, 3.0, id, details()); // diff 7   -> no
        Assert.alwaysGreaterThan(10.0, 8.0, id, details()); // diff 2   -> emit (3)
        assertEquals(3, capture.guidanceFor(id).size(),
                "guidance should re-emit only on a strictly better (smaller) left-right");
    }

    /**
     * A NaN (left-right) should be reported but must not corrupt the tracked
     * mark: a subsequent non-improving finite value must still be suppressed.
     */
    @Test
    void nanIsReportedButDoesNotUpdateMark() {
        String id = "num-nan";
        Assert.alwaysGreaterThan(10.0, 5.0, id, details());          // diff 5   -> emit (1), mark=5
        Assert.alwaysGreaterThan(Double.NaN, 5.0, id, details());    // diff NaN -> emit (2), mark stays 5
        Assert.alwaysGreaterThan(10.0, 5.0, id, details());          // diff 5   -> no (mark still 5)
        assertEquals(2, capture.guidanceFor(id).size(),
                "NaN should be reported once but must not move the mark to NaN");
    }
}
