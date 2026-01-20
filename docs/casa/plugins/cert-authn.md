---
tags:
- Casa
- 2FA
- Certificate
- Smartcards
---

# Client certificate authentication

## Overview

At a high level, certificate authentication is a method of verifying identity using digital certificates. Here, the client (user or device) provides a certificate to the server to prove its identity. A certificate contains information like a digital signature, expiration date, name of client (a.k.a subject), certificate authority (CA) name, serial number, and more, all structured using the X.509 standard. Actual authentication occurs in the SSL/TLS handshake, an important process that takes place before any relevant data is transmitted in a SSL/TLS session.

The Casa client certificate authentication plugin allows users to enroll digital certificates and use them as a form of second-factor authentication in the Janssen server. This approach supports [smart cards](#is-the-plugin-compatible-with-smart-cards) as well.

### Requisites

- A Janssen server with Apache or nginx HTTP front
- Jans Casa 1.16.0 or higher. Need it in an [older](#can-the-plugin-be-used-on-older-versions-of-jans) version?
- It is assumed your organization already has an establish mechanism of certificate issuance and deployment. For the purpose of testing, this documentation provides manual steps for certificate issuance

### Configuration steps

The below are the steps required to setup client certificate authentication with Casa in Jans:

- Generate client certificates and an associated certificate chain for testing. This may be skipped if you already own certificates
- Import client certificates in the user's browser
- Setup a cert pickup URL in your front HTTP server
- Deploy and configure the cert-authn Agama project in the server
- Install the cert-authn Casa plugin

## Generate testing certificates

!!! Note
    This section assumes the usage of a *nix machine. Translate commands to your OS accordingly

!!! Note
    Downloads listed here will take up no more than 32MB of bandwidth

For this task, two approaches are covered here. You can choose any or even both for a more extensive testing:

- Reusing already made testing certificates. These are PKI testing certificates published by the Computer Research Security Center of [NIST ITL](https://www.nist.gov/itl)

- Creating a tiny CA through free, open-source tools provided by [Smallstep Labs](https://smallstep.com/). This approach is easy too - just need to run a few commands

The steps given are supposed to be executed in a developer or administrative machine, not your Janssen server.

=== "NIST PKI test certs"

    Visit the PKI testing [site](https://csrc.nist.gov/projects/pki-testing) and download the test descriptions and test data of the Path Validation Testing Program. Extract the contents and in a new, separate directory, copy the following:

    Signing certificates:

    - GoodCACert.crt
    - TrustAnchorRootCertificate.crt

    End-entity certificates:

    - ValidCertificatePathTest1EE.p12
    - InvalidEESignatureTest3EE.p12
    - ValidGeneralizedTimenotAfterDateTest8EE.p12

    There are many end-entity certificates and they map to the tests described in the PDF file downloaded earlier. These three were just picked to exemplify configuration and testing of client certificate authentication in Jans.

    `crt` files are in DER (binary) format and they constitute the so called "certificate chain" or "certificate path". Here is how to generate the chain in PEM format:

    ```bash
    echo "-----BEGIN CERTIFICATE-----" > chain.pem
    base64 TrustAnchorRootCertificate.crt >> chain.pem
    echo "-----END CERTIFICATE-----" >> chain.pem

    echo "-----BEGIN CERTIFICATE-----" >> chain.pem
    base64 GoodCACert.crt >> chain.pem
    echo "-----END CERTIFICATE-----" >> chain.pem
    ```

    In summary, the result is a file containing Base64-encoded certs of the root CA followed by the intermediate CA employed to sign the end-entity (user) certificates.

=== "Generating certs with Smallstep"

    1. Download and install `step-ca` (a private online CA). Go to `https://github.com/smallstep/certificates/releases/latest` and pick the artifact that best matches your OS/platform

    1. Downlod and install `step-cli` (`step-ca` client). Go to `https://github.com/smallstep/cli/releases/latest` and pick the artifact that best matches your OS/platform

    1. Create a file containing the password to encrypt the CA keys, for instance `echo 'Admin1.' > capwd`

    1. Initialize a PKI without CA configuration: `step ca init --pki --name foobar --deployment-type standalone --password-file capwd`. After completion some feedback will be shown including the location of the root certificate and intermediate certificate. Keep those paths at hand

    1. Build the certificate chain file: `cat .step/certs/root_ca.crt .step/certs/intermediate_ca.crt > chain.pem`

    1. Create a private key and a certificate for `cn=Joe` (subject of the cert): `step certificate create Joe joe.crt joe.key --ca ~/.step/certs/intermediate_ca.crt --ca-key ~/.step/secrets/intermediate_ca_key --ca-password-file capwd`. This command will create a certificate signed with the intermediate certificate

    1. Inspect the created cert: `step certificate inspect joe.crt`. Note the expiration is of one day and that "Client authentication" is explicitly part of the extended key usages

    1. Package Joe's certificate and key into a `p12` file: `step certificate p12 joe.p12 joe.crt joe.key`. You will be prompted two passwords: one for the `p12` file itself, and the password previously used to encrypt the private key of the user certificate

### Import client certificates

Instructions for this vary among browsers. Often, it boils down to locating a "certificates" section in the settings menu and import the `p12` file under "Your certificates". A prompt will appear: enter the password used when the `p12` file was generated.

If using the NIST PKI test certs, type **password**. Import all the `p12` files copied.

## Setup a cert pickup URL

For certificate authentication, the HTTP sever must configured so the `CertificateRequest` message is sent in the TLS handshake. This will make the client (web browser in this case) show a dialog for the user to pick one certificate from those already imported into the browser's certificate manager.

![browser-dialog](../../assets/casa/plugins/cert-authn-browser_dialog.png)

The instructions to setup this URL vary depending on the TLS version required and type of HTTP server. Here we provide guidance for Apache 2.4 with TLS 1.2 and 1.3. You can translate the Apache configuration directives directly to nginx. If you face issues or use a different server, please open a [discussion](https://github.com/JanssenProject/jans/discussions).

!!! Note
    The last available version of TLS is 1.3 and is highly recommended to use it.

=== "Apache 2.4 with TLS 1.3"

    This configuration requires creating a subdomain or opening a new port for serving HTTPs. There will be only one URL served in this case, e.g. `https://acme.co:444/`; all other content will respond with HTTP 404 (not found). Hitting this URL will bring up the native web browser dialog for selecting a user certificate.  

    The easiest way to set this up is adding a `VirtualHost` directive associated to a new port, for instance 444. This [snippet](https://github.com/JanssenProject/jans/raw/vreplace-janssen-version/jans-casa/plugins/cert-authn/apache/certauthn_vhost_tls1.3.conf) exemplifies a safe way to do so. Note you have to edit accordingly:

    - The server name
    - The paths to the SSL certificate. This is the web serving certificate and is unrelated to the certificates used for authentication

    Transfer the edited file to `/etc/apache2/sites-enabled`. Note there is already a file named `https_jans.conf` with a `VirtualHost` for port 443 that you can use as a guide. Finally, add a `Listen 444` directive to file `/etc/apache2/ports.conf`.

=== "Apache 2.4 with TLS 1.2"

    This is the fastest approach and involves adding a directive inside the existing `VirtualHost` for port 443 in  `/etc/apache2/sites-enabled/https_jans.conf`. Copy this [snippet](https://github.com/JanssenProject/jans/raw/vreplace-janssen-version/jans-casa/plugins/cert-authn/apache/locationmatch_tls1.2.conf) and paste it just after the closing of the `Location` directive associated to `/jans-casa`.

    In this case your cert pickup URL is `https://<your-host-name>/jans-casa/pl/cert-authn/index.zul`.

### Test

Restart Apache (e.g. `systemctl restart apache2`) and in a browser visit the URL configured, e.g. `https://acme.co:444` (TLS 1.3) or `https://acme.co/jans-casa/pl/cert-authn/` (TLS 1.2). This will display a dialog like [this](../../assets/casa/plugins/cert-authn-browser_dialog.png) listing the certificates imported so far. Do not select anything and close the browser window.

## Deploy and configure the Agama project

!!! Note
    Instructions provided here assume usage of TUI. Do the equivalent in admin-ui or other configuration mechanism

1. Download the certificate authentication Agama project archive: `https://maven.jans.io/maven/io/jans/casa/plugins/cert-authn-agama/replace-janssen-version/cert-authn-agama-replace-janssen-version-project.zip`

1. Transfer the zip file to a location in the server and deploy it. For example, if using TUI, go to Agama menu -> "Upload project". Wait one minute

1. Scroll through the list of projects until `cert-authn` is highlighted

1. Open the configuration management dialog (press `c`) and choose to export the sample configuration to a file on disk

1. Edit property `certPickupUrl` accordingly. This is the URL that displays the browser dialog for choosing a certificate

1. If you did not [Generate testing certificates](#generate-testing-certificates), i.e. already own some certs, create a file named `chain.pem` by concatenating the contents in PEM format of the certificate chain starting with the root CA cert, and appending the rest of intermediate certificates. The last certificate would be the one employed to sign the end-entity (user certificate). Ensure the BEGIN/END CERTIFICATE marker lines are included

1. Compute a one liner JSON string for the contents of `certChainPEM` property: `sed -i.bak ':a;N;$!ba;s/\n/\\n/g' chain.pem`

1. Save the JSON file and open again the configuration management dialog for the cert-authn Agama project. Import the resulting file

The next **optional** step is assigning an icon to certificate authentication for the Casa selector page:

1. Scroll through the list of projects until the `casa` project is highlighted

1. Open the project details (press `v`) and export the current configuration to a file on disk

1. Edit the file: in the `selector` section under `io.jans.casa.authn.main`, add:

    ```json
    "io.jans.casa.authn.cert": {
       "icon": "<span class='fa-layers fa-fw f2 mr1 nl2'><i class='far fa-circle' data-fa-transform='shrink-4 up-3 right-4'></i><i class='far fa-circle' data-fa-transform='shrink-5 up-3 right-4'></i><i class='far fa-circle' data-fa-transform='shrink-6 up-3 right-4'></i><i class='fas fa-bookmark' data-fa-transform='rotate-30 shrink-9 down-4'></i><i class='fas fa-bookmark' data-fa-transform='rotate--30 shrink-9 down-4 right-8'></i></span>",
       "textKey": "casa.selector.certauthn"
    }
    ```

1. Save the file, and import it back as the configuration for project `casa`


## Install the cert-authn plugin

1. Download the plugin jar file `https://maven.jans.io/maven/io/jans/casa/plugins/cert-authn/replace-janssen-version/cert-authn-replace-janssen-version-jar-with-dependencies.jar` and copy to your server's `/opt/jans/jetty/jans-casa/plugins`. Alternatively upload the file using Casa itself: go to `Administration console` > `Casa plugins`

1. Wait one minute. In the admin console, navigate to the "Authentication methods" page. A new "User certificates" widget will appear. Enable the authentication method, drag it to the location (priority) desired, and hit "Save"

1. Navigate to Casa main dashboard. A new menu item will appear for the certificate enrollment

## Testing

!!! Warning
    The dialog for users to pick a certificate from the already imported certificates do not behave the same for all browsers. In some cases, a browser will "cache" the selection and remember the choice for the whole browsing session or even for several days. Sometimes, the user can choose for how long the selection is remembered. You can always clear the SSL cache to make the prompt appear again. Instructions to do so vary.

### Enrollment

Use one or more testing users in Jans. Assign (enroll) testing certificates as desired. Here's how to do it for a single account:

1. Ensure you have [imported](#import-client-certificates) the user certificates in `p12` format into the certificate manager of the given browser

1. Login as the user and click on "Manage certificates" on the main dashboard

1. Read the hints given for an effective enrollment. Click on the "Proceed" button

1. The browser will be redirected to the configured cert pickup URL and a native dialog will appear. From here, choose one of the available certificates

1. A message will appear acknowledging the certificate was successfully linked to the account. Click on "Return"

1. The browser will be taken to the listing of currently enrolled certificates. A new entry will appear associated to the certificate previously chosen

Testing can be extended to the following scenarios:

- Enroll an additional certificate
- Attempt to enroll an already enrolled certificate
- Attempt to enroll an expired certificate
- Attempt to enroll an invalid certificate if you have one at hand (as in the NIST PKI certs), or a certificate issued with a certificate chain that is different from the chain currently configured for the `cert-authn` Agama project

## Authentication

Once testing users have certificates enrolled, add other credentials like OTP so second-factor authentication can be turned on for them.

Here are steps for testing authentication for a given user (happy path):

1. If there is a Casa session open, log out and try to login again

1. Supply username and password

1. On the selection page click on "user certificates". If this option does not appear, ensure you have properly followed [these](#install-the-cert-authn-plugin) steps

1. The browser will be redirected to the configured cert pickup URL. If no dialog appears, the browser is reusing a previous selection. Close the window and start again, or flush the SSL cache

1. Select one of the already enrolled certificates. Authentication should succeed

Testing can be extended to the following scenarios where authentication should fail:

- Selecting a certificate imported in the browser but not enrolled in Casa
- Selecting a certificate a different user has enrolled
- Selecting a certificate that was enrolled but has expired
- Not selecting any certificate: hitting "Cancel" or "Don't send a certificate" option in the browser dialog

## FAQ

### How to customize the browser dialog for not remembering the user choice?

This is not possible - at least from Janssen server.

### Is the plugin compatible with smart cards?

Yes. There is a simple demonstrative [tutorial](./cert-authn-tutorial.md) available.

### Does the plugin support revocation?

It's not implemented but we may add CRL functionality on request. OCSP is another option but it seems to be a [discouraged](https://www.feistyduck.com/newsletter/issue_121_the_slow_death_of_ocsp) practice these days.

Another practice to consider for your PKI is using passive revocation and short-lived certificates. Here, a certificate that has been revoked can no longer be renewed at the CA. It will still be valid for the remainder of its validity period, but cannot be prolonged.

### Can the plugin be used on older versions of Jans?

The plugin can be run in versions 1.1.5 to 1.15.0 with a subtle update in the application descriptor. Run the following in your VM-based installation:

- `cd` to `/opt/jans/jetty/jans-casa/webapps`

- Run `jar -xf jans-casa.war WEB-INF/web.xml`

- Edit `web.xml` by inserting the following inside the `web-app` tag:

    ```xml
    <session-config>
        <tracking-mode>COOKIE</tracking-mode>
    </session-config>
    ```

- Save the file and update the war file: `jar -uf jans-casa.war WEB-INF/web.xml`

- Run `rm -rf WEB-INF`

- Restart casa

### Are certificates required to have the Client Authentication purpose in the extended key usage?

No. Section [4.2.1.12](https://datatracker.ietf.org/doc/html/rfc5280.html#section-4.2.1.12) of RFC 5280 mentions:

> Certificate using applications MAY require that the extended key usage extension be present and that a particular purpose be indicated in order for the certificate to be acceptable to that application.

We chose not to require the presence of the purpose.
