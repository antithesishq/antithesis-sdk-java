build: 
	# Generate java and .c files needed for JNI
	swig -java -package com.antithesis.sdk.internal VoidstarWrapper.i

  # Copy the generated java files to the internal package
	cp *.java ../src/main/java/com/antithesis/sdk/internal

	# Create the wrapper DSO used by JNI to access libvoidstar
	${CC} -I"${JAVA_HOME}\include" -I . -shared -o libVoidstarWrapper.so *.c

	# Add a dependency which ldd will try to resolve at runtime
	# ldd will fail to resolve this outside of the Antithesis runtime environment
	# libVoidstarWrapper is only loaded if libvoidstar is successfully
	# loaded first.
	patchelf --add-needed libvoidstar.so ./libVoidstarWrapper.so

	# Move the wrapper into a folder so it can be added into the target jar
	mkdir -p ../src/main/resources
	cp libVoidstarWrapper.so ../src/main/resources

clean:
	rm -f *.c libVoidstarWrapper.so *.java ../src/main/java/com/antithesis/sdk/internal/VoidstarWrapper*.java ../src/main/resources/*.so
	
