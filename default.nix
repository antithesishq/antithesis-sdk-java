{ }:
let
  ffi = (import ./ffi/default.nix {}).ffi;  
in {
  inherit ffi;
  inherit (import ./sdk/default.nix { inherit ffi; }) sdk docs;
}
