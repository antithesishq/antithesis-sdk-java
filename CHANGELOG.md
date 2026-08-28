# Changelog

## 1.7.0 - 2026-08-28

- Fixed a data race in numeric guidance assertions.
- When used in local debug mode, the output file (`ANTITHESIS_SDK_LOCAL_OUTPUT`) will no longer be truncated at initialization.
- `Lifecycle.sendEvent` no longer trims the event name or substitutes `"anonymous"` for empty names; names pass through verbatim, matching the other SDKs.
- Fixed an initialization race where an event emitted by a second thread could precede the `antithesis_sdk` version header in the output stream.
- The instrumentor's symbol table now reports each edge's *source file* (the class's `SourceFile` attribute, qualified by its package directory) in the `file` column, instead of the name of the jar containing the class, so triage reports show real code locations. Classes compiled without a `SourceFile` attribute still fall back to the jar name.

## 1.6.0 - 2026-08-04

- Remove small modulo bias from `random_choice`.
- Add `com.antithesis.sdk.AntithesisRandom`, a subclass of `java.util.Random` that draws entropy from `com.antithesis.sdk.Random`.
- Adopt libvoidstar coverage leases: hot edges are counted locally and cross JNI only when
  the platform needs to hear about them, instead of on every hit.
- Negotiate the instrumentation ABI version with libvoidstar at load. The native shim
  binds all post-v1 symbols weakly and emulates lease behavior over the v1 interface, so
  a newer jar runs correctly against any libvoidstar vintage. The negotiated ABI is logged
  at initialization.

## 1.5.1 - 2026-07-07

- Skip redundant coverage callbacks. Once the platform indicates an edge no longer needs to be reported, the SDK avoids the native call for every later hit of that edge, matching the behavior of the other SDKs.
- Fix minor concurrency issue in edge reporting

## 1.5.0 - 2026-03-23

Bump ASM to 9.9.1 for scanning classes compiled with the latest Java versions.

## 1.4.6 - 2026-02-09

Reduce verbosity of guidance tracking. The SDK now only emits guidance events when a value strictly exceeds the previous tracked min/max, rather than on equal values too.

## 1.4.5 - 2025-11-10

Update `jackson-databind` dependency to require 2.18.5 or later (2.18 is the current LTS-supported release).

## 1.4.4 - 2025-07-01

Internal release workflow fixes. No user-visible changes.

## 1.4.3 - 2025-05-12

Emit error messages to stderr instead of stdout for both the SDK and FFI.

## 1.4.2 - 2025-02-27

Switch assertion cataloging to use abstract interpretation for more reliable results for `message`. This fixes cases where the previous bytecode-scanning approach missed or mis-cataloged assertions.

## 1.4.1 - 2025-02-07

Miscellanous bug fixes and cleanup.

## 1.4.0 - 2024-11-07

Adding guidance-based assertions. These are both assertions and guidance for the fuzzer to explore your program more effectively.

Unsign JARs during Java instrumentation, fixing instrumentation of signed third-party libraries.

Improve SDK robustness.

## 1.3.1 - 2024-08-16

Internal build system improvements (Gradle). No user-visible changes.

## 1.3.0 - 2024-08-05

Initial release.
