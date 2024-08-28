{ pkgs ? (import ./../../../star/build_tools/pinned_nixpkgs.nix).pkgs}:

let
  build_script =
    pkgs.writeShellScript "build_ffi.sh" ''
      set -e
      cd swig

	    # Generate java and .c files needed for JNI
	    # swig -version
	    ${pkgs.swig}/bin/swig -java -package com.antithesis.ffi.internal FfiWrapper.i

      # Copy the generated java files to the internal package
	    cp --verbose FfiWrapperJNI.java ../src/main/java/com/antithesis/ffi/internal

	    # Create the wrapper DSO used by JNI to access libvoidstar
	    ${pkgs.clang}/bin/clang -I"${pkgs.jdk8}/include" -I . -shared -o libFfiWrapper.so *.c

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
