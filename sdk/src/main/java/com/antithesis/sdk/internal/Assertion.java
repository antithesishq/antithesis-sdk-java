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

    public void trackEntry() {
        // Requirement: Catalog entries must always will emit()
        if (!this.hit) {
            if (!TRACKER.containsKey(this.id)) {
                TRACKER.put(this.id, new TrackingInfo(this.location));
            }
            this.emit();
            return;
        }

        TrackingInfo trackingInfo = TRACKER.compute(this.id, (key, value) -> {
            if (value == null) {
                // Establish TrackingInfo for this trackingKey when needed
                value = new TrackingInfo(this.location);
            }
            // Record the condition in the associated TrackingInfo entry,
            if (this.condition) {
                value.trackPass();
            } else {
                value.trackFail();
            }
            return value;
        });

        if (this.condition) {
            if (trackingInfo.passCount == 1) {
                emit();
            }
        } else {
            if (trackingInfo.failCount == 1) {
                emit();
            }
        }
        return;
    }

    private void emit() {
        ObjectNode assertionNode = MAPPER.createObjectNode();
        assertionNode.put("antithesis_assert", MAPPER.valueToTree(this));

        Internal.dispatchOutput(assertionNode);
    }

    private static class TrackingInfo {
        int passCount = 0;
        int failCount = 0;
        LocationInfo locInfo;

        public TrackingInfo(final LocationInfo locInfo) {
            this.locInfo = locInfo;
        }

        protected void trackPass() {
            this.passCount++;
        }

        protected void trackFail() {
            this.failCount++;
        }

        protected LocationInfo getLocationInfo() {
            return this.locInfo;
        }

        protected void setLocationInfo(final LocationInfo locInfo) {
            this.locInfo = locInfo;
        }
    }

}


