package com.antithesis.sdk;

import com.antithesis.ffi.internal.OutputHandler;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test-only support for observing what the {@link Assert} methods emit.
 * <p>
 * The SDK funnels every assertion/guidance through
 * {@code Internal.dispatchOutput -> HandlerFactory.get().output(String)}.  In a
 * normal unit-test run there is no native library, so the handler resolves to a
 * {@code NoOpHandler} that silently discards output, which is why the pre-existing
 * tests could not assert on anything.
 * <p>
 * This helper reflectively installs a {@link CaptureSupport} instance as the
 * {@code HandlerFactory.HANDLER_INSTANCE} so emitted JSON is captured in memory,
 * and reflectively clears the SDK's static de-duplication trackers between tests
 * so each test starts from a clean slate.
 * <p>
 * IMPORTANT: this class only <em>reads</em>/<em>replaces</em> internal state via
 * reflection.  It does not modify any production source, in keeping with the
 * "tests only" constraint.
 */
final class CaptureSupport implements OutputHandler {

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
    static CaptureSupport install() {
        CaptureSupport capture = new CaptureSupport();
        setStaticField("com.antithesis.sdk.internal.HandlerFactory", "HANDLER_INSTANCE", capture);
        clearTrackers();
        return capture;
    }

    static void clearTrackers() {
        clearStaticMap("com.antithesis.sdk.internal.Assertion", "TRACKER");
        clearStaticMap("com.antithesis.sdk.internal.Guidance", "NUMERIC_TRACKERS");
    }

    // ---- queries over what was emitted ---------------------------------------

    /** The inner objects under the {@code "antithesis_assert"} wrapper key. */
    List<JsonNode> assertions() {
        return unwrap("antithesis_assert");
    }

    /** The inner objects under the {@code "antithesis_guidance"} wrapper key. */
    List<JsonNode> guidance() {
        return unwrap("antithesis_guidance");
    }

    List<JsonNode> assertionsFor(final String id) {
        return withId(assertions(), id);
    }

    List<JsonNode> guidanceFor(final String id) {
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

    // ---- reflection plumbing --------------------------------------------------

    private static void setStaticField(final String className, final String fieldName, final Object value) {
        try {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set " + className + "." + fieldName, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void clearStaticMap(final String className, final String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof Map) {
                ((Map<Object, Object>) value).clear();
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to clear " + className + "." + fieldName, e);
        }
    }
}
