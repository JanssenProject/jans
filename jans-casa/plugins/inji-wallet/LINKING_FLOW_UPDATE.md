# Inji Wallet Linking Flow Update

## Changes Made

Updated the Inji Wallet plugin to follow Jose Gonzalez's recommended pattern for credential linking, based on the account linking and certificate authentication implementations.

## Key Changes

### 1. InjiWalletRedirectVM.java
**Pattern**: Follows `CertAuthenticationSummaryVM` from cert-authn plugin

**Changes**:
- Uses `io.jans.casa.authn.acctlinking` flow instead of direct Inji flow
- Passes `providerId=mosip` and encrypted `uidRef` as flow inputs
- Uses `StringEncrypter` to encrypt the userId (matching cert-authn pattern)
- Properly handles OAuth callback with state verification
- Uses `WebUtils.execRedirect()` for browser redirect as Jose specified

**Flow**:
1. User clicks "Link" button in index.zul
2. Opens `inji-redirect.zul?credentialType=NID` (or TAX)
3. VM builds OAuth request with:
   - Flow: `agama_io.jans.casa.authn.acctlinking-{base64_inputs}`
   - Inputs: `{"providerId": "mosip", "uidRef": "{encrypted_userId}", "credentialType": "NID"}`
4. Redirects to Janssen auth server
5. Auth server triggers `io.jans.casa.authn.acctlinking` Agama flow
6. Flow receives providerId and uidRef, handles MOSIP verification
7. On success, redirects back to `inji-redirect.zul` (no credentialType param)
8. VM handles callback, verifies code, notifies main page

### 2. InjiWalletInterludeVM.java
**Pattern**: Similar to InjiWalletRedirectVM but for interlude page

**Changes**:
- Updated to use same pattern as RedirectVM
- Uses `io.jans.casa.authn.acctlinking` flow
- Passes same parameters: `providerId=mosip`, encrypted `uidRef`, and `credentialType`

## Important Notes

### Casa Client Configuration
As Jose mentioned, you need to add the redirect URI to the Casa client:
- Add `https://your-domain.com/jans-casa/pl/inji-wallet/user/inji-redirect.zul` to Casa client's redirect URIs

### Flow Integration
The `io.jans.casa.authn.acctlinking` flow needs to:
1. Receive inputs: `providerId`, `uidRef`, `credentialType`
2. Decrypt `uidRef` to get the userId
3. Handle MOSIP credential verification based on `providerId=mosip`
4. Link the credential to the user account
5. Return success/failure

### Comparison with Account Linking
This follows the exact same pattern as account linking:
- Account linking: `providerId=google/facebook/etc`, links social accounts
- Inji Wallet: `providerId=mosip`, links MOSIP credentials

## Testing

1. Ensure Casa client has the redirect URI configured
2. User logs into Casa
3. Navigate to "Inji Wallet" menu
4. Click "Link" for National ID or TAX ID
5. Should redirect to Inji Wallet verification
6. After verification, should return and show credential as linked

## References

- `cert-authn/src/main/java/io/jans/casa/plugins/certauthn/vm/CertAuthenticationSummaryVM.java`
- `acct-linking/src/main/java/io/jans/casa/plugins/acctlinking/vm/AccountsLinkingVM.java`
- Jose's guidance: "replicate what the account linking functionality is doing"
