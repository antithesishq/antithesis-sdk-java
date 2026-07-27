package com.antithesis.sdk;

import com.antithesis.sdk.internal.CaptureSupport;

import com.antithesis.sdk.Assert.AssertType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises the de-duplication contract implemented in
 * {@code Assertion.trackEntry}: for a given id, only the first pass and the
 * first fail should be emitted, catalog entries (hit=false) should always be
 * emitted, and a pass followed by a fail should produce two emissions.
 */
public class AssertDedupTest {

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
        return mapper.createObjectNode();
    }

    @Test
    void repeatedPassesEmitOnlyOnce() {
        for (int i = 0; i < 5; i++) {
            Assert.always(true, "dedup-pass", details());
        }
        assertEquals(1, capture.assertionsFor("dedup-pass").size(),
                "only the first passing hit should be emitted");
    }

    @Test
    void repeatedFailsEmitOnlyOnce() {
        for (int i = 0; i < 5; i++) {
            Assert.always(false, "dedup-fail", details());
        }
        assertEquals(1, capture.assertionsFor("dedup-fail").size(),
                "only the first failing hit should be emitted");
    }

    @Test
    void firstPassAndFirstFailBothEmit() {
        Assert.always(true, "dedup-pass-then-fail", details());
        Assert.always(false, "dedup-pass-then-fail", details());
        Assert.always(true, "dedup-pass-then-fail", details());
        Assert.always(false, "dedup-pass-then-fail", details());
        assertEquals(2, capture.assertionsFor("dedup-pass-then-fail").size(),
                "one emission for the first pass and one for the first fail");
    }

    @Test
    void catalogEntriesAlwaysEmit() {
        // hit=false is a catalog entry; per the contract it must emit every time.
        for (int i = 0; i < 3; i++) {
            Assert.rawAssert(AssertType.Always, "Always",
                    "com.example.Klass", "fn", "File.java", 1, 2,
                    "dedup-catalog", true, "dedup-catalog", details(),
                    /* hit */ false, /* mustHit */ true);
        }
        assertEquals(3, capture.assertionsFor("dedup-catalog").size(),
                "every catalog entry (hit=false) should be emitted");
    }
}
