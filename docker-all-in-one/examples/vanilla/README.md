# Examples

The following example is tested using `microk8s`

## Preparing environment


1. Install `microk8s` and `docker`

1.  Build the image locally (as of writing, there's no image available in repository):

    ```
    make build-dev
    ```

1. File contains SSA, saved as `ssa.jwt`

## Configure

1.  Enable `microk8s` add-ons:

    ```
    sudo microk8s.enable dns
    sudo microk8s.enable storage
    sudo microk8s.enable metallb
    ```

    You will be prompted for IP address range, i.e. `192.168.0.105-192.168.0.111`.
    Note, choose range starting from your node's IP address.

1.  Determine which ingress will be used (`nginx` or `istio`).

    If using `nginx`, enable `ingress` add-on:

    ```
    sudo microk8s.enable ingress
    ```

    If using `istio`, enable `istio` add-on:

    ```
    sudo microk8s.enable community
    sudo microk8s.enable istio
    ```

## Install

If using `nginx` ingress:

```
make install INGRESS=nginx
```

If using `istio` ingress:

```
make install INGRESS=istio
```

## Uninstall

If using `nginx` ingress:

```
make uninstall INGRESS=nginx
```

If using `istio` ingress:

```
make uninstall INGRESS=istio
```
