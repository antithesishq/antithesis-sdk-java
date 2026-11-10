package com.antithesis.sdk.internal;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.antithesis.sdk.Assert.GuidanceType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

@lombok.Builder
@lombok.AllArgsConstructor
public final class Guidance {
    private static final Map<String, NumericTrackingInfo> NUMERIC_TRACKERS = new ConcurrentHashMap<>();
    private static final ObjectMapper MAPPER;

    static {
        class LowercaseEnumSerializer extends JsonSerializer<GuidanceType> {
            @Override
            public void serialize(GuidanceType value, JsonGenerator jsonGen, SerializerProvider provider) throws IOException {
                jsonGen.writeString(value.name().toLowerCase());
            }
        }

        ObjectMapper mapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addSerializer(GuidanceType.class, new LowercaseEnumSerializer());
        mapper.registerModule(module);

        MAPPER = mapper;
    }

    @JsonProperty("guidance_type")
    final private GuidanceType guidanceType;
    @JsonProperty("id")
    final private String id;
    @JsonProperty("message")
    final private String message;
    @JsonProperty("location")
    final private LocationInfo location;
    @JsonProperty("maximize")
    final private boolean maximize;
    @JsonIgnore
    final private ObjectNode data;
    @JsonProperty("hit")
    final private boolean hit;

    // Visible for testing: clears per-guidance tracking so tests start fresh.
    static void resetTracking() {
        NUMERIC_TRACKERS.clear();
    }

    public static boolean shouldEmit(GuidanceType type, ObjectNode data, boolean maximize, String id, boolean hit) {
        if (!hit || type != GuidanceType.Numeric) {
            return true;
        }
        JsonNode left = data == null ? null : data.get("left");
        JsonNode right = data == null ? null : data.get("right");
        return left == null ||
            right == null ||
            !left.isNumber() ||
            !right.isNumber() ||
            shouldEmitNumeric(left.doubleValue(), right.doubleValue(), maximize, id);
    }

    public static boolean shouldEmitNumeric(double left, double right, boolean maximize, String id) {
        if (!Double.isFinite(left) || !Double.isFinite(right)) {
            Internal.reportNonFiniteOperand(id);
            return false;
        }
        NumericTrackingInfo tracker = NUMERIC_TRACKERS.get(id);
        if (tracker == null) {
            tracker = NUMERIC_TRACKERS.computeIfAbsent(id, k -> new NumericTrackingInfo(maximize));
        }
        return tracker.shouldSend(left - right);
    }

    public void emit() {
        ObjectNode guidanceNode = MAPPER.createObjectNode();
        ObjectNode metadata = MAPPER.valueToTree(this);
        guidanceNode.set("antithesis_guidance", metadata);

        Internal.dispatchGuidance(guidanceNode, metadata, hit ? data : null, id);
    }

    private static class NumericTrackingInfo {
        // Finite operands cannot produce a NaN gap, so NaN bits stand in for unset.
        private static final long UNSET = Double.doubleToRawLongBits(Double.NaN);

        private final AtomicLong markBits;
        private final boolean maximize;

        NumericTrackingInfo(boolean maximize) {
            this.maximize = maximize;
            this.markBits = new AtomicLong(UNSET);
        }

        boolean shouldSend(double value) {
            while (true) {
                long currentBits = markBits.get();
                if (currentBits != UNSET) {
                    double current = Double.longBitsToDouble(currentBits);
                    if (maximize ? (current >= value) : (current <= value)) {
                        return false;
                    }
                }
                if (markBits.compareAndSet(currentBits, Double.doubleToRawLongBits(value))) {
                    return true;
                }
            }
        }
    }
}
