---
tags:
  - administration
  - tools
  - config-api
  - monitoring
---

# Health - Check endpoints
- /api/v1/health - Returns application consolidated health status - liveness and readiness
- /api/v1/health/live - Returns application liveness status
- /api/v1/health/ready -Returns application readiness status
- /api/v1/health/server-stat - Returns application server status and stats metric like `memoryfree`, `uptime`, `free_disk_space`, etc

Sample health response
```json
HTTP/1.1 200 OK
Content-Type: application/json
[
    {
        "name": "jans-config-api liveness",
        "status": "UP"
    },
    {
        "name": "jans-config-api readiness",
        "status": "UP"
    }
]
```

Sample liveness response
```json
HTTP/1.1 200 OK
Content-Type: application/json
{
    "name": "jans-config-api liveness",
    "status": "UP"
}
```

Sample readiness response
```json
HTTP/1.1 200 OK
Content-Type: application/json
{
    "name": "jans-config-api readiness",
    "status": "UP"
}
```

Sample server status response
```json
HTTP/1.1 200 OK
Content-Type: application/json
{
    "dbType":"MySQL",
	"lastUpdate":"2023-03-16T03:17:44",
	"facterData":
	{
	    "memoryfree":"7.1%",
		"swapfree":"60.1%",
		"hostname":"jans.server2",
		"ipaddress":"192.168.0.102",
		"uptime":"3:03",
		"free_disk_space":"65.0%",
		"load_average":"0.77"
	}
}
```


## Application logs
Application log can be verified if application is started properly.
[Refer here](logs.md) for more details.
