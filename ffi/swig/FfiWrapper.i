%module FfiWrapper

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

    public static void loadLibrary() throws IOException {
        try {
            if (hasNativeLibrary()) {
                System.load(NATIVE_LIBRARY_PATH);
                File file = File.createTempFile("libFfiWrapper", ".so");
                try (InputStream link = (Thread.currentThread().getContextClassLoader().getResourceAsStream("libFfiWrapper.so"))){
                    Files.copy(
                        link,
                        file.getAbsoluteFile().toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                    System.load(file.getAbsoluteFile().toString());
                }
            } else {
                throw new RuntimeException("Native code library failed to load");
            }
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load. \n" + e);
            throw e;
        }
    }
%}

unsigned long fuzz_get_random();
void fuzz_json_data( const char* message, size_t length );
void fuzz_flush();
size_t init_coverage_module(size_t edgeCount, const char* symbolFilePath);
bool notify_coverage(size_t edgePlusModule);
