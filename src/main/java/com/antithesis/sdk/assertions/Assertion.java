package com.antithesis.sdk.assertions;

import com.antithesis.sdk.internal.Internal;
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

@lombok.Builder @lombok.AllArgsConstructor
final class Assertion {

    private static final LocationInfo NoInfo = new LocationInfo(
      "class", "function", "file", 0, 0);

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

        protected void setLocationInfo(final LocationInfo locInfo) {
            this.locInfo = locInfo;
        }

        protected LocationInfo getLocationInfo() {
            return this.locInfo;
        }
    }

    private static final Map<String, TrackingInfo> TRACKER = new ConcurrentHashMap<>();
    private static final ObjectMapper MAPPER;

    static {
        class LowercaseEnumSerializer extends JsonSerializer<AssertType> {
            @Override
            public void serialize(AssertType value, JsonGenerator jsonGen, SerializerProvider provider) throws IOException {
                jsonGen.writeString(value.name().toLowerCase());
            }
        }

        ObjectMapper mapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addSerializer(AssertType.class, new LowercaseEnumSerializer());
        mapper.registerModule(module);

        MAPPER = mapper;
    }

    public enum AssertType {
        Always, Sometimes, Reachability, Unknown
    }

    @JsonProperty("assert_type")
    final private AssertType assertType;

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

        TRACKER.compute(this.id, (key, value) -> {
            if (key != null && value != null) {
                // Record the condition in the associated TrackingInfo entry,
                if (this.condition) {
                    value.trackPass();
                } else {
                    value.trackFail();
                }
                return value;
            } else {
                // Establish TrackingInfo for this trackingKey when needed
                return new TrackingInfo(this.location);
            }
        });

        // Emit the assertion when first seeing a condition
        TrackingInfo trackingInfo = TRACKER.get(this.id);
        if (trackingInfo.failCount == 1 || trackingInfo.passCount == 1) {
            emit();
        }
    }

    private void emit() {
        Internal.dispatchOutput(MAPPER.valueToTree(this));
    }

}


