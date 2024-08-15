package com.antithesis.sdk.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

final public class Internal {

    public static long dispatchRandom() {
        return HandlerFactory.get().random();
    }

    public static void dispatchOutput(final ObjectNode s) {
        try {
            // TODO (@shomik) verify jsonL format
            // TODO (@shomik) Explicitly handle NoClassDefFoundError to facilitate using Jackson as a compileOnly dep and providing a concrete implementation at jar runtime
            String jsonStr = new ObjectMapper().writeValueAsString(s);

            HandlerFactory.get().output(jsonStr);
        } catch (IOException e) {
            // TODO (@shomik) logging
        }
    }

    public static long dispatchInitializeModuleCoverage(long edgeCount, String symbolFilePath) {
        return HandlerFactory.get().initializeModuleCoverage(edgeCount, symbolFilePath);
    }

    public static void dispatchNotifyModuleEdge(long edgePlusModule) {
        HandlerFactory.get().notifyModuleEdge(edgePlusModule);
    }
}
