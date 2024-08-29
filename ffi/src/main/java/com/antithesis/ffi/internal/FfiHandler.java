package com.antithesis.ffi.internal;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Logger;

public class FfiHandler implements OutputHandler {

    public static Optional<OutputHandler> get() {
        try {
            FfiWrapperJNI.loadLibrary();
            return Optional.of(new FfiHandler());
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    @Override
    public void output(final String value) {
        FfiWrapperJNI.fuzz_json_data(value, value.length());
        FfiWrapperJNI.fuzz_flush();
    }

    @Override
    public long random() {
        return FfiWrapperJNI.fuzz_get_random();
    }

}
