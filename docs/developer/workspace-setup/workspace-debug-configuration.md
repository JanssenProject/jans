# Workspace setup for debugging Janssen Server

In this guide we will make configuration changes in your [Janssen workspace](setup-developer-workspace.md) to enable IDE based debugging. 
Guide uses IntelliJIdea Java IDE. 


- Create debug configuration 
  - Use `add configuration` dialogue and add a new configuration of type `Remote JVM Debug` 
  - Set `host` value to *localhost* and port to any available port. Guide will use `5892`. 
  - `save` this configuration
- For Janssen server to listen for incoming debugger connections, we need to pass JVM parameters.
  - Under `<jans-auth-server-code>/server` create new folder called `.mvn` and under that create a file `jvm.config` in this file put line

  ```
  -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5892
  ```

- Start server using maven 

  ```
   mvn -DskipTests -Djans.base=./target jetty:run-war
  ```

Server will suspend immedieately at the start up and wait for incoming debugger connection

- From IDE, run the remote JVM configuration to start your debug session. Debugger will connect with server on port `5892`.



	
