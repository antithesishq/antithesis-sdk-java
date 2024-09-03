package com.antithesis.sdk.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

final public class Internal {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SDK_VERSION = loadSDKVersion();

    private static final String PROTOCOL_VERSION = "1.0.0";

    private static String loadSDKVersion() {
        try {
            Enumeration<URL> manifests = Internal.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while(manifests.hasMoreElements()) {
                Manifest manifest = new Manifest(manifests.nextElement().openStream());
                Attributes attributes = manifest.getMainAttributes();

                Attributes.Name aName = new Attributes.Name("Implementation-Title");
                Attributes.Name aVersion = new Attributes.Name("Implementation-Version");

                if(attributes.containsKey(aName)) {
                    // Antithesis FFI and SDK are guaranteed to share the same version
                    if(attributes.getValue(aName).equals("Antithesis FFI for Java")) {
                        if (attributes.containsKey(aVersion)) {
                            return attributes.getValue(aVersion);
                        }
                    }
                }
            }
        } catch (Exception e) {
            return "0.0.0";
        }
        return "0.0.0";
    }

    public static void dispatchVersionInfo() {
        ObjectNode antithesisLanguageInfo = MAPPER.createObjectNode();
        antithesisLanguageInfo.put("name", "Java");
        antithesisLanguageInfo.put("version", System.getProperty("java.version"));

        ObjectNode antithesisVersionInfo = MAPPER.createObjectNode();
        antithesisVersionInfo.put("language", antithesisLanguageInfo);
        antithesisVersionInfo.put("sdk_version", SDK_VERSION);
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
