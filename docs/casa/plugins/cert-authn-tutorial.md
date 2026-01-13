---
tags:
- Casa
- 2FA
- Certificate
- Smartcards
- Yubikeys
---

# Tutorial: smart card authentication in Janssen

With Casa's [certificate authentication plugin](./cert-authn.md) administrators can configure client certificate authentication so users can present digital certificates as a form of second-factor authentication. Some organizations opt to deploy certificate authentication through the use of smart cards - physical cards with embedded integrated circuits that act as security tokens. This practice is considered to offer a high level of security compared to other forms of multi-factor authentication. A well-known example of smart card usage is the U.S. Department of Defense (DoD).

In this document we present a basic example on how to use security keys from the [YubiKey 5 series](https://www.yubico.com/authentication-standards/smart-card/) for smart card authentication.

## Requisites

- Certificate authentication [plugin](./cert-authn.md) installed, configured, and tested
- One or more security keys from the YubiKey 5 series
- A machine with Microsoft Windows
- Firefox browser (optional)

**Notes**:
- This document only provides steps for Microsoft Edge and Mozilla Firefox on Windows
- It is assumed the certificates used for testing were already generated and are available as files (PEM/p12). Usage of Windows is intended only for client testing

## Yubikey initialization

Before starting to use a YubiKey for smart card operations, it is recommended to change the PIN, PUK, and management key from their default values. These two pages bring useful information in this regard:

- https://developers.yubico.com/PIV/Guides/Device_setup.html
- https://developers.yubico.com/PIV/Introduction/Admin_access.html

For mere testing purposes, you may skip this entirely - the default values are listed in the last link provided.

The Yubico PIV Tool (required for Firefox) may also be used to set PIN, PUK, and management key as described in section "Preparing a YubiKey for real use" of this [document](https://developers.yubico.com/yubico-piv-tool/YubiKey_PIV_introduction.html).

## Smart card authentication with Edge

### Install the smart card minidriver

This is a piece of software that allows Windows (and Edge) to interface smoothly with Yubikeys as smart cards. Installers available at [Yubico website](https://www.yubico.com/support/download/smart-card-drivers-tools/).

### Import a certificate

When setting up the Casa plugin, admins (or users themselves) imported their certificates in the certificate manager of the web browser. Here, the certificate has to be imported into the Yubikey instead. Here's how to do so:

1. As administrator, run the following commands:

    ```bash
    reg add "HKLM\SOFTWARE\Microsoft\Cryptography\Defaults\Provider\Microsoft Base Smart Card Crypto Provider" /v AllowPrivateExchangeKeyImport /t REG_DWORD /d 1

    reg add "HKLM\SOFTWARE\Microsoft\Cryptography\Defaults\Provider\Microsoft Base Smart Card Crypto Provider" /v AllowPrivateSignatureKeyImport /t REG_DWORD /d 1
    ```

1. Insert the key

1. Run `certutil -csp "Microsoft Base Smart Card Crypto Provider" -importpfx C:\Path\to\user.p12`. Enter the PIN when prompted

### Test

Using Microsoft Edge, follow steps similar those when the plugin was formerly [tested](./cert-authn.md#testing). Here, the browser dialog for picking a cert will be shown, and then a prompt will appear for entering the Yubikey PIN.

## Smart card authentication with Firefox

### Install the Yubico PIV Tool

This is a [tool](https://developers.yubico.com/yubico-piv-tool/) that provides administrative [PIV](https://developers.yubico.com/PIV/) capabilities for Yubikeys and also bundles a [PKCS#11](https://en.wikipedia.org/wiki/PKCS_11) module that enables a communication bridge between the Yubikeys and other software such as Firefox. Installers for Windows are available [here](https://www.yubico.com/support/download/smart-card-drivers-tools/)

### Import a certificate

When setting up the Casa plugin, admins (or users themselves) imported their certificates in the certificate manager of the web browser. Here, the certificate has to be imported into the Yubikey instead. Follow the steps below:

1. In a command line window, run `c:\Program Files\Yubico\Yubico PIV Tool\bin\yubico-piv-tool -s9a -KPKCS12 -aimport-key -aimport-certificate -i C:\Path\to\user.p12`. Enter the Yubikey PIN when prompted. This will import the end-entity certificate into slot `9a` - more about slots [here](https://developers.yubico.com/PIV/Introduction/Certificate_slots.html)

1. You can run `c:\Program Files\Yubico\Yubico PIV Tool\bin\yubico-piv-tool -astatus` to ensure the certificate was properly added

1. Open Firefox settings. In the certificate manager, go to the "Authorities" tab and import the issuer certificate (of the end-entity cert)

### Load YKCS11 module

1. As administrator, ensure the `bin` directory of the PIV tool is added to the `Path` environment variable. This path may look like `c:\Program Files\Yubico\Yubico PIV Tool\bin`

1. Still in the Firefox settings, locate "Security devices" and click on "Load". Choose a name for the Yubikey PKCS#11  module and browse to the `bin` directory of the PIV tool. Finally select the file `libykcs11.dll`

1. Restart Firefox

### Test

Using Firefox, follow steps similar those when the plugin was formerly [tested](./cert-authn.md#testing). Here, the browser dialog for picking a cert will be shown, and then a prompt will appear for entering the Yubikey PIN.

## Useful resources

- https://support.yubico.com/s/article/YubiKey-smart-card-deployment-guide
- https://developers.yubico.com/PIV/Guides/
- https://support.yubico.com/s/article/Enabling-Smart-Card-in-Firefox-on-Windows
- https://developers.yubico.com/yubico-piv-tool/YKCS11/Supported_applications/firefox.html
