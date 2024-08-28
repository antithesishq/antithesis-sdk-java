{ }:
let
  pkgs = import (builtins.fetchTarball {
    name = "nixos-24.05-release";
    url =
      "https://github.com/NixOS/nixpkgs/archive/63dacb46bf939521bdc93981b4cbb7ecb58427a0.tar.gz";
    sha256 = "sha256:1lr1h35prqkd1mkmzriwlpvxcb34kmhc9dnr48gkm8hh089hifmx";
  }) { };

  # Load the last verified release of Gradle2Nix v2, because the tip has a kotlin DSL mismatch (8.19.24)
  gradle2nix = (import (builtins.fetchTarball {
    name = "gradle2nix";
    url =
      "https://github.com/tadfisher/gradle2nix/archive/6e37e6e3f91701a633c53a6f06937f714cdcc530.tar.gz";
    sha256 = "sha256:1viz4jql51dmszmcrxw1cdwcwg9zfmlrvb9z36q7mhb7qc12hcak";
  }) { inherit pkgs; });

  ffi = (gradle2nix.buildGradlePackage {
    pname = "Antithesis Java FFI";
    version = "1.3.1";
    lockFile = "${./gradle.lock}";
    gradleBuildFlags = [ "-p ffi" "--quiet" "build" ];
    buildJdkVersion = pkgs.jdk21;
  }).overrideAttrs (_: prev: {
    src = [ ./build.gradle ./settings.gradle ./gradle.properties ./ffi/src ];
    # A custom "unpack" is needed to provide a straightforward src directory with mixed files and subdirectories
    unpackPhase = ''
      runHook preUnpack
      for srcFile in $src; do
          if [ -d $srcFile ]; then
              mkdir $(stripHash $srcFile)
              cp -r $srcFile/* $(stripHash $srcFile)
          else
              cp $srcFile $(stripHash $srcFile)
          fi 
      done
      runHook postUnpack
    '';

    installPhase = ''
      runHook preInstall
      mkdir -p $out/lib
      cp build/{libs,dependencies}/*.jar $out/lib
      cp -r build/docs $out/docs
      runHook postInstall
    '';
  });

  # [PH             sdk = (gradle2nix.buildGradlePackage {
  # [PH               pname = "Antithesis Java SDK";
  # [PH               version = "1.3.1";
  # [PH               lockFile = "${./gradle.lock}";
  # [PH               gradleBuildFlags = [ "-p sdk" "--quiet" "build" ];
  # [PH               buildJdkVersion = pkgs.jdk21;
  # [PH             }).overrideAttrs (_: prev: {
  # [PH               src = [ ./build.gradle ./settings.gradle ./gradle.properties ./sdk/src ];
  # [PH               # A custom "unpack" is needed to provide a straightforward src directory with mixed files and subdirectories
  # [PH               unpackPhase = ''
  # [PH                 runHook preUnpack
  # [PH                 for srcFile in $src; do
  # [PH                     if [ -d $srcFile ]; then
  # [PH                         mkdir $(stripHash $srcFile)
  # [PH                         cp -r $srcFile/* $(stripHash $srcFile)
  # [PH                     else
  # [PH                         cp $srcFile $(stripHash $srcFile)
  # [PH                     fi 
  # [PH                 done
  # [PH                 runHook postUnpack
  # [PH               '';
  # [PH           
  # [PH               installPhase = ''
  # [PH                 runHook preInstall
  # [PH                 mkdir -p $out/lib
  # [PH                 cp build/{libs,dependencies}/*.jar $out/lib
  # [PH                 cp -r build/docs $out/docs
  # [PH                 runHook postInstall
  # [PH               '';
  # [PH             });
in {
  #inherit ffi sdk;
  #java_sdk = "${sdk}/lib";
  inherit ffi;
  java_ffi = "${ffi}/lib";
  #docs = "${sdk}/docs";

  gradleUpdateScript = pkgs.writeShellScript "generate_gradle_lock_file" ''
    export JAVA_HOME=${pkgs.jdk21}
    ${gradle2nix}/bin/gradle2nix
  '';
}
