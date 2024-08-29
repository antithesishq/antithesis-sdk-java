package com.antithesis.sdk.internal;

import com.antithesis.ffi.internal.FfiWrapperJNI;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class InstrumentationTest {
    @Test
    void testRandom() {
        assumeTrue(FfiWrapperJNI.LOAD_LIBRARY_MARKER);
        FfiWrapperJNI.fuzz_get_random();
    }

    @Test
    void testJsonData() {
        assumeTrue(FfiWrapperJNI.LOAD_LIBRARY_MARKER);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = mapper.createObjectNode();
        jsonNode.put("hello", "world");

        String theString = "";
        try {
            theString = mapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }
        FfiWrapperJNI.fuzz_json_data(theString, theString.length());
    }

    @Test
    void testFlush() {
        assumeTrue(FfiWrapperJNI.LOAD_LIBRARY_MARKER);
        FfiWrapperJNI.fuzz_flush();
    }
}
