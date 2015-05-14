#traceall.jar and aspectjweaver.jar must in the classpath

set CLASSPATH=%CLASSPATH%;<aspectj_path>/aspectjweaver.jar;<traceall_dir>/traceall-<version>.jar

#set the following jvm args too

set JAVA_OPTS=-javaagent:<aspectj_path>/aspectjweaver.jar -Dorg.nkumar.utilities.traceall.leastCount=50

#change the scope in AllTracingAspect
