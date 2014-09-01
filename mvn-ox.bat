set M2_HOME=U:\own\java\apache-maven-3.0.3
set MAVEN_OPTS=-Xms128M -Xmx512M -XX:MaxPermSize=192M 

%M2_HOME%\bin\mvn.bat -s settings.xml %*
