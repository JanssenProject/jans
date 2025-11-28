# MCP Server for Janssen configuration API

This is a MCP server for Janssen configuration API. It uses MCP Inspector to connect to the Janssen server and run tools.

Current tools:
- List OpenID Connect clients configured on the Janssen server

![alt text](image.png)

## How to run this MCP server

### PREREQUISITES

- Java 17
- Maven
- MCP Inspector installed
- A Janssen server instance

### Build MCP server

- Run `mvn clean install` to build the project
- Run `mvn clean package` to build the project

Project creates an uber jar in `target` directory.

### Run the MCP server

`run-mcp-server.sh` script is provided to run the MCP server.
This script will start chrome and run mcp inspector. 

#### Update the script environment variables
- In order to connect and invoke config-api on the Janssen server, this MCP server
needs an access token with appropriate scopes. Update the access token in the
`JANS_OAUTH_ACCESS_TOKEN` environment variable.
- Update the Janssen server URL in the `JANS_CONFIG_API_URL` environment variable.
- Save the file. 

#### Test using MCP Inspector
- Now run the script by running `./run-mcp-server.sh`. 
- This will start chrome and run mcp inspector.
- you can click `connect` to connect with the MCP server. Then click `tools` to
list all the available tools. 
- Select the tool to invoke and hit `run` to see the response from the tool. For
instance, to list all the clients configured on the Janssen server, select
`list clients` and hit `run`. This should show JSON list of clients on the jans server.
