---
tags:
- development
- contribute
- remote debug
---

# Remote Debugging

Janssen Server modules run as Java processes. Hence, like any other Java process
the JVM running the module can be configured to open a debug port where a remote
debugger can be attached. The steps below will show how to configure 
`auth-server` module for remote debugging.

1. Pass the command-line options to the JVM

   On the Janssen Server host, open the service config file 
   `/etc/default/jans-auth` and add the following JVM parameters to as 
   `JAVA_OPTIONS`
    ```
    -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=6001
    ```
   This will open the port `6001` for the remote debugger. Any other port can 
   also be used based on availability.

2. Restart `jans-auth` services
    ```
    systemctl restart jans-auth.service
    ```

3. Check if the port is open and accessible from within the Janssen Server host
   Use the `jdb` tool from JDK to test if the JVM port has been opened
   ```
   ./<path-to-JDK>/bin/jdb -attach 6001
   ```
   if the port is open, it'll give you output like the below:
   ```
   Set uncaught java.lang.Throwable
   Set deferred uncaught java.lang.Throwable
   Initializing jdb ...
   >
   ```
   press `ctrl+c` to come out of it.

4. Ensure that the port is accessible from outside the host VM as well and 
   firewalls are configured accordingly

5. Connect to the remote port on the Janssen Server host from the developer 
   workstation. Use any IDE (Intellij, Eclipse,
   etc.) to create and run a remote debugging profile. Provide IP and debug 
   port of the Janssen Server host.

   For IntelliJIdea, create a debug configuration as below:

   ![](../assets/image-jans-remote-debug-intellij.png)