package com.antithesis.sdk.internal;

import com.antithesis.sdk.Assert;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    final private ObjectNode details;
    @JsonIgnore
    final private ObjectNode guidanceDetails;

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

    public static boolean shouldEmit(final String id, final boolean condition) {
        TrackingInfo tracker = TRACKER.get(id);
        if (tracker == null) {
            tracker = TRACKER.computeIfAbsent(id, key -> new TrackingInfo(NoInfo));
        }
        return tracker.shouldEmit(condition);
    }

    public static void recordRaw(final Assert.AssertType assertType, final String displayType,
                                 final String className, final String functionName, final String fileName,
                                 final int beginLine, final int beginColumn, final String id,
                                 final boolean condition, final String message, final ObjectNode details,
                                 final boolean hit, final boolean mustHit) {
        recordRaw(assertType, displayType, className, functionName, fileName, beginLine, beginColumn, id,
                condition, message, details, hit, mustHit, null);
    }

    public static void recordRaw(final Assert.AssertType assertType, final String displayType,
                                 final String className, final String functionName, final String fileName,
                                 final int beginLine, final int beginColumn, final String id,
                                 final boolean condition, final String message, final ObjectNode details,
                                 final boolean hit, final boolean mustHit,
                                 final ObjectNode guidanceDetails) {
        TrackingInfo tracker = TRACKER.get(id);
        LocationInfo location = null;
        if (tracker == null) {
            location = new LocationInfo(className, functionName, fileName, beginLine, beginColumn);
            TrackingInfo candidate = new TrackingInfo(location);
            TrackingInfo existing = TRACKER.putIfAbsent(id, candidate);
            tracker = existing == null ? candidate : existing;
        }
        if (hit && !tracker.shouldEmit(condition)) {
            return;
        }
        // Raw calls supply their own emission location; the tracker retains
        // the first registration's location for ordinary assertions.
        if (location == null) {
            location = new LocationInfo(className, functionName, fileName, beginLine, beginColumn);
        }
        Assertion.builder().assertType(assertType).displayType(displayType)
                .location(location).id(id).condition(condition).message(message)
                .details(details).hit(hit).mustHit(mustHit)
                .guidanceDetails(guidanceDetails).build().emit();
    }

    public void emit() {
        ObjectNode assertionNode = MAPPER.createObjectNode();
        ObjectNode payload = MAPPER.valueToTree(this);
        assertionNode.set("antithesis_assert", payload);
        ObjectNode emittedDetails = hit ? details : null;
        if (hit && guidanceDetails != null) {
            if (details == null) {
                emittedDetails = guidanceDetails;
            } else {
                // Merge only when emitting; retain caller ownership and let
                // generated guidance fields override user-supplied fields.
                emittedDetails = MAPPER.createObjectNode();
                emittedDetails.setAll(details);
                emittedDetails.setAll(guidanceDetails);
            }
        }
        Internal.dispatchWithDetails(assertionNode, payload, "details", emittedDetails, id, false);
    }

    private static class TrackingInfo {
        private final AtomicInteger passCount = new AtomicInteger();
        private final AtomicInteger failCount = new AtomicInteger();
        private final LocationInfo locInfo;

        public TrackingInfo(final LocationInfo locInfo) {
            this.locInfo = locInfo;
        }

        boolean shouldEmit(boolean condition) {
            return (condition ? passCount : failCount).incrementAndGet() == 1;
        }

        protected LocationInfo getLocationInfo() {
            return this.locInfo;
        }
    }

}
