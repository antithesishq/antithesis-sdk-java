{ pkgs ? <nixpkgs>,
  gradle2nix ? (import ./../../build_tools/java_tools.nix { inherit pkgs; }).gradle2nix,
  strip_lock_hash ? (import ./../../build_tools/java_tools.nix { inherit pkgs; }).strip_lock_hash
}:
let
  ffi = (import ./ffi/default.nix { inherit pkgs gradle2nix strip_lock_hash; }).ffi;  
in {
  inherit ffi;
  inherit (import ./sdk/default.nix { inherit pkgs gradle2nix ffi strip_lock_hash; }) sdk docs;
}
