%module FfiWrapper

%include "various.i"
%apply char *BYTE { const char* message }
%{
#include <stddef.h>
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

uint64_t fuzz_get_random();
void fuzz_json_data( const char* message, size_t length );
void fuzz_flush();
size_t init_coverage_module(size_t edge_count, const char* symbol_file_name);
bool notify_coverage(size_t edge_plus_module);

#ifdef __cplusplus
}
#endif
%}

%pragma(java) jniclassimports=%{
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
%}

%pragma(java) jniclasscode=%{
    private static final String NATIVE_LIBRARY_PATH = "/usr/lib/libvoidstar.so";

    public static boolean hasNativeLibrary() {
       return Files.exists(Paths.get(NATIVE_LIBRARY_PATH));
    }

    // Static variables initialization is guaranteed to execute by the Java Language Spec
    public static final boolean LOAD_LIBRARY_MARKER = loadLibrary();

    private static boolean loadLibrary() {
        boolean nativeLibraryFound = hasNativeLibrary();
        if (nativeLibraryFound) {
            // We follow the steps below to load the native library:
            // 1. Identify if the system temp directory exists, if not create it.
            // 2. Find the libFfiWrapper.so in the classpath. The libFfiWrapper.so should be
            // packed with antithesis-ffi-VERSION.jar.
            // 3. Copy the libFfiWrapper.so to the system temp directory.
            // * We do not load the library through `System.loadLibrary` because Spring boot was apparently notably
            // unhappy under certain setups with us trying to put it in known loadLibrary paths thus,
            // we instead create a temp file and load it by absolute path.
            try {
                File tmpDir =  new File(System.getProperty("java.io.tmpdir", "/tmp"));
                if (!tmpDir.exists()) {
                    tmpDir.mkdirs();
                }
                File file = File.createTempFile("libFfiWrapper", ".so", tmpDir);
                try (InputStream link = (Thread.currentThread().getContextClassLoader().getResourceAsStream("libFfiWrapper.so"))){
                    Files.copy(
                        link,
                        file.getAbsoluteFile().toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                    System.load(file.getAbsoluteFile().toString());
                }
                System.err.println("Successfully loaded native library!");
            } catch (UnsatisfiedLinkError e) {
                System.err.println("Failed to load a native library:" + e);
                return false;
            } catch (IOException e) {
                System.err.println("Failed to load FFI wrapper from resources:" + e);
                return false;
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e);
                return false;
            }
        }
        return nativeLibraryFound;
    }
%}

unsigned long fuzz_get_random();
void fuzz_json_data( const char* message, size_t length );
void fuzz_flush();
size_t init_coverage_module(size_t edgeCount, const char* symbolFilePath);
bool notify_coverage(size_t edgePlusModule);
