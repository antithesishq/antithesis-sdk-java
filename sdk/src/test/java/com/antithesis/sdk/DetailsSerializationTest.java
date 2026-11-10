package com.antithesis.sdk;

import com.antithesis.sdk.internal.CaptureSupport;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * How and when caller-supplied details are serialized: once per emission and
 * never for suppressed hits, merged with generated guidance without mutating
 * the caller's node, and reported (rather than swallowed) when serialization
 * fails.  The shape of the emitted JSON and the diagnostic packets are pinned
 * by the cross-SDK conformance harness, not here; what remains are the public
 * API paths and the Jackson write paths (bulk primitive arrays, writeObject,
 * writeTree, parser copies) that the harness's interpreter cannot express.
 */
class DetailsSerializationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private CaptureSupport capture;

    @BeforeEach void install() { capture = CaptureSupport.install(); }
    @AfterEach void uninstall() { CaptureSupport.uninstall(); }

    @Test void detailsSerializeOncePerEmission() {
        CountingValue value = new CountingValue(false);
        ObjectNode details = MAPPER.createObjectNode().putPOJO("value", value);
        for (int i = 0; i < 10; i++) {
            Assert.always(true, "counted", details);
            Assert.always(false, "counted", details);
        }
        assertEquals(2, value.calls);
        assertEquals(2, capture.assertions().size());
    }

    @Test void primitiveArraysReceiveNumericDiagnostics() {
        ObjectNode details = MAPPER.createObjectNode()
                .putPOJO("doubles", new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1.5})
                .putPOJO("longs", new long[]{Long.MAX_VALUE, Long.MIN_VALUE});
        Assert.always(true, "primitive arrays", details);

        assertNumericWarnings(capture.records());
        JsonNode emitted = capture.assertions().get(0).get("details");
        assertEquals("NaN", emitted.get("doubles").get(0).asText());
        assertEquals("Infinity", emitted.get("doubles").get(1).asText());
        assertEquals(-1.5, emitted.get("doubles").get(2).doubleValue());
        assertEquals(Long.MAX_VALUE, emitted.get("longs").get(0).longValue());
        assertEquals(Long.MIN_VALUE, emitted.get("longs").get(1).longValue());
    }

    @Test void customNodesSerializeOnceThroughObjectTreeAndParserWrites() {
        WriteValue[] serializers = {
                generator -> generator.writeObject(Map.of("value", Long.MAX_VALUE)),
                generator -> generator.writeTree(MAPPER.createObjectNode().put("value", Long.MAX_VALUE)),
                generator -> {
                    try (JsonParser parser = MAPPER.createParser("{\"value\":9223372036854775807}")) {
                        parser.nextToken();
                        generator.copyCurrentStructure(parser);
                    }
                }
        };
        for (int i = 0; i < serializers.length; i++) {
            WritingNode value = new WritingNode(serializers[i]);
            ObjectNode details = MAPPER.createObjectNode().set("custom", value);
            int start = capture.records().size();
            Assert.always(true, "custom node " + i, details);

            assertEquals(1, value.calls);
            assertEquals(start + (i == 0 ? 2 : 1), capture.records().size());
            if (i == 0) {
                assertEquals(6003, capture.records().get(start).path("antithesis_warning").path("code").asInt());
            }
            JsonNode emitted = capture.assertions().get(i).get("details").get("custom");
            assertEquals(Long.MAX_VALUE, emitted.get("value").longValue());
        }
    }

    @Test void bulkArraySlicesInspectOnlyWrittenElements() {
        WritingNode value = new WritingNode(generator -> {
            generator.writeStartArray();
            generator.writeArray(new double[]{Double.NaN, 0x1p53, Double.POSITIVE_INFINITY}, 1, 1);
            generator.writeArray(new long[]{Long.MAX_VALUE, 7, Long.MIN_VALUE}, 1, 1);
            generator.writeEndArray();
        });
        Assert.always(true, "array slices", MAPPER.createObjectNode().set("custom", value));

        assertEquals(1, value.calls);
        assertEquals(1, capture.records().size());
        JsonNode emitted = capture.assertions().get(0).get("details").get("custom");
        assertEquals(0x1p53, emitted.get(0).get(0).doubleValue());
        assertEquals(7, emitted.get(1).get(0).intValue());
        assertEquals(1, emitted.get(0).size());
        assertEquals(1, emitted.get(1).size());
    }

    @Test void serializationFailureDiscardsNumericWarningsWithoutConsumingThem() {
        WritingNode value = new WritingNode(generator -> {
            generator.writeStartArray();
            generator.writeNumber(Double.NaN);
            generator.writeNumber(Long.MAX_VALUE);
            throw new IOException("deliberate serialization failure");
        });
        ObjectNode details = MAPPER.createObjectNode().set("custom", value);
        assertDoesNotThrow(() -> Assert.always(true, "failure after warnings", details));

        assertEquals(1, value.calls);
        assertEquals(2, capture.records().size());
        assertSerializationError(capture.records().get(0), "failure after warnings");
        assertFalse(capture.assertions().get(0).has("details"));
        assertSame(value, details.get("custom"));

        Assert.always(true, "after failure", MAPPER.createObjectNode().put("nan", Double.NaN).put("big", Long.MAX_VALUE));
        List<JsonNode> records = capture.records();
        assertNumericWarnings(records.subList(2, records.size()));
    }

    private static void assertNumericWarnings(List<JsonNode> records) {
        assertEquals(3, records.size());
        assertEquals(6002, records.get(0).path("antithesis_warning").path("code").asInt());
        assertEquals(6003, records.get(1).path("antithesis_warning").path("code").asInt());
    }

    @FunctionalInterface
    private interface WriteValue {
        void write(JsonGenerator generator) throws IOException;
    }

    private static final class WritingNode extends ValueNode {
        final WriteValue write;
        int calls;

        WritingNode(WriteValue write) { this.write = write; }

        @Override public void serialize(JsonGenerator generator, SerializerProvider provider) throws IOException {
            calls++;
            write.write(generator);
        }

        @Override public JsonToken asToken() { return JsonToken.VALUE_EMBEDDED_OBJECT; }
        @Override public JsonNodeType getNodeType() { return JsonNodeType.POJO; }
        @Override public String asText() { return "custom"; }
        @Override public boolean equals(Object other) { return this == other; }
        @Override public int hashCode() { return System.identityHashCode(this); }
    }

    @Test void richAssertionsPreserveCallerDetailsAndOverrideOperands() {
        ObjectNode details = MAPPER.createObjectNode().put("left", "caller").put("extra", 7);
        Assert.alwaysGreaterThan(2, 1, "rich", details);
        JsonNode emitted = capture.assertionsFor("rich").get(0).get("details");
        assertEquals(2, emitted.get("left").asInt());
        assertEquals(1, emitted.get("right").asInt());
        assertEquals(7, emitted.get("extra").asInt());
        assertEquals("caller", details.get("left").asText());
        assertFalse(details.has("right"));
    }

    @Test void suppressedRichAssertionsDoNotTraverseCallerDetails() {
        CountingMap fields = new CountingMap();
        ObjectNode details = new ObjectNode(JsonNodeFactory.instance, fields).put("extra", 7);
        Assert.alwaysGreaterThan(2, 1, "rich counted", details);
        int traversals = fields.traversals;
        assertTrue(traversals > 0);
        for (int i = 0; i < 10; i++) {
            Assert.alwaysGreaterThan(2, 1, "rich counted", details);
        }
        assertEquals(traversals, fields.traversals);
    }

    @Test void richSerializationFailureDoesNotDiscardGuidance() {
        CountingValue value = new CountingValue(true);
        ObjectNode details = MAPPER.createObjectNode().putPOJO("value", value);
        assertDoesNotThrow(() -> Assert.alwaysGreaterThan(2, 1, "rich failure", details));
        assertEquals(1, value.calls);
        assertFalse(capture.assertionsFor("rich failure").get(0).has("details"));
        assertEquals(1, capture.guidanceFor("rich failure").size());
        assertSerializationError(capture.records().get(0), "rich failure");
    }

    private static final class CountingMap extends LinkedHashMap<String, JsonNode> {
        int traversals;

        @Override public Set<Map.Entry<String, JsonNode>> entrySet() {
            traversals++;
            return super.entrySet();
        }
    }

    private static void assertSerializationError(JsonNode record, String context) {
        JsonNode error = record.get("antithesis_error");
        assertNotNull(error, record::toString);
        assertEquals(6001, error.get("code").asInt());
        assertEquals(context, error.get("context").asText());
    }

    private static final class CountingValue implements JsonSerializable {
        int calls;
        final boolean fail;

        CountingValue(boolean fail) { this.fail = fail; }

        @Override public void serialize(JsonGenerator generator, SerializerProvider provider) throws IOException {
            calls++;
            if (fail) throw new IOException("deliberate serialization failure");
            generator.writeNumber(42);
        }

        @Override public void serializeWithType(JsonGenerator generator, SerializerProvider provider,
                                                TypeSerializer typeSerializer) throws IOException {
            serialize(generator, provider);
        }
    }
}
