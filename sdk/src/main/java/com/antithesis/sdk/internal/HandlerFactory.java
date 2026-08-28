package com.antithesis.sdk.internal;

import com.antithesis.ffi.internal.FfiHandler;
import com.antithesis.ffi.internal.OutputHandler;
import com.antithesis.ffi.internal.FfiWrapperJNI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;

public class HandlerFactory {

    private final static boolean CATALOG_SENT = didLoadCatalog();
    // Will be initialized through the static 'HandlerFactory.get()' function
    private static volatile OutputHandler HANDLER_INSTANCE;

    private static boolean didLoadCatalog() {
        String className = "com.antithesis.sdk.generated.AssertionCatalog";
        Class theClass = null;
        try {
            theClass = Class.forName(className);
            if(FfiWrapperJNI.LOAD_LIBRARY_MARKER) {
                ClassLoader currentClassloader = HandlerFactory.class.getClassLoader();
                System.err.println(currentClassloader);
            }
        } catch (Throwable e) {
            if(FfiWrapperJNI.LOAD_LIBRARY_MARKER) {
                e.printStackTrace();
            }
        }
        return theClass != null;
    }

    public static OutputHandler get() {
        if (HANDLER_INSTANCE == null) {
            HANDLER_INSTANCE = getInternal();
        }
        return HANDLER_INSTANCE;
    }

    // Visible for testing: install a specific output handler (e.g. an in-memory
    // capture) so tests can observe what the SDK emits.
    static void useHandler(final OutputHandler handler) {
        HANDLER_INSTANCE = handler;
    }

    private static synchronized OutputHandler getInternal() {
        if (HANDLER_INSTANCE == null) {
            OutputHandler handler =
                FfiHandler.get().orElseGet(() ->
                        LocalHandler.get().orElseGet(() ->
                                NoOpHandler.get().orElseThrow(RuntimeException::new))
                );
            // Emit the version header through the handler directly and only
            // then publish it: publishing first would let another thread's
            // event overtake the header through the unsynchronized fast path
            // in get().
            Internal.dispatchVersionInfo(handler);
            HANDLER_INSTANCE = handler;
        }
        return HANDLER_INSTANCE;
    }

    private static class NoOpHandler implements OutputHandler {
        public static Optional<OutputHandler> get() {
            return Optional.of(new NoOpHandler());
        }

        @Override
        public void output(final String value) {
        }

        @Override
        public long random() {
            return new Random().nextLong();
        }

    }

    private static class LocalHandler implements OutputHandler {
        private static final String LOCAL_OUTPUT_ENV_VAR = "ANTITHESIS_SDK_LOCAL_OUTPUT";
        private final File outFile;

        private LocalHandler(final String fileName) {
            this.outFile = new File(fileName);

            String fullPath;
            try {
                fullPath = this.outFile.getAbsolutePath();
            } catch (Throwable e) {
                System.err.printf("Unable to getAbsolutePath() for '%s'\n", this.outFile.toString());
                System.err.println(e);
                fullPath = fileName;
            }
            System.err.printf("Assertion output will be sent to: \"%s\"\n", fullPath);
        }

        public static Optional<OutputHandler> get() {
            String fileName = System.getenv(LOCAL_OUTPUT_ENV_VAR);
            if (fileName != null && !fileName.isEmpty()) {
                return Optional.of(new LocalHandler(fileName));
            }
            return Optional.empty();
        }

        @Override
        public void output(final String value) {
            // Using a byte[] and a FileOutputStream to ensure that each write is dispatched as a single syscall
            final byte[] line = (value + "\n").getBytes(StandardCharsets.UTF_8);
            try (FileOutputStream out = new FileOutputStream(outFile, true)) {
                out.write(line);
            } catch (IOException ignored) {
            }
        }

        @Override
        public long random() {
            return new Random().nextLong();
        }

    }

}
