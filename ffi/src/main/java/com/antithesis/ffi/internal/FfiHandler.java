package com.antithesis.ffi.internal;

import java.util.Optional;

public class FfiHandler implements OutputHandler, CoverageHandler {

    private static long offset = -1;

    public static Optional<OutputHandler> get() {
        try {
            if(FfiWrapperJNI.LOAD_LIBRARY_MARKER) {
                return Optional.of(new FfiHandler());
            }
            return Optional.empty();
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
