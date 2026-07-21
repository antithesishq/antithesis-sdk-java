package com.antithesis.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Edge-case / contract tests that pin down behaviours the public API leaves
 * unspecified.  Some of these encode the behaviour a well-behaved API
 * <em>should</em> have and will fail against the current implementation; those
 * failures are intentional and are reported as bugs rather than fixed here.
 */
public class AssertEdgeCaseTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private CaptureSupport capture;

    @BeforeEach
    void setUp() {
        capture = CaptureSupport.install();
    }

    private ObjectNode details() {
        return mapper.createObjectNode();
    }

    /**
     * A helper that merges left/right into the details should not mutate the
     * caller-supplied {@code ObjectNode}.  Mutating the caller's argument is a
     * surprising side effect (and can clobber the caller's own keys).
     */
    @Test
    void numericHelperShouldNotMutateCallerDetails() {
        ObjectNode userDetails = details().put("user", "value");
        Assert.alwaysGreaterThan(5.0, 3.0, "edge-no-mutate", userDetails);

        assertFalse(userDetails.has("left"),
                "caller-supplied details should not have 'left' injected into it");
        assertFalse(userDetails.has("right"),
                "caller-supplied details should not have 'right' injected into it");
    }

    /**
     * Regardless of the mutation question above, the emitted assertion details
     * must contain the merged left/right values.
     */
    @Test
    void numericHelperMergesLeftRightIntoEmittedDetails() {
        Assert.alwaysGreaterThan(5.0, 3.0, "edge-merged", details());
        JsonNode emittedDetails = capture.assertionsFor("edge-merged").get(0).get("details");
        assertEquals(5.0, emittedDetails.get("left").asDouble(), 0.0);
        assertEquals(3.0, emittedDetails.get("right").asDouble(), 0.0);
    }

    @Test
    void mixedNumberTypesAreComparedAsDoubles() {
        Assert.alwaysGreaterThan(3, 2.5, "edge-mixed", details()); // Integer vs Double
        JsonNode g = capture.guidanceFor("edge-mixed").get(0);
        assertEquals(3.0, g.get("guidance_data").get("left").asDouble(), 0.0);
        assertEquals(2.5, g.get("guidance_data").get("right").asDouble(), 0.0);
        assertEquals(true, capture.assertionsFor("edge-mixed").get(0).get("condition").asBoolean());
    }

    /** The plain assertion methods tolerate a null details argument. */
    @Test
    void plainAlwaysToleratesNullDetails() {
        assertDoesNotThrow(() -> Assert.always(true, "edge-null-plain", null));
        assertEquals(1, capture.assertionsFor("edge-null-plain").size());
    }

    /**
     * Documents the current (inconsistent) behaviour: the numeric helpers
     * dereference details and therefore throw on null, unlike the plain methods.
     */
    @Test
    void numericHelperThrowsOnNullDetails_currentBehaviour() {
        assertThrows(NullPointerException.class,
                () -> Assert.alwaysGreaterThan(1.0, 2.0, "edge-null-numeric", null));
    }
}
