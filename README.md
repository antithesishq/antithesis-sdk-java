# Antithesis SDK for Java

This library provides classes for Java programs to configure the [Antithesis](https://antithesis.com) platform. It contains three kinds of functionality:
* Assertion macros that allow you to define test properties about your software or workload.
* Randomness functions for requesting both structured and unstructured randomness from the Antithesis platform.
* Lifecycle functions that inform the Antithesis environment that particular test phases or milestones have been reached.

For general usage guidance see the [Antithesis Java SDK Documentation](https://antithesis.com/docs/using_antithesis/sdk/java/)

### Build
This has been successfully built on Linux using
`gradle 8.7`, `groovy 3.0.11` and `openjdk8 8u362-ga`.  

    # Clean and build
    ./gradlew clean
    ./gradlew build

    # Build results
    ls -l build/libs/antithesis-sdk-*.jar build/libs/antithesis-ffi-*.jar

### Runtime
The antithesis SDK for Java will expect the following
at runtime. Minimum versions are shown and later versions 
should work.

    antithesis-ffi 1.4.3 (or above)
    jackson 2.2.3 (or above)

