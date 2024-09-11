{ pkgs ? (import ./../../../star/build_tools/pinned_nixpkgs.nix).pkgs}:

let
  build_script =
    pkgs.writeShellScript "build_ffi.sh" ''
      set -eu
      cd swig

			# Generate java and .c files needed for JNI
			# swig -version
			${pkgs.swig}/bin/swig -java -package com.antithesis.ffi.internal FfiWrapper.i

			# Copy the generated java files to the internal package
			cp --verbose FfiWrapperJNI.java ../src/main/java/com/antithesis/ffi/internal

			# Create the wrapper DSO used by JNI to access libvoidstar
			ls -R ${pkgs.jdk8}/include 1>&2 
			${pkgs.clang}/bin/clang `${pkgs.findutils}/bin/find $(realpath ${pkgs.jdk8}/include) -type d -exec echo '-I{}' \;` -I . -shared -o libFfiWrapper.so *.c

			# Add a dependency which ldd will try to resolve at runtime
			# ldd will fail to resolve this outside of the Antithesis runtime environment
			# libFfiWrapper is only loaded if libvoidstar is successfully
			# loaded first.
			${pkgs.patchelf}/bin/patchelf --add-needed /usr/lib/libvoidstar.so ./libFfiWrapper.so

			# Move the wrapper into a folder so it can be added into the target jar
			mkdir -p ../src/main/resources
			cp --verbose libFfiWrapper.so ../src/main/resources
    '';

in {
  inherit build_script;
}
