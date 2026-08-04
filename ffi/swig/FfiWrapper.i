%module FfiWrapper

%include "various.i"
%apply char *BYTE { const char* message }
%{
#include <stddef.h>
#include <stdint.h>
#include <stdbool.h>
#include <stdio.h>
#include <unistd.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Instrumentation ABI version 1 (see the linkage ABI versioning section of
   instrumentation.h in the Antithesis platform): present in every libvoidstar ever
   shipped, so these bind normally through the DT_NEEDED entry on
   /usr/lib/libvoidstar.so that the Makefile adds with patchelf. */
uint64_t fuzz_get_random();
void fuzz_json_data( const char* message, size_t length );
void fuzz_flush();
size_t init_coverage_module(size_t edge_count, const char* symbol_file_name);
bool notify_coverage(size_t edge_plus_module);

/* Instrumentation ABI version 2 (coverage leases + version negotiation). Declared WEAK:
   this jar ships independently of libvoidstar, so it must load — and the whole SDK must 
   keep working — against older libraries that predate these symbols. The dynamic linker
   resolves an undefined weak symbol to NULL instead of failing the load (even under 
   LD_BIND_NOW=1). These must never be referenced from generated wrapper code: every use
    below goes through a dispatcher gated on the negotiated ABI version. */
uint64_t notify_coverage_v2(size_t edge_plus_module, uint64_t hit_since_last_call) __attribute__((weak));
const uint64_t* coverage_lease_generation_addr(void) __attribute__((weak));
uint64_t instrumentation_max_abi_version(void) __attribute__((weak));
uint64_t instrumentation_request_abi_version(uint64_t requested) __attribute__((weak));

#ifdef __cplusplus
}
#endif

/* The ABI version this shim speaks. */
#define FFI_REQUESTED_ABI 2

/* Lease word layout; must match the ANTITHESIS_LEASE_* constants in instrumentation.h. */
#define FFI_LEASE_GRANTED_SHIFT 20
#define FFI_LEASE_MAX_GRANT 0xFFFFFul

/* The ABI version negotiated with the loaded libvoidstar: the granted result of
   requesting FFI_REQUESTED_ABI, or 1 if the library predates the negotiation mechanism.
   Cached; the racy first-call initialization is benign because the library latches the
   first grant for the life of the process — every call returns the same answer.*/
static unsigned long negotiated_abi(void) {
    static unsigned long cached = 0;
    unsigned long v = cached;
    if (v == 0) {
        if (instrumentation_request_abi_version != NULL) {
            v = (unsigned long)instrumentation_request_abi_version(FFI_REQUESTED_ABI);
            if (v > FFI_REQUESTED_ABI) {
                /* The library retired every version we speak, so there is no dialect we
                   know how to use. Terminate deliberately with an explanation — this
                   fires on the first instrumentation call, well before the application
                   does real work — rather than guess at semantics we don't know. */
                fprintf(stderr,
                    "Antithesis: this SDK speaks instrumentation ABI %d, but the loaded libvoidstar no longer serves it (granted %lu). Please upgrade the Antithesis Java SDK. Terminating.\n",
                    FFI_REQUESTED_ABI, v);
                _exit(1);
            }
        } else {
            v = 1;  /* Library predates ABI negotiation: version 1 by definition. */
        }
        cached = v;
    }
    return v;
}

/* What FfiWrapperJNI.notify_coverage_v2 actually calls (see %rename below). Against an
   ABI-1 library it emulates leases on top of notify_coverage() the same way the stub
   library models them: true -> grant 0 (call again on the next hit); false -> the v1
   "never call again", expressed as an epoch-0 max-grant lease that Java counts down
   locally. hit_since_last_call is deliberately dropped in emulation: an ABI-1 library
   only ever accumulates hits it told us to stop reporting, and never received them from
   v1 SDKs either. */
static unsigned long ffi_notify_coverage_v2(size_t edge_plus_module, unsigned long hit_since_last_call) {
    if (negotiated_abi() >= 2)
        return (unsigned long)notify_coverage_v2(edge_plus_module, hit_since_last_call);
    if (notify_coverage(edge_plus_module))
        return 0;
    return (FFI_LEASE_MAX_GRANT << FFI_LEASE_GRANTED_SHIFT) | FFI_LEASE_MAX_GRANT;
}

/* Revocation word handed out when the loaded libvoidstar predates leases: epoch 0
   forever, exactly like the stub library's. */
static const uint64_t emulated_lease_generation = 0;

/* SWIG-friendly shim: hand the lease revocation word's address to Java as a plain
   integer. Java reads it with sun.misc.Unsafe.getLongVolatile, because a direct
   ByteBuffer has no volatile accessor and a plain load could be hoisted out of
   JIT-compiled hot loops, hiding revocation. */
static unsigned long lease_generation_address(void) {
    if (negotiated_abi() >= 2)
        return (unsigned long)(uintptr_t)coverage_lease_generation_addr();
    return (unsigned long)(uintptr_t)&emulated_lease_generation;
}

/* Negotiated and maximum ABI versions.*/
static unsigned long native_instrumentation_abi(void) {
    return negotiated_abi();
}
static unsigned long native_max_instrumentation_abi(void) {
    if (instrumentation_max_abi_version != NULL)
        return (unsigned long)instrumentation_max_abi_version();
    return 1;
}
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
%rename(notify_coverage_v2) ffi_notify_coverage_v2;
unsigned long ffi_notify_coverage_v2(size_t edgePlusModule, unsigned long hitSinceLastCall);
unsigned long lease_generation_address();
unsigned long native_instrumentation_abi();
unsigned long native_max_instrumentation_abi();
