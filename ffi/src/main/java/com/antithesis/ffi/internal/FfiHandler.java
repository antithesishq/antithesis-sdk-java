package com.antithesis.ffi.internal;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class FfiHandler implements OutputHandler {

    private static long offset = -1;

    // Coverage lease bookkeeping — see "Coverage leases" in libvoidstar's
    // instrumentation.h for the contract. Each entry holds the lease word returned by
    // notify_coverage_v2 for that edge: epoch(24) | granted(20) | remaining(20). While
    // an entry has remaining > 0 and its epoch matches the current value of
    // libvoidstar's revocation word, hits of that edge are counted locally by
    // decrementing the entry instead of crossing JNI (and taking the process-global
    // native lock behind it). A zeroed entry always sends the next hit to native, so
    // an edge's first hit — the one that can produce new coverage — can never be
    // skipped.
    //
    // Unsynchronized on purpose: a race can lose a decrement or absorb NUM_THREADS extra
    // hits, which only perturbs pause-interval statistics.
    private static long[] lease;
    private static long leaseGenerationAddress;

    // Field layout of a lease word; must match instrumentation.h.
    private static final int EPOCH_SHIFT = 40;
    private static final int GRANTED_SHIFT = 20;
    private static final long FIELD_MASK = 0xFFFFF;
    private static final long EPOCH_MASK = 0xFFFFFF;

    // The revocation word must be read with volatile semantics: the JIT may hoist a
    // plain loop-invariant load out of a hot loop, and a thread spinning in fully
    // leased code would then never observe revocation. On x86 a volatile load costs
    // the same as a plain one. Unsafe rather than VarHandle because the SDK targets
    // Java 8; if Unsafe is unavailable, leases stay disabled and every hit calls
    // into libvoidstar (correct, just slower).
    private static final sun.misc.Unsafe UNSAFE = loadUnsafe();

    private static sun.misc.Unsafe loadUnsafe() {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (sun.misc.Unsafe) field.get(null);
        } catch (Throwable t) {
            System.err.println("Antithesis: sun.misc.Unsafe unavailable (" + t + "); coverage leases disabled.");
            return null;
        }
    }

    public static Optional<OutputHandler> get() {
        if (FfiWrapperJNI.LOAD_LIBRARY_MARKER) {
            return Optional.of(new FfiHandler());
        }
        return Optional.empty();
    }

    public static long initializeModuleCoverage(long edgeCount, String symbolFilePath) {
        if (offset != -1) {
            // A Java application may not contain multiple "modules" in Antithesis terms.
            throw new IllegalStateException("Antithesis Java instrumentation has already been initialized.");
        }
        if (edgeCount > Integer.MAX_VALUE || edgeCount < 1) {
            throw new IllegalArgumentException("Antithesis Java instrumentation supports [1 ," + Integer.MAX_VALUE + "] edges");
        }
        offset = FfiWrapperJNI.init_coverage_module(edgeCount, symbolFilePath);
        long address = FfiWrapperJNI.lease_generation_address();
        if (UNSAFE != null && address != 0) {
            leaseGenerationAddress = address;
            // Edge ids from the instrumentor are 1-based, so the table needs edgeCount + 1 slots.
            lease = new long[(int) Math.min(edgeCount + 1, Integer.MAX_VALUE)];
        }
        long negotiatedAbi = FfiWrapperJNI.native_instrumentation_abi();
        long maxAbi = FfiWrapperJNI.native_max_instrumentation_abi();
        String msg = String.format(
                "Initialized Java module at offset 0x%016x with %d edges; symbol file %s; native instrumentation ABI %d negotiated (max %d): %s",
                offset, edgeCount, symbolFilePath, negotiatedAbi, maxAbi,
                negotiatedAbi >= 2 ? "coverage leases" : "lease emulation over v1");
        System.err.println(msg);
        return offset;
    }

    public static void notifyModuleEdge(long edgePlusModule) {
        final long[] leases = lease;
        final long edge = edgePlusModule - offset;
        if (leases == null || edge < 0 || edge >= leases.length) {
            FfiWrapperJNI.notify_coverage_v2(edgePlusModule, 0);
            return;
        }
        final int index = (int) edge;
        final long entry = leases[index];
        final long remaining = entry & FIELD_MASK;
        if (remaining != 0
                && (entry >>> EPOCH_SHIFT) == (UNSAFE.getLongVolatile(null, leaseGenerationAddress) & EPOCH_MASK)) {
            leases[index] = entry - 1;  // absorb this hit locally
            return;
        }
        // Report how many times this edge was hit under the old lease so libvoidstar's
        // per-edge counters stay exact whether it expired naturally or was revoked
        // mid-flight.
        final long hits = ((entry >>> GRANTED_SHIFT) & FIELD_MASK) - remaining;
        leases[index] = FfiWrapperJNI.notify_coverage_v2(edgePlusModule, hits);
    }

    @Override
    public void output(final String value) {
        // `fuzz_json_data` expects length in UTF-8 encoded bytes.
        byte[] utf8Bytes = value.getBytes(StandardCharsets.UTF_8);
        FfiWrapperJNI.fuzz_json_data(utf8Bytes, utf8Bytes.length);
        FfiWrapperJNI.fuzz_flush();
    }

    @Override
    public long random() {
        return FfiWrapperJNI.fuzz_get_random();
    }

}
