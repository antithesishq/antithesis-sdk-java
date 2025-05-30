package com.antithesis.sdk.internal;

import com.antithesis.ffi.internal.FfiHandler;
import com.antithesis.ffi.internal.OutputHandler;
import com.antithesis.ffi.internal.FfiWrapperJNI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    private static synchronized OutputHandler getInternal() {
        if (HANDLER_INSTANCE == null) {
            HANDLER_INSTANCE =
                FfiHandler.get().orElseGet(() ->
                        LocalHandler.get().orElseGet(() ->
                                NoOpHandler.get().orElseThrow(RuntimeException::new))
                );
            Internal.dispatchVersionInfo();
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
            try (FileWriter writer = new FileWriter(outFile, true)) {
                writer.write(value);
                writer.write("\n");
                writer.flush();
            } catch (IOException ignored) {
            }
        }

        @Override
        public long random() {
            return new Random().nextLong();
        }

    }

}
