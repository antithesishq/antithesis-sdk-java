package com.antithesis.sdk.internal;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Logger;

public class VoidstarHandler implements OutputHandler {

    private static INativeInstrumentation NATIVE_VOIDSTAR;

    // JNA already uses the archaic java.util.logging library, so we can use it as well, in order
    // not to depend on another logging framework.
    private static final Logger logger = Logger.getLogger(VoidstarHandler.class.getName());

    private static long offset = -1;

    private VoidstarHandler(final INativeInstrumentation instrumentation) {
        NATIVE_VOIDSTAR = instrumentation;
    }
    public static Optional<OutputHandler> get() {
        try {
            return Optional.of(new VoidstarHandler(NativeInstrumentationFactory.get()));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    @Override
    public void output(final String value) {
        byte[] decoded = value.getBytes(StandardCharsets.UTF_8);
        byte[] cString = new byte[decoded.length + 1];
        // Null terminated C string
        System.arraycopy(decoded, 0, cString, 0, decoded.length);

        NATIVE_VOIDSTAR.fuzz_json_data(cString, value.length());
        NATIVE_VOIDSTAR.fuzz_flush();
    }

    @Override
    public long random() {
        return NATIVE_VOIDSTAR.fuzz_get_random();
    }

    @Override
    public long initializeModuleCoverage(long edgeCount, String symbolFilePath) {
        if (offset != -1) {
            // A Java application may not contain multiple "modules" in Antithesis terms.
            throw new IllegalStateException("Antithesis Java instrumentation has already been initialized.");
        }
        if (edgeCount > Integer.MAX_VALUE || edgeCount < 1) {
            throw new IllegalArgumentException("Antithesis Java instrumentation supports [1 ," + Integer.MAX_VALUE + "] edges");
        }
        offset = NATIVE_VOIDSTAR.init_coverage_module(edgeCount, createNativeString(symbolFilePath));
        String msg = String.format("Initialized Java module at offset 0x%016x with %d edges; symbol file %s", offset, edgeCount, symbolFilePath);
        logger.info(msg);
        return offset;
    }

    @Override
    public void notifyModuleEdge(long edgePlusModule) {
        // Right now, the Java implementation defers completely to the native library. 
        // See instrumentation.h to understand the logic here. The shim (i.e. StaticModule.java)
        // is responsible for handling the return value.
        NATIVE_VOIDSTAR.notify_coverage(edgePlusModule);
    }

    static private byte[] createNativeString(String s) {
        byte[] decoded = s.getBytes(StandardCharsets.UTF_8);
        byte[] cstring = new byte[decoded.length + 1];
        // null-terminate the string...it's C!
        System.arraycopy(decoded, 0, cstring, 0, decoded.length);
        return cstring;
    }
}
