# Jans Tarp

## Relying Party tool in form of a Browser Extension.

[Demo Video](https://www.loom.com/share/6bfe8c5556a94abea05467e3deead8a2?sid=b65c81d9-c1a1-475c-b89b-c105887d31ad)

This extension is for convenient testing of authentication flows on browser.

## Supporting Browser

- Chrome
- Firefox (version >= 115.0.3 )

## Prerequisite

- Node.js (>= v18.15.0)

## Build

1. Change directory to the project directory (`/jans-tarp`).
2. Run `npm install`.
3. Run `npm run build`. It will create Chrome and Firefox build in `/jans-tarp/dist/chrome` and `/jans-tarp/dist/firefox` directories respectively.
4. To pack the build into a zip file run `npm run pack`. This command will pack  Chrome and Firefox builds in zip files at `/jans-tarp/release`.

## Installation in browser

### Chrome

1. Go to `Settings --> Extensions` of Chrome browser.
2. Switch on the `Developer mode`.
3. Click on `Load unpacked` button to load the extension and select the build created in `/jans-tarp/dist/chrome` directory.

### Firefox

The extension can directly installed on Firefox browser from https://addons.mozilla.org/en-US/firefox/addon/jans-tarp/.

#### Temporary Installation (from build)

1. In Firefox, open the `about:debugging` page.
2. Click the `This Firefox` option.
3. Click the `Load Temporary Add-on` button, then select the `jans-tarp-firefox-v{}.zip` zip file from `/jans-tarp/release/`.

##### Note:

When you are testing Janssen IdP with self-signed cert then follow below steps before client registration using jans-tarp.

1. Open the OP_HOST url on browser.
2. Accept the security risk due to self-signed cert and continue.

![self-signed cert risk](./docs/images/untrusted_cert_risk.png)

## Testing with Keycloak (installed on localhost)

1. Login to KC admin console

2. Go to `Clients --> Client registration --> Client details --> Trusted Hosts`  and set localhost as Trusted Hosts (as your KC is running on localhost).

![Trusted Hosts](./docs/images/kc_trusted_hosts.png)

3. Go to `Client scopes` and create a scope with name `openid`. The assigned type should be `Optional`.

![Client scopes](./docs/images/kc_add_scope.png)

Once above configuration is done, jans-tarp can be used test KC IdP.
