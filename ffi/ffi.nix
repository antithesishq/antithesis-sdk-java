{ pkgs }:

let
  build_script = pkgs.writeShellScript "build_ffi.sh" ''
   PATH=${pkgs.lib.makeBinPath ( with pkgs; [swig clang findutils patchelf ]) }:$PATH
    set -eu
    export JAVA_HOME=${pkgs.jdk8}
    ${pkgs.gnumake}/bin/make -C swig build
  '';

in { inherit build_script; }
