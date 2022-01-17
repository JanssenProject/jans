"""
pygluu.kubernetes.terminal.openbanking
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for terminal openbanking prompts .

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""

import click
from pygluu.kubernetes.terminal.helpers import read_file, read_file_bytes
from pygluu.kubernetes.helpers import exec_cmd, prompt_password


class PromptOpenBanking:
    """Prompt is used for prompting users for input used in deploying Gluu OpenBanking distribution.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_openbanking(self):
        """Prompts for OpenBanking distribution .
        """

        if self.settings.get("global.cnObExtSigningJwksUri") in ("None", ''):
            self.settings.set("global.cnObExtSigningJwksUri",
                              click.prompt("Open banking external signing jwks uri. Used in SSA Validation.",
                                           default="https://keystore.openbankingtest.org.uk/keystore/openbanking.jwks"))

        if self.settings.get("global.cnObExtSigningJwksCrt") in ("None", ''):
            print(
                "Place the Open banking external signing jwks AS certificate string in a file named obsigning.pem. "
                "Used in SSA Validation. "
                " This will be encoded using base64 so please do not encode it.")
            encoded_obsigning_pem = read_file("./obsigning.pem")
            self.settings.set("global.cnObExtSigningJwksCrt", encoded_obsigning_pem)

        if self.settings.get("global.cnObExtSigningJwksKey") in ("None", ''):
            print(
                "Place the Open banking external signing jwks AS key string in a file named obsigning.key. Used in "
                "SSA Validation. "
                " This will be encoded using base64 so please do not encode it.")
            encoded_obsigning_pem = read_file("./obsigning.key")
            self.settings.set("global.cnObExtSigningJwksKey", encoded_obsigning_pem)

        # TODO: its possible that there is no passphrase for the key,
        # and hence the below prompt will always prompt which will affect CI/CD.
        # An installer param should be prompted for that case.
        if self.settings.get("global.cnObExtSigningJwksKeyPassPhrase") in ("None", ''):
            self.settings.set("global.cnObExtSigningJwksKeyPassPhrase",
                              click.prompt(
                                  "OOpen banking external signing jwks AS key passphrase to unlock provided key.",
                                  default=""))

        if self.settings.get("global.cnObExtSigningAlias") in ("None", ''):
            self.settings.set("global.cnObExtSigningAlias",
                              click.prompt("Open banking external signing AS Alias. "
                                           "This is a kid value.Used in SSA Validation, "
                                           "kid used while encoding a JWT sent to token URL",
                                           default="XkwIzWy44xWSlcWnMiEc8iq9s2G"))

        if self.settings.get("global.cnObStaticSigningKeyKid") in ("None", ''):
            self.settings.set("global.cnObStaticSigningKeyKid",
                              click.prompt("Open banking  signing AS kid to force the AS to use a specific signing key",
                                           default="Wy44xWSlcWnMiEc8iq9s2G"))

        if self.settings.get("global.cnObTransportCrt") in ("None", ''):
            print(
                "Place the Open banking AS transport certificate string in a file named obtransport.pem. Used in SSA "
                "Validation. "
                " This will be encoded using base64 so please do not encode it.")
            encoded_obtransport_pem = read_file("./obtransport.pem")
            self.settings.set("global.cnObTransportCrt", encoded_obtransport_pem)

        if self.settings.get("global.cnObTransportKey") in ("None", ''):
            print("Place the Open banking AS transport ke string in a file named obtransport.key. Used in SSA "
                  "Validation. "
                  " This will be encoded using base64 so please do not encode it.")
            encoded_obtransport_key = read_file("./obtransport.key")
            self.settings.set("global.cnObTransportKey", encoded_obtransport_key)

        # TODO: its possible that there is no passphrase for the key,
        # and hence the below prompt will always prompt which will affect CI/CD.
        # An installer param should be prompted for that case.
        if self.settings.get("global.cnObTransportKeyPassPhrase") in ("None", ''):
            self.settings.set("global.cnObTransportKeyPassPhrase",
                              click.prompt("Open banking AS transport key passphrase to unlock AS transport key.",
                                           default=""))

        if self.settings.get("global.cnObTransportAlias") in ("None", ''):
            self.settings.set("global.cnObTransportAlias",
                              click.prompt("Open banking transport Alias used inside the JVM",
                                           default="OpenBankingAsTransport"))

        if self.settings.get("installer-settings.openbanking.hasCnObTransportTrustStore") in ("None", ''):
            self.settings.set("installer-settings.openbanking.hasCnObTransportTrustStore",
                              click.confirm("Do you have the Open banking AS transport truststore crt. "
                                            "This is normally generated from the OB issuing CA, "
                                            "OB Root CA and Signing CA.",
                                            default=False))

        if self.settings.get("global.cnObTransportTrustStore") in ("None", ''):
            if self.settings.get("installer-settings.openbanking.hasCnObTransportTrustStore"):
                print("Place the Open banking AS transport truststore p12 in a file  "
                      "named obtransporttruststore.p12. Used in SSA "
                      "Validation. "
                      " This will be encoded using base64 so please do not encode it.")
                encoded_transport_truststore_pem = read_file_bytes("./obtransporttruststore.p12")
                self.settings.set("global.cnObTransportTrustStore", encoded_transport_truststore_pem)
            else:
                print("Place the Open banking issuing CA, OB Root CA and Signing CA string in one file "
                      "named obcas.pem. Example command: cat obissuingca.pem obrootca.pem obsigningca.pem > obcas.pem "
                      "This will be used to generate the ob transport truststore p12 file "
                      " This will be encoded using base64 so please do not encode it.")
                # check file is there
                read_file("./obcas.pem")
                if self.settings.get("installer-settings.openbanking.cnObTransportTrustStoreP12password") in ("None", ''):
                    self.settings.set("installer-settings.openbanking.cnObTransportTrustStoreP12password",
                                      prompt_password("Open Banking CAs"))
                try:
                    stdout, stderr, retcode = exec_cmd(
                        f'keytool -importcert -file obcas.pem -keystore ob-transport-truststore.p12 -noprompt '
                        f'-alias obkeystore  '
                        f'-storepass {self.settings.get("installer-settings.openbanking.cnObTransportTrustStoreP12password")}')
                except FileNotFoundError:
                    print("Please install keytool.")
                encoded_transport_truststore_pem = read_file_bytes("./ob-transport-truststore.p12")
                self.settings.set("global.cnObTransportTrustStore", encoded_transport_truststore_pem)
