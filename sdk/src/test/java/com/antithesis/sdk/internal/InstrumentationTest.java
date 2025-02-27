package com.antithesis.sdk.internal;

import com.antithesis.ffi.internal.FfiWrapperJNI;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;

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
        byte[] utf8Bytes = theString.getBytes(StandardCharsets.UTF_8);
        FfiWrapperJNI.fuzz_json_data(utf8Bytes, utf8Bytes.length);
    }

    @Test
    void testFlush() {
        assumeTrue(FfiWrapperJNI.LOAD_LIBRARY_MARKER);
        FfiWrapperJNI.fuzz_flush();
    }
}
