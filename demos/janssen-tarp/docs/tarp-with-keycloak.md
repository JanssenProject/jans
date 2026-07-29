# Using Janssen Tarp with Keycloak (localhost)

This tutorial shows how to point the Janssen Tarp browser extension at a **Keycloak** instance running on `localhost`, instead of a Janssen Auth Server. Tarp registers OIDC clients using Dynamic Client Registration (DCR), so Keycloak needs a couple of one-time settings enabled before registration will succeed.

## 1. Prerequisites

- Janssen Tarp installed in your browser (see the [end-user tutorial](./janssen-tarp-tutorial.md) or [build-from-source tutorial](./janssen-tarp-build-from-source.md))
- Docker (or another way to run Keycloak locally)

## 2. Start Keycloak locally

The simplest way to run Keycloak for testing is via Docker:

```bash
docker run -p 127.0.0.1:8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest start-dev
```

This exposes Keycloak on `http://localhost:8080` with an admin user `admin` / `admin`. `start-dev` runs it in development mode, which is fine for local testing (not for production).

Log in to the Admin Console at `http://localhost:8080/admin`.

## 3. Create a realm and a user

1. In the Admin Console, click **Manage realms → Create realm**, give it a name (e.g. `myrealm`), and click **Create**.
2. Switch to that realm, go to **Users → Create new user**, fill in a username, and click **Create**.
3. Open the new user's **Credentials** tab, set a password, and toggle **Temporary** to **Off** so it doesn't need to be reset at first login.

You'll use this realm and user later to sign in through Tarp's authentication flow.

## 4. Configure Keycloak for Janssen Tarp

Tarp calls Keycloak's client registration endpoint directly from the browser (anonymously, without an initial access token), so two realm settings need to be adjusted first.

### 4.1 Trust `localhost` for client registration

1. In the Admin Console, go to **Clients → Client registration → Client details** (Keycloak may also show this as **Initial access token / Registration policies** depending on version) **→ Trusted Hosts**.
2. Add `localhost` to the trusted hosts list, since your Keycloak instance is running on localhost.
3. Save.

![Trusted Hosts configuration](https://raw.githubusercontent.com/JanssenProject/jans/main/demos/janssen-tarp/docs/images/kc_trusted_hosts.png)

### 4.2 Add an `openid` client scope

1. Go to **Client scopes → Create client scope**.
2. Name it `openid`.
3. Set its **Type** to **Optional**.
4. Save.

![Add openid client scope](https://raw.githubusercontent.com/JanssenProject/jans/main/demos/janssen-tarp/docs/images/kc_add_scope.png)

> Tarp requests the `openid` scope by default when registering a client. Without this scope existing on the realm, registration will fail or the resulting client won't behave as a proper OIDC client.

Once these two settings are in place, your Keycloak realm is ready to accept registrations from Tarp.

## 5. Register a client in Tarp against Keycloak

1. Open the Janssen Tarp extension. On the **Authentication** tab, click **+ Add Client**.
2. In the **Register OIDC Client** dialog:
   - **Issuer** — your Keycloak realm's issuer URL, e.g. `http://localhost:8080/realms/myrealm`
   - **Scopes** — add `openid` (press Enter after typing it)
   - Leave **Add an existing client** unchecked, since you want Tarp to dynamically register a new client
3. Click **Register**.

![Register OIDC Client dialog](images/01-register-oidc-client.jpg)

Tarp calls Keycloak's DCR endpoint and, on success, adds the new client to the **OIDC Clients** table with a generated Client ID and Client Secret.

![Registered client in the OIDC Clients list](images/02-client-registered.jpg)

> If registration fails here, double check `localhost` is listed under Trusted Hosts (step 4.1) and that the `openid` scope exists (step 4.2) — these are the two most common causes of DCR failures against Keycloak.

## 6. Run the authentication flow against Keycloak

1. In the client's row, click the ⚡ trigger icon.
2. In **Authentication Flow Inputs**, optionally tick **Display tokens after authentication**, then click **Trigger Auth Flow**.

![Authentication Flow Inputs dialog](images/03-auth-flow-inputs.jpg)

3. You'll be redirected to Keycloak's login page for `myrealm`. Sign in with the user you created in Step 3.
4. After a successful login, Tarp shows the **User Details** page with the Access Token, ID Token, and userinfo returned by Keycloak.

![User Details page with tokens](images/04-user-details-tokens.jpg)

From here, you can continue into the **Cedarling** tab exactly as described in the [end-user tutorial](./janssen-tarp-tutorial.md#5-configure-cedarling) — Cedarling authorization is independent of which IdP issued the tokens, as long as your policy store and trusted issuer configuration recognize Keycloak as a token issuer.

