# UMA RPT Endpoint Documentation

## Overview
The UMA RPT (Requesting Party Token) Endpoint allows a client to obtain a Requesting Party Token (RPT) by presenting an authorization API token (AAT) along with the necessary permission tickets. 

For the complete architectural specification, refer to the [UMA 2.0 Grant Specification](https://docs.oasis-open.org/uma/wg/oauth-uma-grant-2.0/v1.0/oauth-uma-grant-2.0-v1.0.html).

## Janssen Server Properties & Configuration
The RPT Endpoint behavior is controlled via specific configuration keys within the Janssen Auth Server configuration properties.

* **Property Name:** `umaRptLifetime`
  * **Description:** Defines the lifetime of the generated Requesting Party Token in seconds.
  * **Recommended Value:** `3600` (1 hour)
* **Property Name:** `umaRptAsJwt`
  * **Description:** A boolean flag indicating whether the Janssen Server should issue RPTs as JSON Web Tokens (JWTs) instead of opaque strings.
  * **Recommended Value:** `false` (or set to `true` if JWT-based inspection is desired by the resource server).

## How to Configure via TUI / Command Line
Administrators can modify these configurations using the Janssen Text User Interface (TUI) or the command-line utility.

### Using Janssen TUI:
1. Launch the Janssen Text UI (`jans-cli` or `jans-tui`).
2. Navigate to **Auth Server Settings** > **UMA Configurations**.
3. Locate the `RPT Lifetime` or `RPT as JWT` fields to update your preferences.
4. Save the changes and confirm the service reloads the properties cleanly.
