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
    private static final String[] SDK_VERSION_INFO = loadSDKVersion();

    private static String[] loadSDKVersion() {
        String sdkVersion = "0.0.0";
        String protocolVersion = "0.0.0";
        String[] info = {sdkVersion, protocolVersion};

        try {
            Enumeration<URL> manifests = Internal.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (manifests.hasMoreElements()) {
                Manifest manifest = new Manifest(manifests.nextElement().openStream());
                Attributes attributes = manifest.getMainAttributes();

                Attributes.Name aTitle = new Attributes.Name("Implementation-Title");
                Attributes.Name aVersion = new Attributes.Name("Implementation-Version");
                Attributes.Name aProtocol = new Attributes.Name("Specification-Version");

                if (attributes.containsKey(aTitle)) {
                    String jarTitle = attributes.getValue(aTitle);
                    // Antithesis FFI and SDK are guaranteed to share the same version
                    boolean isFFIManifest = jarTitle.equals("Antithesis FFI for Java");
                    if (isFFIManifest) {
                        if (attributes.containsKey(aVersion)) {
                            sdkVersion = attributes.getValue(aVersion);
                        }
                        if (attributes.containsKey(aProtocol)) {
                            protocolVersion = attributes.getValue(aProtocol);
                        }
                        break;  // no sense in looking any further
                    }
                }
            }
        } catch (Exception e) {
            return info;
        }
        info[0] = sdkVersion;
        info[1] = protocolVersion;
        return info;
    }

    public static void dispatchVersionInfo() {
        ObjectNode antithesisLanguageInfo = MAPPER.createObjectNode();
        antithesisLanguageInfo.put("name", "Java");
        antithesisLanguageInfo.put("version", System.getProperty("java.version"));

        ObjectNode antithesisVersionInfo = MAPPER.createObjectNode();
        antithesisVersionInfo.put("language", antithesisLanguageInfo);
        antithesisVersionInfo.put("sdk_version", SDK_VERSION_INFO[0]);
        antithesisVersionInfo.put("protocol_version", SDK_VERSION_INFO[1]);

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
