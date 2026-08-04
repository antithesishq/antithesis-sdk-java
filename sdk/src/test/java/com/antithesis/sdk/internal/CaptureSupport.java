package com.antithesis.sdk.internal;

import com.antithesis.ffi.internal.OutputHandler;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test-only support for observing what the {@code Assert} methods emit.
 * <p>
 * The SDK funnels every assertion/guidance through
 * {@code Internal.dispatchOutput -> HandlerFactory.get().output(String)}.  In a
 * normal unit-test run there is no native library, so the handler resolves to a
 * {@code NoOpHandler} that silently discards output.  This helper installs an
 * in-memory {@link CaptureSupport} instance via the package-private
 * {@link HandlerFactory#useHandler} test seam and resets the SDK's static
 * de-duplication trackers, so each test starts from a clean slate and can assert
 * on the emitted JSON.
 */
public final class CaptureSupport implements OutputHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            // The SDK writes Double.NaN / Infinity as bare NaN / Infinity tokens;
            // allow the capturing reader to parse them back.
            .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);

    private final List<JsonNode> emitted = new CopyOnWriteArrayList<>();

    @Override
    public void output(final String value) {
        try {
            emitted.add(MAPPER.readTree(value));
        } catch (Exception e) {
            throw new RuntimeException("Captured output was not valid JSON: " + value, e);
        }
    }

    @Override
    public long random() {
        return 0L;
    }

    // ---- installation / reset -------------------------------------------------

    /**
     * Installs a fresh capturing handler and clears the static trackers.
     * Call from a {@code @BeforeEach}.
     */
    public static CaptureSupport install() {
        CaptureSupport capture = new CaptureSupport();
        HandlerFactory.useHandler(capture);
        Assertion.resetTracking();
        Guidance.resetTracking();
        return capture;
    }

    /**
     * Uninstalls the capturing handler and clears the static trackers.
     * Call from a {@code @BeforeEach}.
     */
    public static void uninstall() {
        Assertion.resetTracking();
        Guidance.resetTracking();
        HandlerFactory.useHandler(null);
    }

    // ---- queries over what was emitted ---------------------------------------

    /** The inner objects under the {@code "antithesis_assert"} wrapper key. */
    public List<JsonNode> assertions() {
        return unwrap("antithesis_assert");
    }

    /** The inner objects under the {@code "antithesis_guidance"} wrapper key. */
    public List<JsonNode> guidance() {
        return unwrap("antithesis_guidance");
    }

    public List<JsonNode> assertionsFor(final String id) {
        return withId(assertions(), id);
    }

    public List<JsonNode> guidanceFor(final String id) {
        return withId(guidance(), id);
    }

    private List<JsonNode> unwrap(final String wrapperKey) {
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode node : emitted) {
            if (node.has(wrapperKey)) {
                out.add(node.get(wrapperKey));
            }
        }
        return out;
    }

    private static List<JsonNode> withId(final List<JsonNode> nodes, final String id) {
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode node : nodes) {
            JsonNode idNode = node.get("id");
            if (idNode != null && id.equals(idNode.asText())) {
                out.add(node);
            }
        }
        return out;
    }
}
