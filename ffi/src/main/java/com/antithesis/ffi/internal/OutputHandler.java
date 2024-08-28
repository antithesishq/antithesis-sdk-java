package com.antithesis.ffi.internal;

public interface OutputHandler {
    void output(String value);

    long random();

    long initializeModuleCoverage(long edgeCount, String symbolFilePath);

    void notifyModuleEdge(long edgePlusModule);
}

