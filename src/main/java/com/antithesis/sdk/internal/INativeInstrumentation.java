package com.antithesis.sdk.internal;
// [WAS] package com.antithesis.instrumentation;

/**
 * JNA will create bindings to libvoidstar.so based on
 * these declarations.
 */
public interface INativeInstrumentation {
    long fuzz_get_random();
    void fuzz_json_data( byte[] message, long length );
    void fuzz_flush();
    long init_coverage_module(long edgeCount, byte[] symbolFilePath);
    void notify_coverage(long edgePlusModule);
}
