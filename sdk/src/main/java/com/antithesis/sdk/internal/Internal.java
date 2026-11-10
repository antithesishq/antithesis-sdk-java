package com.antithesis.sdk.internal;

import com.antithesis.ffi.internal.OutputHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

final public class Internal {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String[] SDK_VERSION_INFO = loadSDKVersion();
    // Integers of this magnitude or more are not exact as doubles
    // (JavaScript's Number.MAX_SAFE_INTEGER + 1).
    private static final long SAFE_INTEGER_BOUND = 1L << 53;
    private static final BigInteger SAFE_INTEGER_BOUND_BIG = BigInteger.valueOf(SAFE_INTEGER_BOUND);
    // Findings of InspectingNode, one bit per diagnostic code.
    private static final int NON_FINITE = 1;
    private static final int BEYOND_SAFE_INTEGER = 2;
    private static final AtomicBoolean nonFiniteReported = new AtomicBoolean();
    private static final AtomicBoolean unsafeIntegerReported = new AtomicBoolean();
    private static final AtomicBoolean nonFiniteOperandReported = new AtomicBoolean();

    static void resetDiagnostics() {
        nonFiniteReported.set(false);
        unsafeIntegerReported.set(false);
        nonFiniteOperandReported.set(false);
    }

    private static String[] loadSDKVersion() {
        String sdkVersion = "0.0.0";
        String protocolVersion = "0.0.0";
        String[] info = {sdkVersion, protocolVersion};

        try {
            Enumeration<URL> manifests = Internal.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (manifests.hasMoreElements()) {
                Manifest manifest = new Manifest(manifests.nextElement().openStream());
                Attributes attributes = manifest.getMainAttributes();

                Attributes.Name aTitle = new Attributes.Name("Implementation-Title");
                Attributes.Name aVersion = new Attributes.Name("Implementation-Version");
                Attributes.Name aProtocol = new Attributes.Name("Specification-Version");

                if (attributes.containsKey(aTitle)) {
                    String jarTitle = attributes.getValue(aTitle);
                    // Antithesis FFI and SDK are guaranteed to share the same version
                    boolean isFFIManifest = jarTitle.equals("Antithesis FFI for Java");
                    if (isFFIManifest) {
                        if (attributes.containsKey(aVersion)) {
                            sdkVersion = attributes.getValue(aVersion);
                        }
                        if (attributes.containsKey(aProtocol)) {
                            protocolVersion = attributes.getValue(aProtocol);
                        }
                        break;  // no sense in looking any further
                    }
                }
            }
        } catch (Exception e) {
            return info;
        }
        info[0] = sdkVersion;
        info[1] = protocolVersion;
        return info;
    }

    // Writes the version header through the given handler directly, NOT
    // through HandlerFactory.get(): this runs while the handler is being
    // initialized, before it is published, so the header is guaranteed to
    // precede every event (and the call cannot recurse into the factory).
    public static void dispatchVersionInfo(final OutputHandler handler) {
        ObjectNode antithesisLanguageInfo = MAPPER.createObjectNode();
        antithesisLanguageInfo.put("name", "Java");
        antithesisLanguageInfo.put("version", System.getProperty("java.version"));

        ObjectNode antithesisVersionInfo = MAPPER.createObjectNode();
        antithesisVersionInfo.put("language", antithesisLanguageInfo);
        antithesisVersionInfo.put("sdk_version", SDK_VERSION_INFO[0]);
        antithesisVersionInfo.put("protocol_version", SDK_VERSION_INFO[1]);

        ObjectNode antithesisSDKInfo = MAPPER.createObjectNode();
        antithesisSDKInfo.put("antithesis_sdk", antithesisVersionInfo);
        try {
            handler.output(MAPPER.writeValueAsString(antithesisSDKInfo));
        } catch (IOException ignored) {
        }
    }

    public static long dispatchRandom() {
        return HandlerFactory.get().random();
    }

