{ ffiPath ? (import ./../ffi/default.nix {}).java_ffi
}:
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


  sdk = let
    f_text = builtins.readFile ../gradle.properties;
    f_lines = builtins.split "\n" f_text;
    f_lines_text = builtins.filter(el: builtins.isString el) f_lines;
    all_version_lines = builtins.filter(el: pkgs.lib.strings.hasInfix "version" el) f_lines_text;
    first_version_line = pkgs.lib.lists.findFirst(el: true) "version=0.0.1" all_version_lines;
    line_components = pkgs.lib.strings.splitString "=" first_version_line;
    version_component = builtins.elemAt line_components 1;
    sdk_version = builtins.replaceStrings[" " "\t"] ["" ""] version_component;
  in (gradle2nix.buildGradlePackage {
      pname = "Antithesis Java SDK";
      version = sdk_version;
      lockFile = "${./gradle.lock}";
      gradleBuildFlags = [ "--quiet" "build" ];
      buildJdkVersion = pkgs.jdk21;
    }).overrideAttrs(_: prev: {
      src = [ 
        ./build.gradle 
        ./settings.gradle 
        ../gradle.properties 
        ./src 
      ];

      # A custom "unpack" is needed to provide a straightforward 
      # src directory with mixed files and subdirectories
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
        # make sure we can copy files into this directory (and below)
        chmod -R 777 .

        # copy the ffi jar into src/libs/antithesis-ffi-1.3.0.jar
        mkdir -p libs
        cp -R ${ffiPath}/*.jar libs

        runHook postUnpack
      '';

      installPhase = ''
        runHook preInstall
        mkdir -p $out/lib
        cp --verbose build/{libs,dependencies}/*.jar $out/lib
        runHook postInstall
      '';
  });

in {
  inherit sdk;
  java_sdk = "${sdk}/lib";

  gradleUpdateScript = pkgs.writeShellScript "generate_gradle_lock_file" ''
    export JAVA_HOME=${pkgs.jdk21}
    ${gradle2nix}/bin/gradle2nix
  '';
}
