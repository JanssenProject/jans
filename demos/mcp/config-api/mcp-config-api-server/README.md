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
- To connect and invoke config-api on the Janssen server, this MCP server needs an access token with appropriate scopes. Get the access token from the Janssen server.

### Build MCP server

- Clone the `Jans` repository from GitHub and navigate to
`demos/mcp/config-api/mcp-config-api-server` directory.
- Run `mvn clean package` to build the project

Project creates an uber jar in `target` directory.

### Run the MCP server

- Run the command below after replacing the values for `JANS_HOST_URL` and `JANS_OAUTH_ACCESS_TOKEN` with the values for your Janssen server. This will start chrome and run mcp inspector. If you are running MCP server against a Janssen server with self-signed certificate, run this server in [development mode](#development-mode).
    ```bash
    export JANS_HOST_URL="https://example.jans.host.io" && 
    export JANS_OAUTH_ACCESS_TOKEN="b6c8ef0b-09b5-xxx-xxx-xxx" && 
    npx @modelcontextprotocol/inspector java -jar target/jans-mcp-config-api-server-0.0.1.jar
    ```

- Navigate to the Chrome instance opened by MCP Inspector.
- Click `connect` to connect with the MCP server. Then click `tools` to
list all the available tools. 
- Select the tool to invoke and hit `run` to see the response from the tool. For
instance, to list all the clients configured on the Janssen server, select
`list clients` and hit `run`. This should show JSON list of clients on the jans server.

### Development mode

This MCP server can be run in development mode by adding `-dev` flag to the
command line. This will disable SSL certificate validation. This is useful
when running MCP server against a Janssen server with self-signed certificate.

    ```bash
    export JANS_HOST_URL="https://example.jans.host.io" && 
    export JANS_OAUTH_ACCESS_TOKEN="b6c8ef0b-09b5-xxx-xxx-xxx" && 
    npx @modelcontextprotocol/inspector java -jar target/jans-mcp-config-api-server-0.0.1.jar -dev
    ```

