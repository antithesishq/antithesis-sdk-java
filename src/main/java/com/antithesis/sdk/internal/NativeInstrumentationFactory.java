package com.antithesis.sdk.internal;
// [WAS] package com.antithesis.instrumentation;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class encapsulates the creation of Java bindings by means of JNA. It should include
 * only code necessary to create these bindings.
 */
public final class NativeInstrumentationFactory {

    public static final String NATIVE_LIBRARY_NAME = "libvoidstar.so";
    public static final String CONTAINER_LIBRARY_DIRECTORY = "/usr/lib";

    private static final Logger logger = Logger.getLogger(NativeInstrumentationFactory.class.getName());

    private static final AntithesisInstrumentationLibrary nativeInterface;

    /**
     * The interface passed to Native#load() must match the native functions
     * as well as extend the current JNA Library marker interface.
     */
    private interface AntithesisInstrumentationLibrary extends INativeInstrumentation, Library {
    }

    static {
        try {
            if (Files.exists(Paths.get(CONTAINER_LIBRARY_DIRECTORY))) {
                NativeLibrary.addSearchPath(NATIVE_LIBRARY_NAME, CONTAINER_LIBRARY_DIRECTORY);
                logger.info(CONTAINER_LIBRARY_DIRECTORY + " added to JNA search path");
            } else {
                logger.warning(CONTAINER_LIBRARY_DIRECTORY + " is missing.");
            }
            nativeInterface = Native.load(NATIVE_LIBRARY_NAME, AntithesisInstrumentationLibrary.class);
        } catch (UnsatisfiedLinkError error) {
            // load() throws an UnsatisfiedLinkError if it can't find the native library
            // logger.log(Level.SEVERE, "Could not find " + NATIVE_LIBRARY_NAME, error);
            logger.log(Level.INFO, "Could not find " + NATIVE_LIBRARY_NAME);
            throw error;
        // TODO (@shomik) Explicitly handle NoClassDefFoundError to facilitate using JNA as a compileOnly dep and providing a concrete implementation at jar runtime
        } catch (Throwable t) {
            // logger.log(Level.SEVERE, "Could not find " + NATIVE_LIBRARY_NAME, t);
            logger.log(Level.INFO, "Could not find " + NATIVE_LIBRARY_NAME);
            throw new RuntimeException(t);
        }
    }

    public static INativeInstrumentation get() {
        return nativeInterface;
    }
}
