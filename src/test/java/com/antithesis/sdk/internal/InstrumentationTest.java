package com.antithesis.sdk.internal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;



public class InstrumentationTest {
    @Test
    void testRandom() {
        assumeTrue(VoidstarWrapperJNI.hasNativeLibrary());
        try {
             VoidstarWrapperJNI.loadLibrary();
        } catch (Throwable e) {
            fail("Unable to load native library");
        }
        VoidstarWrapperJNI.fuzz_get_random();
    }

    @Test
    void testJsonData() {
        assumeTrue(VoidstarWrapperJNI.hasNativeLibrary());

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = mapper.createObjectNode();
        jsonNode.put("hello", "world");

        try {
             VoidstarWrapperJNI.loadLibrary();
        } catch (Throwable e) {
            fail("Unable to load native library");
        } 
        String theString = "";
        try {
            theString = mapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }
        VoidstarWrapperJNI.fuzz_json_data(theString, theString.length());
    }

    @Test
    void testFlush() {
        assumeTrue(VoidstarWrapperJNI.hasNativeLibrary());
        try {
             VoidstarWrapperJNI.loadLibrary();
        } catch (Throwable e) {
            fail("Unable to load native library");
        } 
        VoidstarWrapperJNI.fuzz_flush();
    }
}
