build: 
	ln -sf `dev build.instrumentation.instrumentation_determ`/lib/libinstrumentation_determ.so libvoidstar.so
	swig -java -package com.antithesis.sdk.internal VoidstarWrapper.i
	${CC} -I"${JAVA_HOME}\include" -I ${STARDIR}/instrumentation/ -shared -L . -lvoidstar -o libVoidstarWrapper.so *.c
	cp *.java ../src/main/java/com/antithesis/sdk/internal
	mkdir -p ../src/main/resources
	cp libVoidstarWrapper.so ../src/main/resources

clean:
	rm -f *.c *.so *.java ../src/main/java/com/antithesis/sdk/internal/VoidstarWrapper*.java ../src/main/resources/*.so
	
