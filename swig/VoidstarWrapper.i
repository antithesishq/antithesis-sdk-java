%module VoidstarWrapper
// %{
// #include "sdk.h"
// %}

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
import java.nio.file.StandardCopyOption;
%}

%pragma(java) jniclasscode=%{
    static void loadLibrary() throws IOException {
        try {
            System.load("/usr/lib/libvoidstar.so");
            File file = File.createTempFile("libVoidstarWrapper", ".so");
            try (InputStream link = (Thread.currentThread().getContextClassLoader().getResourceAsStream("libVoidstarWrapper.so"))){
                Files.copy(
                    link,
                    file.getAbsoluteFile().toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
                System.load(file.getAbsoluteFile().toString());
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
