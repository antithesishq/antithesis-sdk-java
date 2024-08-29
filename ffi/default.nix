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

  ffi = let
    f_text = builtins.readFile ../gradle.properties;
    f_lines = builtins.split "\n" f_text;
    f_lines_text = builtins.filter(el: builtins.isString el) f_lines;
    all_version_lines = builtins.filter(el: pkgs.lib.strings.hasInfix "version" el) f_lines_text;
    first_version_line = pkgs.lib.lists.findFirst(el: true) "version=0.0.1" all_version_lines;
    line_components = pkgs.lib.strings.splitString "=" first_version_line;
    version_component = builtins.elemAt line_components 1;
    ffi_version = builtins.replaceStrings[" " "\t"] ["" ""] version_component;
  in (gradle2nix.buildGradlePackage {
      pname = "Antithesis Java FFI";
      version = ffi_version;
      lockFile = "${./gradle.lock}";
      gradleBuildFlags = [ "--quiet" "build" ];
      buildJdkVersion = pkgs.jdk21;
    }).overrideAttrs(_: prev: {
      src = [
        ./build.gradle
        ./settings.gradle
        ../gradle.properties
        ./src
        ./swig
      ];

      # A custom "unpack" is needed to provide a straightforward 
      # src directory with mixed files and subdirectories
      unpackPhase = let 
        ffinix = (import ./ffi.nix {inherit pkgs;});
        ffibuild = ffinix.build_script;
      in ''
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

        # push in a custom settings.gradle
        # echo "rootProject.name = 'antithesis'" > settings.gradle

        # Establish ffi_build.sh for later use by gradle
        # the 'buildFfiBridge' task invokes ffi_build.sh via commandLine'
        ln -s ${ffibuild}  ./ffi_build.sh
 
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
  inherit ffi;

  gradleUpdateScript = pkgs.writeShellScript "generate_gradle_lock_file" ''
    export JAVA_HOME=${pkgs.jdk21}
    ${gradle2nix}/bin/gradle2nix
  '';
}
