This guide shows how to upgrade a Janssen helm deployment.

Note

Custom scripts are considered external configuration and are not updated automatically during an upgrade. Please review and adapt your custom scripts to work with the newer version of the Janssen Server.

1. `helm ls -n <namepsace>`

1. Keep note of the helm release version

1. Add your changes to `override.yaml`

1. Apply your upgrade:

   `helm upgrade <janssen-release-name> janssen/janssen -n <namespace> -f override.yaml --version=0.0.0-nightly`
