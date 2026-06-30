# Developing for Janssen Project

This section provides useful resources and how-tos to the developers who want to participate in the development of the Janssen Project.

## Build

Janssen is a multi-module monorepo. Java components are Maven projects; each top-level module is a parent POM that builds its own sub-modules. Build them **in dependency order** so artifacts land in your local Maven repository (`~/.m2/repository`) before downstream modules need them.

### Prerequisites

- **JDK 11** or later
- **Apache Maven 3.0.3** or later
- **Git**
- **Rust tool-chain** (only if building `cedarling-java` from source instead of using pre-built native libraries from [GitHub Releases](https://github.com/JanssenProject/jans/releases))

### Clone the repository

```
git clone https://github.com/JanssenProject/jans.git
cd jans
```

### Resolving dependencies using GitHub hosted packages

Janssen server packages are built every 24 during the nightly builds. These are hosted on [GitHub Packages](https://github.com/orgs/JanssenProject/packages).

When building any module locally, the build process can download any missing Janssen Server dependencies from GitHub packages. To enable this, follow the steps below. This will to configure appropriate access token in maven settings.

- Install [GitHub CLI](https://cli.github.com/)

- Sign-in to GitHub from command-line

  ```
  gh auth login
  ```

- Add `read:packages` scope to your access token using the command below

  ```
  gh auth refresh -s read:packages
  ```

- Extract the token

  ```
  gh auth token
  ```

- Add the configuration below to your maven settings file. Create the file if it does not exist.

  ```
  <settings>
    <servers>
      <server>
        <id>github</id>
        <username>YOUR_GITHUB_USERNAME</username>
        <password>PASTE_YOUR_GH_AUTH_TOKEN_HERE</password>
      </server>
    </servers>
  </settings>
  ```

### Build Java modules

Use the command below to build each module.

```
mvn clean install -Dmaven.test.skip=true -f <path-to-module-pom-file>
```

Note

Remove `-Dmaven.test.skip=true` to execute tests during the build. Note that some of the modules contain integration tests. These tests need [environment setup](https://docs.jans.io/nightly/contribute/development/run-integration-tests/index.md) to be successful. Running tests without the required environment setup may fail the build.

Java modules are listed below.

| #   | Module directory                         | Description                                    |
| --- | ---------------------------------------- | ---------------------------------------------- |
| 1   | `jans-bom`                               | Shared dependency versions (Bill of Materials) |
| 2   | `jans-orm`                               | Object-relational mapping layer                |
| 3   | `jans-core`                              | Shared libraries used across Janssen services  |
| 4   | `agama`                                  | Agama DSL and engine                           |
| 5   | `jans-auth-server`                       | Authorization Server (OpenID Connect / OAuth)  |
| 6   | `jans-cedarling/bindings/cedarling-java` | Cedarling Java/Kotlin binding (see note below) |
| 7   | `jans-lock/lock-server`                  | Lock Server                                    |
| 8   | `jans-fido2`                             | FIDO2 / passkey service                        |
| 9   | `jans-scim`                              | SCIM service                                   |
| 10  | `jans-link`                              | Account linking service                        |
| 11  | `jans-config-api`                        | Configuration REST API                         |
| 12  | `jans-casa`                              | Self-service end-user portal                   |

### Build cedarling-java

`jans-lock/lock-server` depends on the `cedarling-java` binding. On **Linux**, `mvn clean install` in `jans-cedarling/bindings/cedarling-java` downloads pre-built native libraries from GitHub Releases automatically.

To build the native library yourself (required on macOS and Windows, or when working on Cedarling changes), follow the steps in the [Cedarling Java binding guide](https://docs.jans.io/nightly/cedarling/developer/cedarling-kotlin/index.md) before running Maven in that directory.

### Build jans-cedarling

Please refer to the instructions [here](https://docs.jans.io/nightly/cedarling/tutorials/rust/#building-from-source).

### Build jans-linux-setup

1. From the root of the cloned `jans` repository, change to the setup directory

   ```
   cd jans-linux-setup
   ```

1. Use the [setup package binding](https://docs.jans.io/nightly/jans-linux-setup#installation) instructions to build setup package

### Build jans-cli-tui

1. From the root of the cloned `jans` repository, change to TUI directory

   ```
   cd jans-cli-tui
   ```

1. Use the [jans-cli-tui package binding guide](https://docs.jans.io/nightly/jans-cli-tui#installation) to build TUI package

## Next steps:

- [Setup local development workspace using Eclipse](https://docs.jans.io/nightly/contribute/development/local-run-under-eclipse/index.md)
- [Remote debug Janssen Server](https://docs.jans.io/nightly/contribute/development/remote-debugging/index.md)
- [Run integration tests locally](https://docs.jans.io/nightly/contribute/development/run-integration-tests/index.md)
- [Useful tools and techniques](https://docs.jans.io/nightly/contribute/development/useful-tools/index.md)

Explore left navigation bar on the [documentation site](https://docs.jans.io) for complete list.
