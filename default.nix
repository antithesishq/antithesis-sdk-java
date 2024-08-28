{ }:
let
  ffiPath = (import ./ffi/default.nix {}).java_ffi;
in {
  inherit (import ./sdk/default.nix { inherit ffiPath; }) sdk java_sdk docs;
}
