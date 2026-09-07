# Jans Command Overview

The `jans` command is a top-level wrapper script for managing the Janssen Server. This guide provides an overview of its usage and available commands. List of available commands may change as more commands are added. To see the current list of commands available in your installation, run following command at the Janssen Server:

Command

```
jans
```

## Available Commands

### Version and build information

Displays the version and build information of the currently installed Janssen Server.

Command

```
jans version
```

### CLI

Invokes the Janssen Command-Line Interface.

Command

```
jans cli
```

### TUI

Launches the text-based user interface for Janssen.

Command

```
jans tui
```

### Logs

Shows the log file paths for various Janssen Server modules.

Command

```
jans logs
```

### Status

Displays the status of Janssen Server module services.

Command

```
jans status
```

### Start

Starts services for the Janssen Server.

Command

```
     jans start
```

Start a specific service.

sample command

```
jans start -service=jans-config-api
```

Sample Output

```
Executing sudo systemctl start jans-config-api
```

### Stop

Stops services for the Janssen Server.

Command

```
jans stop
```

Stop a specific service.

Command

```
jans stop -service=jans-config-api
```

Sample Output

```
Executing sudo systemctl stop jans-config-api
```

### Restart

Restart services for the Janssen Server.

Command

```
jans restart
```

Restart a specific service.

Sample Command

```
jans restart -service=jans-config-api
```

Sample Output

```
Executing sudo systemctl restart jans-config-api
```

### Health

Retrieves health status from the Janssen services health-check endpoint.

Command

```
jans health
```

Health check for specific service.

Command

```
jans health -service=<service-name>
```

sample Output

```
Checking health status for jans-config-api
  Executing curl -s http://localhost:8074/jans-config-api/api/v1/health/live
  Command output: {"name":"jans-config-api liveness","status":"UP"}
```

### Info

Lists important URLs such as `.well-known` endpoints.

Command

```
jans info
```
