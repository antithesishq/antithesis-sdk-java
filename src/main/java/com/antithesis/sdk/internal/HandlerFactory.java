package com.antithesis.sdk.internal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.Random;

public class HandlerFactory {

    // Will be initialized through the static 'HandlerFactory.get()' function
    private static OutputHandler HANDLER_INSTANCE = null;

    private final static boolean CATALOG_SENT = didLoadCatalog();

    private static boolean didLoadCatalog() {
        String className = "com.antithesis.sdk.generated.AssertionCatalog";
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            System.out.println("HandlerFactory.didLoadCatalog() could not obtain a ClassLoader");
            return false;
        }
        Class theClass = null;
        boolean shouldInitialize = true;
        try {
            theClass = Class.forName(className, shouldInitialize, classLoader);
        } catch (Throwable e) {
            System.out.printf("HandlerFactory.didLoadCatalog() unable to load using Class.forName(\"%s\", %b, classLoader)\n", className, shouldInitialize);
            System.out.printf("Caught: %s\n", e.toString());
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
                    VoidstarHandler.get().orElseGet(() ->
                            LocalHandler.get().orElseGet(() ->
                                    NoOpHandler.get().orElseThrow(RuntimeException::new))
                    );
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

        @Override
        public long initializeModuleCoverage(long edgeCount, String symbolFilePath) {
            return edgeCount;
        }

        @Override
        public void notifyModuleEdge(long edgePlusModule) {
            return;
        }
    }

    private static class LocalHandler implements OutputHandler {
        private static final String LOCAL_OUTPUT_ENV_VAR = "ANTITHESIS_SDK_LOCAL_OUTPUT";
        private final File outFile;

        private LocalHandler(final String fileName) {
            this.outFile = new File(fileName);
            if (this.outFile == null) {
                System.out.printf("Unable to access \"%s\" for assertion output\n", fileName);
            }
            String fullPath;
            try {
                fullPath = this.outFile.getAbsolutePath();
            } catch (Throwable e) {
                fullPath = fileName;
            }
            System.out.printf("Assertion output will be sent to: \"%s\"\n", fullPath);
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
                // TODO (@shomik) logging
            }
        }

        @Override
        public long random() {
            return new Random().nextLong();
        }

        @Override
        public long initializeModuleCoverage(long edgeCount, String symbolFilePath) {
            return 0;
        }

        @Override
        public void notifyModuleEdge(long edgePlusModule) {
            return;
        }
    }

}