    // Serialize once on success, inspecting the details' numbers as they are
    // written. Details can contain POJONodes or custom nodes, so a
    // serialization failure must not discard the enclosing SDK event.
    public static void dispatchWithDetails(final ObjectNode envelope, final ObjectNode container,
                                           final String field, final ObjectNode details,
                                           final String context, final boolean emptyOnFailure) {
        dispatchWithDetails(envelope, container, field, details, context, emptyOnFailure, false);
    }

    private static void dispatchWithDetails(final ObjectNode envelope, final ObjectNode container,
                                            final String field, final ObjectNode details,
                                            final String context, final boolean emptyOnFailure,
                                            final boolean dropOnFailure) {
        InspectingNode inspecting = null;
        if (details != null) {
            inspecting = new InspectingNode(details);
            container.set(field, inspecting);
        } else if (emptyOnFailure) {
            container.putObject(field);
        }
        String json;
        try {
            json = MAPPER.writeValueAsString(envelope);
        } catch (IOException | RuntimeException error) {
            if (emptyOnFailure) {
                container.putObject(field);
            } else {
                container.remove(field);
            }
            reportDetailsError(context, error);
            if (!dropOnFailure) {
                dispatchOutput(envelope);
            }
            return;
        }
        if (inspecting != null) {
            reportDetailsWarnings(context, inspecting.found);
        }
        HandlerFactory.get().output(json);
    }

    public static void dispatchGuidance(final ObjectNode envelope, final ObjectNode container,
                                        final ObjectNode data, final String context) {
        dispatchWithDetails(envelope, container, "guidance_data", data, context, false, true);
    }

    // Jackson owns traversal and custom serialization. Intercept its typed
    // writes before non-finite values or integer types lose their identity.
    private static final class InspectingNode extends ValueNode {
        private final ObjectNode details;
        int found;

        InspectingNode(final ObjectNode details) {
            this.details = details;
        }

        @Override
        public void serialize(final JsonGenerator g, final SerializerProvider provider) throws IOException {
            details.serialize(new InspectingGenerator(g), provider);
        }

        private static String nonFiniteName(final double value) {
            return Double.isNaN(value) ? "NaN" : value > 0 ? "Infinity" : "-Infinity";
        }

        private final class InspectingGenerator extends JsonGeneratorDelegate {
            InspectingGenerator(final JsonGenerator delegate) {
                // Object/tree writes and parser copies must use this wrapper,
                // rather than bypassing it through the underlying generator.
                super(delegate, false);
            }

            @Override
            public void writeNumber(final double value) throws IOException {
                if (Double.isFinite(value)) {
                    delegate.writeNumber(value);
                } else {
                    found |= NON_FINITE;
                    delegate.writeString(nonFiniteName(value));
                }
            }

            @Override
            public void writeNumber(final float value) throws IOException {
                if (Float.isFinite(value)) {
                    delegate.writeNumber(value);
                } else {
                    found |= NON_FINITE;
                    delegate.writeString(nonFiniteName(value));
                }
            }

            @Override
            public void writeNumber(final long value) throws IOException {
                if (value >= SAFE_INTEGER_BOUND || value <= -SAFE_INTEGER_BOUND) {
                    found |= BEYOND_SAFE_INTEGER;
                }
                delegate.writeNumber(value);
            }

            @Override
            public void writeNumber(final BigInteger value) throws IOException {
                if (value != null && value.abs().compareTo(SAFE_INTEGER_BOUND_BIG) >= 0) {
                    found |= BEYOND_SAFE_INTEGER;
                }
                delegate.writeNumber(value);
            }

