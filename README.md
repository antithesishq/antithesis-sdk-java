# Antithesis SDK for Java

## NOTE: This SDK is still under development & is not yet supported

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

    antithesis-ffi 1.3.1 (or above)
    jackson 2.2.3 (or above)

