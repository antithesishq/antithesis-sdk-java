{ pkgs ? <nixpkgs>,
  gradle2nix ? (import ./../../build_tools/java_tools.nix { inherit pkgs; }).gradle2nix,
}:
let
  ffi = (import ./ffi/default.nix { inherit pkgs gradle2nix; }).ffi;  
in {
  inherit ffi;
  inherit (import ./sdk/default.nix { inherit pkgs gradle2nix ffi; }) sdk docs;
}
