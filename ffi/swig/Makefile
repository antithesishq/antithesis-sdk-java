build:
	# Generate java and .c files needed for JNI
	swig -java -package com.antithesis.ffi.internal FfiWrapper.i

	# Copy the generated java files to the internal package
	cp FfiWrapperJNI.java ../src/main/java/com/antithesis/ffi/internal

	# Create the wrapper DSO used by JNI to access libvoidstar
	${CC} -I"${JAVA_HOME}\include" -I . -shared -o libFfiWrapper.so *.c

	# Add a dependency which ldd will try to resolve at runtime
	# ldd will fail to resolve this outside of the Antithesis runtime environment
	# libFfiWrapper is only loaded if libvoidstar is successfully
	# loaded first.
	patchelf --add-needed /usr/lib/libvoidstar.so ./libFfiWrapper.so

	# Move the wrapper into a folder so it can be added into the target jar
	mkdir -p ../src/main/resources
	cp libFfiWrapper.so ../src/main/resources

clean:
	rm -f *.c libFfiWrapper.so *.java ../src/main/java/com/antithesis/ffi/internal/FfiWrapper*.java ../src/main/resources/*.so
