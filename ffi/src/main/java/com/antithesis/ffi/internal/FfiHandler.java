package com.antithesis.ffi.internal;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class FfiHandler implements OutputHandler {

    private static long offset = -1;

    public static Optional<OutputHandler> get() {
        if (FfiWrapperJNI.LOAD_LIBRARY_MARKER) {
            return Optional.of(new FfiHandler());
        }
        return Optional.empty();
    }

    public static long initializeModuleCoverage(long edgeCount, String symbolFilePath) {
        if (offset != -1) {
            // A Java application may not contain multiple "modules" in Antithesis terms.
            throw new IllegalStateException("Antithesis Java instrumentation has already been initialized.");
        }
        if (edgeCount > Integer.MAX_VALUE || edgeCount < 1) {
            throw new IllegalArgumentException("Antithesis Java instrumentation supports [1 ," + Integer.MAX_VALUE + "] edges");
        }
        offset = FfiWrapperJNI.init_coverage_module(edgeCount, symbolFilePath);
        String msg = String.format("Initialized Java module at offset 0x%016x with %d edges; symbol file %s", offset, edgeCount, symbolFilePath);
        System.err.println(msg);
        return offset;
    }

    public static void notifyModuleEdge(long edgePlusModule) {
        FfiWrapperJNI.notify_coverage(edgePlusModule);
    }

    @Override
    public void output(final String value) {
        // `fuzz_json_data` expects length in UTF-8 encoded bytes.
        byte[] utf8Bytes = value.getBytes(StandardCharsets.UTF_8);
        FfiWrapperJNI.fuzz_json_data(utf8Bytes, utf8Bytes.length);
        FfiWrapperJNI.fuzz_flush();
    }

    @Override
    public long random() {
        return FfiWrapperJNI.fuzz_get_random();
    }

}
