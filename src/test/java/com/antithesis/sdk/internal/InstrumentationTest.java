package com.antithesis.sdk.internal;

import org.junit.jupiter.api.Test;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class InstrumentationTest {
    @Test
    void testRandom() {
        NativeInstrumentationFactory.get().fuzz_get_random();
    }

    @Test
    void testJsonData() {
        HashMap<String, String> map = new HashMap<>();
        map.put("hello", "world");
        String json_string = new JSONObject(map).toJSONString();

        byte[] decoded = json_string.getBytes(StandardCharsets.UTF_8);
        byte[] cString = new byte[decoded.length + 1];
        // Null terminated C string
        System.arraycopy(decoded, 0, cString, 0, decoded.length);

        NativeInstrumentationFactory.get().fuzz_json_data(cString, json_string.length());
    }

    @Test
    void testFlush() {
        NativeInstrumentationFactory.get().fuzz_flush();
    }
}
