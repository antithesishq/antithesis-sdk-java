{ pkgs ? <nixpkgs>,
  gradle2nix ? (import (builtins.fetchTarball {
    name = "gradle2nix";
    url =
      "https://github.com/tadfisher/gradle2nix/archive/6e37e6e3f91701a633c53a6f06937f714cdcc530.tar.gz";
    sha256 = "sha256:1viz4jql51dmszmcrxw1cdwcwg9zfmlrvb9z36q7mhb7qc12hcak";
  }) { inherit pkgs; })
}:
let
  ffi = (import ./ffi/default.nix { inherit pkgs gradle2nix; }).ffi;  
in {
  inherit ffi;
  inherit (import ./sdk/default.nix { inherit pkgs gradle2nix ffi; }) sdk docs;
}
