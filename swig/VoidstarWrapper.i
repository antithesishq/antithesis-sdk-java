%module VoidstarWrapper
%{
#include "instrumentation.h"
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
void notify_coverage(size_t edgePlusModule);