            @Override
            public void writeNumber(final String value) throws IOException {
                // Jackson's fallback serializer for Number subclasses writes
                // their textual form, bypassing the typed overloads above.
                if ("NaN".equals(value) || "Infinity".equals(value) || "-Infinity".equals(value)) {
                    found |= NON_FINITE;
                    delegate.writeString(value);
                    return;
                }
                // Do not parse decimals as doubles: a finite decimal can exceed
                // double's range, and integer digits must remain exact.
                if (value != null && value.matches("-?[0-9]+")
                        && new BigInteger(value).abs().compareTo(SAFE_INTEGER_BOUND_BIG) >= 0) {
                    found |= BEYOND_SAFE_INTEGER;
                }
                delegate.writeNumber(value);
            }

            @Override
            public void writeNumber(final char[] value, final int offset, final int length) throws IOException {
                writeNumber(new String(value, offset, length));
            }

            // JsonGeneratorDelegate forwards bulk writes directly; expand the
            // numeric arrays so their elements receive the scalar checks too.
            @Override
            public void writeArray(final long[] values, final int offset, final int length) throws IOException {
                _verifyOffsets(values.length, offset, length);
                writeStartArray(values, length);
                for (int i = offset, end = offset + length; i < end; i++) {
                    writeNumber(values[i]);
                }
                writeEndArray();
            }

            @Override
            public void writeArray(final double[] values, final int offset, final int length) throws IOException {
                _verifyOffsets(values.length, offset, length);
                writeStartArray(values, length);
                for (int i = offset, end = offset + length; i < end; i++) {
                    writeNumber(values[i]);
                }
                writeEndArray();
            }
        }

        @Override
        public JsonToken asToken() {
            return JsonToken.START_OBJECT;
        }

        @Override
        public JsonNodeType getNodeType() {
            return JsonNodeType.OBJECT;
        }

        @Override
        public String asText() {
            return details.asText();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends JsonNode> T deepCopy() {
            return (T) new InspectingNode(details.deepCopy());
        }

        @Override
        public boolean equals(final Object other) {
            return other == this;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    private static void reportDetailsWarnings(final String context, final int found) {
        // Read before the CAS: once a warning is out, every later emission
        // that would repeat it must not pay a failing lock cmpxchg.
        if ((found & NON_FINITE) != 0 && !nonFiniteReported.get() && nonFiniteReported.compareAndSet(false, true)) {
            reportDiagnostic("antithesis_warning", 6002, context, "Java SDK replaced non-finite numbers in details for \""
                    + context + "\" with the strings \"NaN\", \"Infinity\" or \"-Infinity\""
                    + "; later occurrences in this process are not reported");
        }
        if ((found & BEYOND_SAFE_INTEGER) != 0 && !unsafeIntegerReported.get()
                && unsafeIntegerReported.compareAndSet(false, true)) {
            reportDiagnostic("antithesis_warning", 6003, context, "Java SDK emitted integers beyond \u00b12^53 in details for \""
                    + context + "\"; consumers that parse JSON numbers as doubles will round them"
                    + "; later occurrences in this process are not reported");
        }
    }

    static void reportNonFiniteOperand(final String id) {
        if (!nonFiniteOperandReported.get() && nonFiniteOperandReported.compareAndSet(false, true)) {
            reportDiagnostic("antithesis_warning", 6004, id, "Java SDK skipped numeric guidance for \"" + id
                    + "\" because an operand was not finite"
                    + "; later occurrences in this process are not reported");
        }
    }

    private static void reportDetailsError(final String context, final Exception error) {
        reportDiagnostic("antithesis_error", 6001, context,
                "Java SDK failed to serialize details for \"" + context + "\": " + error);
    }

    private static void reportDiagnostic(final String key, final int code, final String context, final String message) {
        ObjectNode diagnostic = MAPPER.createObjectNode();
        ObjectNode body = diagnostic.putObject(key);
        body.put("code", code);
        body.put("message", message);
        body.put("context", context);
        dispatchOutput(diagnostic);
    }

    public static void dispatchOutput(final ObjectNode s) {
        try {
            String jsonStr = MAPPER.writeValueAsString(s);
            HandlerFactory.get().output(jsonStr);
        } catch (IOException ignored) {
        }
    }

}
