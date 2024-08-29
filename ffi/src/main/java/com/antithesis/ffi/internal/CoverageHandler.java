package com.antithesis.ffi.internal;

public interface CoverageHandler {

    public long initializeModuleCoverage(long edgeCount, String symbolFilePath);

    public void notifyModuleEdge(long edgePlusModule);
}
