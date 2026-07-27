package com.antithesis.sdk.internal;

import com.antithesis.sdk.Assert;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@lombok.Builder
@lombok.AllArgsConstructor
public final class Assertion {

    private static final LocationInfo NoInfo = new LocationInfo(
            "class", "function", "file", 0, 0);
    private static final Map<String, TrackingInfo> TRACKER = new ConcurrentHashMap<>();
    private static final ObjectMapper MAPPER;

    static {
        class LowercaseEnumSerializer extends JsonSerializer<Assert.AssertType> {
            @Override
            public void serialize(Assert.AssertType value, JsonGenerator jsonGen, SerializerProvider provider) throws IOException {
                jsonGen.writeString(value.name().toLowerCase());
            }
        }

        ObjectMapper mapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addSerializer(Assert.AssertType.class, new LowercaseEnumSerializer());
        mapper.registerModule(module);

        MAPPER = mapper;
    }

    @JsonProperty("assert_type")
    final private Assert.AssertType assertType;
    @JsonProperty("display_type")
    final private String displayType;
    @JsonProperty("condition")
    final private boolean condition;
    @JsonProperty("message")
    final private String message;
    @JsonProperty("location")
    final private LocationInfo location;
    @JsonProperty("hit")
    final private boolean hit;
    @JsonProperty("must_hit")
    final private boolean mustHit;
    @JsonProperty("id")
    final private String id;
    @JsonProperty("details")
    final private ObjectNode details;

    public static LocationInfo getLocationInfo(final String id) {
        TrackingInfo maybeTrackingInfo = TRACKER.get(id);
        if (maybeTrackingInfo == null) {
            return NoInfo;
        }
        return maybeTrackingInfo.getLocationInfo();
    }

    // Visible for testing: clears per-assertion tracking so tests start fresh.
    static void resetTracking() {
        TRACKER.clear();
    }

    public void trackEntry() {
        TrackingInfo trackingInfo = TRACKER.computeIfAbsent(this.id, (key) -> {
            return new TrackingInfo(this.location);
        });

        if (!this.hit) {
            // Requirement: Catalog entries must always will emit()
            this.emit();
            return;
        }

        // Record the condition in the associated TrackingInfo entry,
        if (this.condition) {
            if (trackingInfo.trackPass() == 1) {
                emit();
            }
        } else {
            if (trackingInfo.trackFail() == 1) {
                emit();
            }
        }
        return;
    }

    private void emit() {
        ObjectNode assertionNode = MAPPER.createObjectNode();
        assertionNode.set("antithesis_assert", MAPPER.valueToTree(this));

        Internal.dispatchOutput(assertionNode);
    }

    private static class TrackingInfo {
        private final AtomicInteger passCount = new AtomicInteger();
        private final AtomicInteger failCount = new AtomicInteger();
        private final LocationInfo locInfo;

        public TrackingInfo(final LocationInfo locInfo) {
            this.locInfo = locInfo;
        }

        protected int trackPass() {
            return this.passCount.incrementAndGet();
        }

        protected int trackFail() {
            return this.failCount.incrementAndGet();
        }

        protected LocationInfo getLocationInfo() {
            return this.locInfo;
        }
    }

}
