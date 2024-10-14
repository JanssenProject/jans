---
tags:
- command
- jans
---


# Jans Command Overview

The `jans` command is a top-level wrapper script for managing the Janssen Server. 
This guide provides an overview of its usage and available commands.


## Available Commands

### 1. Version

Displays the version of the currently installed Janssen Server.

```bash title="Command"
jans version
```
![gif](../../assets/jans-version.gif)


### 2. CLI

Description: Invokes the Janssen Command-Line Interface.
```bash title="Command"
jans version
```



### 3. TUI
Launches the text-based user interface for Janssen.

```bash title="Command"
jans tui
```
     ![gif](../../assets/jans-tui.gif)

### 4. Logs

Shows the log file paths for various Janssen Server modules. 
```bash title="Command"
jans logs
```
![gif](../../assets/jans-logs.gif)


### 5. Status

Displays the status of Janssen Server module services.

```bash title="Command"
jans status
```
![gif](../../assets/jans-status.gif)


### 6. Start 

Starts services for the Janssen Server.

```bash title="Command"
     jans start
```
Start a specific service.

```bash title="Command"
jans start -service=<service-name>
```
![gif](../../assets/jans-start.gif)

### 7. Stop

Stops services for the Janssen Server.
```bash title="Command"
jans stop
```
Stop a specific service.

```bash title="Command"
jans stop -service=<service-name>
```

![gif](../../assets/jans-stop.gif)

### 8. Restart 

Restarts services for the Janssen Server.

```bash title="Command"
jans restart
```
Restart a specific service.

```bash title="Command"
jans restart -service=<service-name>
```
![gif](../../assets/jans-restart.gif)


### 9. Health

Retrieves health status from the Janssen services' health-check endpoint.

```bash title="Command"
jans health
```
Health check for specific service.

```bash title="Command"
jans health -service=<service-name>
```
![gif](../../assets/jans-health.gif)


### 10. Info

Lists important URLs, such as .well-known and Casa.

```bash title="Command"
jans info
```
![git](../../assets/jans-info.gif)




