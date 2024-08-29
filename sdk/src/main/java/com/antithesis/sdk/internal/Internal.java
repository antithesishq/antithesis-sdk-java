package com.antithesis.sdk.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

final public class Internal {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static long dispatchRandom() {
        return HandlerFactory.get().random();
    }

    public static void dispatchOutput(final ObjectNode s) {
        try {
            String jsonStr = MAPPER.writeValueAsString(s);
            HandlerFactory.get().output(jsonStr);
        } catch (IOException ignored) {
        }
    }

}
