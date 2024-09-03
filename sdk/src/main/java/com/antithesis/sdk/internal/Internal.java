package com.antithesis.sdk.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

final public class Internal {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SDK_VERSION = loadSDKVersion();

    private static final String PROTOCOL_VERSION = "1.0.0";

    private static String loadSDKVersion() {
        return "1.3.1";
    }

    public static void dispatchVersionInfo() {
        ObjectNode antithesisLanguageInfo = MAPPER.createObjectNode();
        antithesisLanguageInfo.put("name", "Java");
        antithesisLanguageInfo.put("version", System.getProperty("java.version"));

        ObjectNode antithesisVersionInfo = MAPPER.createObjectNode();
        antithesisVersionInfo.put("language", antithesisLanguageInfo);
        antithesisVersionInfo.put("sdk_version", loadSDKVersion());
        antithesisVersionInfo.put("protocol_version", PROTOCOL_VERSION);

        ObjectNode antithesisSDKInfo = MAPPER.createObjectNode();
        antithesisSDKInfo.put("antithesis_sdk", antithesisVersionInfo);
        dispatchOutput(antithesisSDKInfo);
    }

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
