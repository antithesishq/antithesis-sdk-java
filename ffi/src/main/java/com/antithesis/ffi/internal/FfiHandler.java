package com.antithesis.ffi.internal;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Logger;

public class FfiHandler implements OutputHandler {

    private static long offset = -1;

    public static Optional<OutputHandler> get() {
        try {
            FfiWrapperJNI.loadLibrary();
            return Optional.of(new FfiHandler());
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    @Override
    public void output(final String value) {
        FfiWrapperJNI.fuzz_json_data(value, value.length());
        FfiWrapperJNI.fuzz_flush();
    }

    @Override
    public long random() {
        return FfiWrapperJNI.fuzz_get_random();
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
        offset = FfiWrapperJNI.init_coverage_module(edgeCount, symbolFilePath);
        String msg = String.format("Initialized Java module at offset 0x%016x with %d edges; symbol file %s", offset, edgeCount, symbolFilePath);
        // TODO: (@shomik) logger.info(msg); 
        return offset;
    }

    @Override
    public void notifyModuleEdge(long edgePlusModule) {
        // Right now, the Java implementation defers completely to the native library. 
        // See instrumentation.h to understand the logic here. The shim (i.e. StaticModule.java)
        // is responsible for handling the return value.
        FfiWrapperJNI.notify_coverage(edgePlusModule);
    }

}