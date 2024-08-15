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

final class Assertion {

    private static final LocationInfo NoInfo = new LocationInfo(
      "class", "function", "file", 0, 0);

    private static class TrackingInfo {
        int passCount = 0;
        int failCount = 0;
        LocationInfo locInfo;

        public TrackingInfo(LocationInfo locInfo) {
            this.locInfo = locInfo;
        }

        protected void trackPass() {
            this.passCount++;
        }

        protected void trackFail() {
            this.failCount++;
        }

        protected void setLocationInfo(LocationInfo locInfo) {
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

    private enum AssertType {
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

    // ----------------------------------------------------------------------
    // Used for normal runtime calls
    // ----------------------------------------------------------------------
    Assertion(final String assertType, String displayType, final boolean condition, final String message, final ObjectNode details, final boolean hit, boolean mustHit) {

        // LocationInfo is only available through previously seen rawAssert()
        // invocations which use the alternate Assertion instance constructor
        // shown below.  There is no attempt to derive LocationInfo programatically
        // at runtime.

        AssertType userAssertType;

        if (assertType.equals("always")) {
            userAssertType = Assertion.AssertType.Always;
        } else if (assertType.equals("sometimes")) {
            userAssertType = Assertion.AssertType.Sometimes;
        } else if (assertType.equals("reachability")) {
            userAssertType = Assertion.AssertType.Reachability;
        } else {
            userAssertType = Assertion.AssertType.Unknown;
        }

        this.assertType = userAssertType;
        this.displayType = displayType;
        this.location = getLocationInfo(message);
        this.id = message;
        this.condition = condition;
        this.message = message;
        this.details = details;
        this.hit = hit;
        this.mustHit = mustHit;

        this.trackEntry();
    }

    // ----------------------------------------------------------------------
    // Used for registration and DIY'ers
    // ----------------------------------------------------------------------
    Assertion(
        String assertType,
        String displayType,
        String className,
        String functionName,
        String fileName,
        int begin_line,
        int begin_column,
        String id,
        boolean condition,
        String message, 
        ObjectNode details, // TODO: something in Java 8 that is better?
        boolean hit,
        boolean mustHit
    ) {
        AssertType userAssertType;

        if (assertType.equals("always")) {
            userAssertType = Assertion.AssertType.Always;
        } else if (assertType.equals("sometimes")) {
            userAssertType = Assertion.AssertType.Sometimes;
        } else if (assertType.equals("reachability")) {
            userAssertType = Assertion.AssertType.Reachability;
        } else {
            userAssertType = Assertion.AssertType.Unknown;
        }

        this.assertType = userAssertType;
        this.displayType = displayType;
        this.location = new LocationInfo(className, functionName, fileName, begin_line, begin_column);
        this.id = id;
        this.message = message;
        this.details = details;
        this.condition = condition;
        this.hit = hit;
        this.mustHit = mustHit;

        this.trackEntry();
    }

    private LocationInfo getLocationInfo(String id) {
        TrackingInfo maybeTrackingInfo = TRACKER.get(id);
        if (maybeTrackingInfo == null) {
            return NoInfo;
        }
        return maybeTrackingInfo.getLocationInfo();
    }

    private void trackEntry() {
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


