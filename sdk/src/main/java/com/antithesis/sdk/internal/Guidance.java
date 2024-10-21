package com.antithesis.sdk.internal;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.antithesis.sdk.Assert.GuidanceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
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
    @JsonProperty("guidance_data")
    final private ObjectNode data;
    @JsonProperty("hit")
    final private boolean hit;

    public void trackEntry() {
        if (!this.hit) {
            this.emit();
            return;
        }

        if (this.guidanceType == GuidanceType.Numeric) {
            NumericTrackingInfo tracker = NUMERIC_TRACKERS.computeIfAbsent(this.id, k -> new NumericTrackingInfo(this.maximize));
            double left = data.get("left").doubleValue();
            double right = data.get("right").doubleValue();
            if (tracker.shouldSend(left - right)) {
                this.emit();
            }
        } else {
            this.emit();
        }
    }

    private void emit() {
        ObjectNode guidacnNode = MAPPER.createObjectNode();
        guidacnNode.put("antithesis_guidance", MAPPER.valueToTree(this));

        Internal.dispatchOutput(guidacnNode);
    }

    private static class NumericTrackingInfo {
        double mark = Double.NEGATIVE_INFINITY;
        boolean maximize;

        NumericTrackingInfo(boolean maximize) {
            this.maximize = maximize;
            this.mark = maximize ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }

        boolean shouldSend(double value) {
            if (maximize ? (mark > value) : (mark < value)) return false;
            this.mark = value;
            return true;
        }
    }
}
